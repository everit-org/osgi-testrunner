/*
 * Copyright (C) 2011 Everit Kft. (http://www.everit.biz)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.everit.osgi.dev.testrunner.internal;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.everit.osgi.dev.testrunner.TestRunnerConstants;
import org.everit.osgi.dev.testrunner.engine.TestClassResult;
import org.everit.osgi.dev.testrunner.engine.TestEngine;
import org.everit.osgi.dev.testrunner.engine.TestExecutionContext;
import org.everit.osgi.dev.testrunner.internal.blocking.BlockingManagerImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * The test manager implementation.
 */
public class TestExtender {

  /**
   * Tracks test engine OSGi services.
   */
  private class TestEngineTrackerCustomizer
      implements ServiceTrackerCustomizer<TestEngine, TestEngine> {

    @Override
    public TestEngine addingService(final ServiceReference<TestEngine> reference) {
      TestEngine testEngine = bundleContext.getService(reference);
      Object testEngineProp =
          reference.getProperty(TestRunnerConstants.SERVICE_PROPERTY_TESTRUNNER_ENGINE);

      if (testEngineProp == null || !(testEngineProp instanceof String)) {
        LOGGER.warning("TestEngine is missing required '"
            + TestRunnerConstants.SERVICE_PROPERTY_TESTRUNNER_ENGINE + "' service property: "
            + reference.toString());
      }

      synchronized (mutex) {
        testEngineByName.put((String) testEngineProp, testEngine);
        mutex.notifyAll();
      }
      return testEngine;
    }

    @Override
    public void modifiedService(final ServiceReference<TestEngine> reference,
        final TestEngine service) {
    }

    @Override
    public void removedService(final ServiceReference<TestEngine> reference,
        final TestEngine service) {

      String name = String
          .valueOf(reference.getProperty(TestRunnerConstants.SERVICE_PROPERTY_TESTRUNNER_ENGINE));
      synchronized (mutex) {
        testEngineByName.remove(name);
      }
      bundleContext.ungetService(reference);
    }
  }

  /**
   * Tracks OSGi services that have the eosgi.testId service property.
   */
  private class TestServiceTrackerCustomizer implements ServiceTrackerCustomizer<Object, Object> {

    @Override
    public Object addingService(final ServiceReference<Object> reference) {
      Object service = bundleContext.getService(reference);
      addTest(reference, service);
      return service;
    }

    @Override
    public void modifiedService(final ServiceReference<Object> reference, final Object service) {
      // Do nothing for now
    }

    @Override
    public void removedService(final ServiceReference<Object> reference, final Object service) {
      removeTest(reference, service);
      bundleContext.ungetService(reference);
    }
  }

  /**
   * Simple holder class of an OSGi service reference and the service object. Two instances of this
   * class are equal if the reference instance is equal.
   */
  private static class TestServiceWithReference {

    ServiceReference<Object> reference;

    Object service;

    TestServiceWithReference(final ServiceReference<Object> reference,
        final Object service) {
      this.reference = reference;
      this.service = service;
    }

    @Override
    public boolean equals(final Object obj) {
      return (obj instanceof TestServiceWithReference)
          && ((TestServiceWithReference) obj).reference.equals(reference);
    }

    @Override
    public int hashCode() {
      return reference.hashCode();
    }

  }

  private static final Logger LOGGER = Logger.getLogger(TestExtender.class.getName());

  private static final File TEST_RESULT_FOLDER_FILE;

  static {
    String testResultFolder = System.getProperty(TestRunnerConstants.PROP_TEST_RESULT_FOLDER);
    if (testResultFolder != null) {
      TEST_RESULT_FOLDER_FILE = new File(testResultFolder);
    } else {
      TEST_RESULT_FOLDER_FILE = null;
    }
  }

  private static Map<String, ?> extractServiceReferencePropsAsMap(
      final ServiceReference<Object> reference) {

    Map<String, Object> result = new HashMap<>();
    String[] propertyKeys = reference.getPropertyKeys();
    for (String key : propertyKeys) {
      result.put(key, reference.getProperty(key));
    }
    return result;
  }

  private final BlockingManagerImpl blockingManager;

  private final BundleContext bundleContext;

  private final boolean developmentMode;

  private final Object mutex = new Object();

  private final Map<String, Set<TestServiceWithReference>> nonExecutedServicesByEngines =
      new HashMap<>();

  private final AtomicBoolean opened = new AtomicBoolean(false);

  private final Map<String, TestEngine> testEngineByName = new HashMap<>();

  private ServiceTracker<TestEngine, TestEngine> testRunnerEngineTracker;

  private ServiceTracker<Object, Object> testServiceTracker;

  /**
   * Constructor.
   *
   * @param bundleContext
   *          the context of the bundle.
   * @param blockingManager
   *          The blocking manager that is notified when a test is executed.
   * @param developmentMode
   *          Whether the test runner is in development mode or not. In development mode only those
   *          tests are executed that are annotated with @TestDuringDevelopment.
   */
  public TestExtender(final BundleContext bundleContext,
      final BlockingManagerImpl blockingManager, final boolean developmentMode) {
    this.bundleContext = bundleContext;
    this.blockingManager = blockingManager;
    this.developmentMode = developmentMode;
  }

  private void addTest(final ServiceReference<Object> reference,
      final Object service) {

    Object engineProp =
        reference.getProperty(TestRunnerConstants.SERVICE_PROPERTY_TESTRUNNER_ENGINE);

    if ((engineProp == null) || !(engineProp instanceof String)) {
      LOGGER.log(Level.WARNING,
          "Unrecognized '" + TestRunnerConstants.SERVICE_PROPERTY_TESTRUNNER_ENGINE
              + "' service property value for test. Are you sure the test engine is available?"
              + " Ignoring: " + reference.toString());
      return;
    }

    String engine = (String) engineProp;

    synchronized (mutex) {

      Set<TestServiceWithReference> tests = nonExecutedServicesByEngines.get(engine);
      if (tests == null) {
        tests = new HashSet<>();
        nonExecutedServicesByEngines.put(engine, tests);
      }

      tests.add(new TestServiceWithReference(reference, service));
      mutex.notifyAll();
    }
  }

  /**
   * Closes all resources that this extender opened.
   */
  public void close() {
    opened.set(false);

    synchronized (mutex) {
      mutex.notifyAll();
    }

    testServiceTracker.close();
    testRunnerEngineTracker.close();
  }

  private ServiceTracker<Object, Object> createTestServiceTracker() {
    try {
      Filter filter =
          bundleContext.createFilter("(" + TestRunnerConstants.SERVICE_PROPERTY_TEST_ID + "=*)");
      return new ServiceTracker<>(bundleContext, filter, new TestServiceTrackerCustomizer());

    } catch (InvalidSyntaxException e) {
      throw new RuntimeException("An exception is thrown that should never happen", e);
    }
  }

  private void dumpTestResults(final ServiceReference<Object> testServiceReference,
      final TestClassResult testClassResult) {

    String testId = ResultUtil.getTestIdFromReference(testServiceReference);
    if (TEST_RESULT_FOLDER_FILE != null) {
      String fileName =
          ResultUtil.generateFileNameWithoutExtension(testClassResult.className, testId,
              true);

      File textFile = new File(TEST_RESULT_FOLDER_FILE, fileName + ".txt");
      try {
        ResultUtil.writeTextResultToFile(testClassResult, testId, textFile, true);
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Error during text test result " + testClassResult.toString()
            + " to file " + textFile.getAbsolutePath(), e);
      }

      File xmlFile = new File(TEST_RESULT_FOLDER_FILE, fileName + ".xml");

      ResultUtil.writeXmlResultToFile(testClassResult, xmlFile, testId, true);
    }

    try {
      StringWriter sw = new StringWriter();
      sw.write("\n");
      ResultUtil.dumpTextResult(testClassResult, testId, sw);
      LOGGER.info(sw.toString());
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Error dumping text result to standard output", e);
    }
  }

  /**
   * Opens the trackers of test engines and test services.
   */
  public void open() {
    testServiceTracker = createTestServiceTracker();
    testServiceTracker.open();

    testRunnerEngineTracker =
        new ServiceTracker<>(bundleContext, TestEngine.class, new TestEngineTrackerCustomizer());
    testRunnerEngineTracker.open();

    opened.set(true);

    new Thread(() -> {
      while (opened.get()) {
        synchronized (mutex) {
          processAvailableTestsWithAvailableEnginesInSync();
          try {
            mutex.wait();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
          }
        }
      }
    }).start();
  }

  private void processAvailableTestsWithAvailableEnginesInSync() {
    Iterator<Entry<String, Set<TestServiceWithReference>>> iterator =
        nonExecutedServicesByEngines.entrySet().iterator();

    while (iterator.hasNext()) {
      Entry<String, Set<TestServiceWithReference>> entry = iterator.next();

      TestEngine testEngine = testEngineByName.get(entry.getKey());

      if (testEngine != null) {
        iterator.remove();
        Set<TestServiceWithReference> testServiceWithReferences = entry.getValue();
        for (TestServiceWithReference testServiceWithReference : testServiceWithReferences) {
          Object testObject = testServiceWithReference.service;
          ServiceReference<Object> reference = testServiceWithReference.reference;

          TestExecutionContext testExecutionContext = new TestExecutionContext();
          testExecutionContext.developmentMode = developmentMode;
          TestClassResult result = testEngine.runTestsOfInstance(testObject,
              extractServiceReferencePropsAsMap(reference), testExecutionContext);

          dumpTestResults(reference, result);

          if (blockingManager != null) {
            blockingManager.handleTestClassResult(result);
          }
        }
      }
    }
  }

  private void removeTest(final ServiceReference<Object> reference,
      final Object service) {

    Object engineTypeObject =
        reference.getProperty(TestRunnerConstants.SERVICE_PROPERTY_TESTRUNNER_ENGINE);

    String engine = String.valueOf(engineTypeObject);

    synchronized (mutex) {
      Set<TestServiceWithReference> tests = nonExecutedServicesByEngines.get(engine);
      if (tests != null) {
        tests.remove(new TestServiceWithReference(reference, service));
        if (tests.isEmpty()) {
          nonExecutedServicesByEngines.remove(engine);
        }
      }
    }
  }
}
