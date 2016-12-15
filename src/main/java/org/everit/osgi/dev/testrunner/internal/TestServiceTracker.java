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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.everit.osgi.dev.testrunner.TestManager;
import org.everit.osgi.dev.testrunner.TestRunnerConstants;
import org.everit.osgi.dev.testrunner.blocking.ShutdownBlocker;
import org.everit.osgi.dev.testrunner.engine.TestClassResult;
import org.everit.osgi.dev.testrunner.internal.blocking.TestNumShutdownBlockerImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Tracks test OSGi services that have the necessary service propertis.
 */
public final class TestServiceTracker extends ServiceTracker<Object, Object> {

  private static final Logger LOGGER = Logger.getLogger(TestServiceTracker.class.getName());

  private static final File TEST_RESULT_FOLDER_FILE;

  static {
    String testResultFolder = System.getenv(TestRunnerConstants.ENV_TEST_RESULT_FOLDER);
    if (testResultFolder != null) {
      TEST_RESULT_FOLDER_FILE = new File(testResultFolder);
    } else {
      TEST_RESULT_FOLDER_FILE = null;
    }
  }

  /**
   * Creates a new Test Service Tracker..
   *
   * @param bundleContext
   *          The context of the test runner bundle.
   * @param testManager
   *          The test manager.
   * @param testNumBlocker
   *          The test number blocker.
   * @return The service tracker instance.
   */
  public static TestServiceTracker createTestServiceTracker(final BundleContext bundleContext,
      final TestManager testManager, final TestNumShutdownBlockerImpl testNumBlocker) {
    try {
      Filter filter =
          bundleContext.createFilter("(" + TestRunnerConstants.SERVICE_PROPERTY_TEST_ID + "=*)");
      return new TestServiceTracker(bundleContext, testManager, filter, testNumBlocker);

    } catch (InvalidSyntaxException e) {
      throw new RuntimeException("An exception is thrown that should never happen", e);
    }
  }

  private ServiceRegistration<ShutdownBlocker> activeTestShutdownBlockerSR;

  private final TestManager testManager;

  private final TestNumShutdownBlockerImpl testNumBlocker;

  private TestServiceTracker(final BundleContext bundleContext, final TestManager testManager,
      final Filter filter,
      final TestNumShutdownBlockerImpl testNumBlocker) {
    super(bundleContext, filter, null);
    this.testManager = testManager;
    this.testNumBlocker = testNumBlocker;
  }

  @Override
  public Object addingService(final ServiceReference<Object> reference) {
    List<TestClassResult> testClassResults = testManager.runTest(reference, false);

    if (testClassResults != null) {
      dumpTestResults(reference, testClassResults);

      int sumTestNum = 0;
      for (TestClassResult testClassResult : testClassResults) {
        sumTestNum += testClassResult.getErrorCount() + testClassResult.getFailureCount()
            + testClassResult.getIgnoreCount() + testClassResult.getRunCount();
      }
      if (testNumBlocker != null) {
        testNumBlocker.addProcessedTestNum(sumTestNum);
      }
    } else {
      LOGGER.info(
          "Tests for reference has no result. The cause should be in the log before this entry: "
              + reference.toString());
    }
    return null;
  }

  @Override
  public void close() {
    super.close();
    if (activeTestShutdownBlockerSR != null) {
      activeTestShutdownBlockerSR.unregister();
      activeTestShutdownBlockerSR = null;
    }
  }

  private void dumpTestResults(final ServiceReference<Object> testServiceReference,
      final List<TestClassResult> testClassResults) {

    String testId = ResultUtil.getTestIdFromReference(testServiceReference);
    for (TestClassResult testClassResult : testClassResults) {
      if (TEST_RESULT_FOLDER_FILE != null) {
        String fileName =
            ResultUtil.generateFileNameWithoutExtension(testClassResult.getClassName(), testId,
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
  }

  @Override
  public void modifiedService(final ServiceReference<Object> reference, final Object service) {
  }

  @Override
  public void removedService(final ServiceReference<Object> reference, final Object service) {
  }

}
