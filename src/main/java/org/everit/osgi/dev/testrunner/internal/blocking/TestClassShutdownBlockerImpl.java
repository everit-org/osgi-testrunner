/*
 * Copyright (C) 2011 Everit Kft. (http://www.everit.org)
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
package org.everit.osgi.dev.testrunner.internal.blocking;

import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.everit.osgi.dev.testrunner.blocking.AbstractShutdownBlocker;
import org.everit.osgi.dev.testrunner.engine.TestClassResult;
import org.everit.osgi.dev.testrunner.testclasscapability.util.TestClassCapabilityDTO;
import org.everit.osgi.dev.testrunner.testclasscapability.util.TestClassCapabilityUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * Waits for all of the test cases to run based on the
 * {@link org.everit.osgi.dev.testrunner.TestRunnerConstants#CAPABILITY_TESTCLASS_NAMESPACE}
 * capability.
 */
public class TestClassShutdownBlockerImpl extends AbstractShutdownBlocker {

  /**
   * Tracks test class capabilities.
   */
  private class TestClassCapabilityTrackerCustomizer implements BundleTrackerCustomizer<Bundle> {

    @Override
    public Bundle addingBundle(final Bundle bundle, final BundleEvent event) {
      processBundle(bundle);
      return bundle;
    }

    @Override
    public void modifiedBundle(final Bundle bundle, final BundleEvent event, final Bundle object) {
      // Do nothing
    }

    @Override
    public void removedBundle(final Bundle bundle, final BundleEvent event, final Bundle object) {
      // Do nothing
    }
  }

  private final AtomicInteger countOfBlockerTestClasses = new AtomicInteger(0);

  private final Map<String, Integer> remainingTestClassCountByClassName = new HashMap<>();

  private final BundleTracker<Bundle> testCaseCapabilityTracker;

  /**
   * Constructor.
   *
   * @param context
   *          The context of the test runner bundle.
   */
  public TestClassShutdownBlockerImpl(final BundleContext context) {
    testCaseCapabilityTracker = new BundleTracker<>(context,
        Bundle.ACTIVE | Bundle.INSTALLED | Bundle.RESOLVED | Bundle.STARTING | Bundle.STOPPING,
        new TestClassCapabilityTrackerCustomizer());
  }

  public void close() {
    testCaseCapabilityTracker.close();
  }

  @Override
  public synchronized void handleTestClassResult(final TestClassResult testClassResult) {
    String className = testClassResult.className;
    Integer remainingTestClassCount = remainingTestClassCountByClassName.get(className);
    remainingTestClassCount = (remainingTestClassCount != null) ? remainingTestClassCount - 1 : -1;

    if (remainingTestClassCount == 0) {
      remainingTestClassCountByClassName.remove(className);
      int newCountOfBlockerTestClasses = countOfBlockerTestClasses.decrementAndGet();
      if (newCountOfBlockerTestClasses == 0) {
        unblock();
      }
    }
  }

  @Override
  public synchronized void logBlockCauses(final StringBuilder sb) {
    for (Entry<String, Integer> entry : remainingTestClassCountByClassName.entrySet()) {
      sb.append("  Test class '").append(entry.getKey()).append("' must be executed '")
          .append(entry.getValue()).append("' time");

      if (entry.getValue() > 1) {
        sb.append('s');
      }
    }
  }

  public void open() {
    testCaseCapabilityTracker.open();
  }

  private synchronized void processBundle(final Bundle bundle) {
    Dictionary<String, String> headers = bundle.getHeaders();
    String capabilityHeader = headers.get(Constants.PROVIDE_CAPABILITY);
    Collection<TestClassCapabilityDTO> testClassCapabilities =
        TestClassCapabilityUtil.resolveTestCaseCapabilities(capabilityHeader);

    for (TestClassCapabilityDTO testClassCapability : testClassCapabilities) {
      Integer alreadyExpectedCount =
          remainingTestClassCountByClassName.get(testClassCapability.clazz);

      alreadyExpectedCount = (alreadyExpectedCount != null) ? alreadyExpectedCount : 0;
      int newExpectedCount = alreadyExpectedCount + testClassCapability.count;

      remainingTestClassCountByClassName.put(testClassCapability.clazz,
          newExpectedCount);

      if (alreadyExpectedCount <= 0 && newExpectedCount > 0) {
        int newCountOfBlockerTestClasses = countOfBlockerTestClasses.incrementAndGet();
        if (newCountOfBlockerTestClasses == 1) {
          block();
        }
      } else if (alreadyExpectedCount > 0 && newExpectedCount <= 0) {
        int newCountOfBlockerTestClasses = countOfBlockerTestClasses.decrementAndGet();
        if (newCountOfBlockerTestClasses == 0) {
          unblock();
        }
      }

      if (newExpectedCount == 0) {
        remainingTestClassCountByClassName.remove(testClassCapability.clazz);
      }
    }
  }
}
