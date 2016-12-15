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
package org.everit.osgi.dev.testrunner.internal.blocking;

import java.util.Dictionary;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import org.everit.osgi.dev.testrunner.TestRunnerConstants;
import org.everit.osgi.dev.testrunner.blocking.AbstractShutdownBlocker;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * A blocker that does not allow to stop the framework until the necessary number of tests ran.
 */
public class TestNumShutdownBlockerImpl extends AbstractShutdownBlocker {

  /**
   * Tracks bundles that might have {@link TestRunnerConstants#HEADER_EXPECTED_NUMBER_OF_TESTS}
   * header.
   */
  private class TestNumSummarizer implements BundleTrackerCustomizer<Bundle> {

    @Override
    public Bundle addingBundle(final Bundle bundle, final BundleEvent event) {
      Dictionary<String, String> bundleHeaders = bundle.getHeaders();
      String expectedTestNumString =
          bundleHeaders.get(TestRunnerConstants.HEADER_EXPECTED_NUMBER_OF_TESTS);
      if (expectedTestNumString != null) {
        try {
          int exptectedTestNum = Integer.parseInt(expectedTestNumString);
          increaseExpectedTestNum(exptectedTestNum);
        } catch (NumberFormatException e) {
          LOGGER.warning("The value of " + TestRunnerConstants.HEADER_EXPECTED_NUMBER_OF_TESTS
              + " header in the bundle '" + bundle.toString() + "' is invalid.");
          e.printStackTrace(System.err);
        }
      }
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

  private static final Logger LOGGER = Logger.getLogger(TestNumShutdownBlockerImpl.class.getName());

  private boolean blocking = false;

  private int expectedTestNumSum = 0;

  private final Lock lock = new ReentrantLock();

  private int processedTestNumSum = 0;

  private final BundleTracker<Bundle> testNumSummarizer;

  /**
   * Constructor.
   *
   * @param context
   *          The context of this bundle.
   */
  public TestNumShutdownBlockerImpl(final BundleContext context) {
    testNumSummarizer =
        new BundleTracker<>(context, Bundle.ACTIVE | Bundle.INSTALLED | Bundle.RESOLVED
            | Bundle.STARTING | Bundle.STOPPING, new TestNumSummarizer());
  }

  /**
   * Adding new test numbers that were processed.
   *
   * @param processedTestNum
   *          The number of tests that were processed.
   */
  public void addProcessedTestNum(final int processedTestNum) {
    lock.lock();
    try {
      processedTestNumSum += processedTestNum;
      checkBlocking();
    } finally {
      lock.unlock();
    }
  }

  private void checkBlocking() {
    if (blocking && (expectedTestNumSum <= processedTestNumSum)) {
      notifyListenersAboutUnblock();
      blocking = false;
    } else if (!blocking && (expectedTestNumSum > processedTestNumSum)) {
      notifyListenersAboutBlock();
      blocking = true;
    }
  }

  public void close() {
    testNumSummarizer.close();
  }

  private void increaseExpectedTestNum(final int additionalExpectedTestNum) {
    lock.lock();
    try {
      expectedTestNumSum += additionalExpectedTestNum;
      checkBlocking();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void logBlockCauses(final StringBuilder sb) {
    lock.lock();
    sb.append("  The expected number of tests currently is ").append(expectedTestNumSum)
        .append(" while ")
        .append(processedTestNumSum).append(" tests has been processed.");
    lock.unlock();
  }

  public void open() {
    testNumSummarizer.open();
  }
}
