/**
 * This file is part of Everit Test Runner Bundle.
 *
 * Everit Test Runner Bundle is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Everit Test Runner Bundle is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Everit Test Runner Bundle.  If not, see <http://www.gnu.org/licenses/>.
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

    private class TestNumSummarizer implements BundleTrackerCustomizer<Bundle> {

        @Override
        public Bundle addingBundle(final Bundle bundle, final BundleEvent event) {
            Dictionary<String, String> bundleHeaders = bundle.getHeaders();
            String expectedTestNumString = bundleHeaders.get(TestRunnerConstants.HEADER_EXPECTED_NUMBER_OF_TESTS);
            if (expectedTestNumString != null) {
                try {
                    int exptectedTestNum = Integer.parseInt(expectedTestNumString);
                    increaseExpectedTestNum(exptectedTestNum);
                } catch (NumberFormatException e) {
                    LOGGER.warning("The value of " + TestRunnerConstants.HEADER_EXPECTED_NUMBER_OF_TESTS
                            + " header in the bundle '" + bundle.toString() + "' is invalid.");
                    e.printStackTrace();
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

    private int processedTestNumSum = 0;

    private int expectedTestNumSum = 0;

    private boolean blocking = false;

    private Lock lock = new ReentrantLock();

    private final BundleTracker<Bundle> testNumSummarizer;

    public TestNumShutdownBlockerImpl(final BundleContext context) {
        testNumSummarizer = new BundleTracker<Bundle>(context, Bundle.ACTIVE | Bundle.INSTALLED | Bundle.RESOLVED
                | Bundle.STARTING | Bundle.STOPPING, new TestNumSummarizer());
    }

    public void addProcessedTestNum(final int processedTestNum) {
        lock.lock();
        processedTestNumSum += processedTestNum;
        checkBlocking();
        lock.unlock();
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
        expectedTestNumSum += additionalExpectedTestNum;
        checkBlocking();
        lock.unlock();
    }

    @Override
    public void logBlockCauses(final StringBuilder sb) {
        lock.lock();
        sb.append("  The expected number of tests currently is ").append(expectedTestNumSum).append(" while ")
                .append(processedTestNumSum).append(" tests has been processed.");
        lock.unlock();
    }

    public void open() {
        testNumSummarizer.open();
    }
}
