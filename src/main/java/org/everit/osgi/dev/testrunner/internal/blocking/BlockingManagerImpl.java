package org.everit.osgi.dev.testrunner.internal.blocking;

/*
 * Copyright (c) 2011, Everit Kft.
 *
 * All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.everit.osgi.dev.testrunner.internal.util.BundleUtil;
import org.everit.osgi.dev.testrunner.internal.util.DependencyUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A manager that handles all the blocking causes why the tests should not start and starts them when there is no more
 * cause. Many technologies can have their starting process asynchronously. In that case technology based
 * {@link Blocker} implementations can monitor their state and notify the {@link BlockingManager} when the tests are
 * ready to run.
 */
public final class BlockingManagerImpl implements BlockingManager {

    /**
     * The list of {@link Blocker}s that can block the test running.
     */
    private static final List<Blocker> BLOCKERS;

    /**
     * The period of time when the causes of the blocks are logged in millisecs.
     */
    public static final int BLOCKING_CAUSE_LOG_PERIOD = 30000;

    /**
     * The max. length in a string of a bundle id. This is used for pretty output during logging.
     */
    private static final int BUNDLE_ID_MAX_LENGTH = 8;

    /**
     * The max. length in a string of a bundle state name. This is used for pretty output during logging.
     */
    private static final int BUNDLE_STATE_NAME_MAX_LENGTH = 12;

    /**
     * Logger of class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockingManagerImpl.class);

    static {
        BLOCKERS = new ArrayList<Blocker>();

        BLOCKERS.add(new FrameworkBlockerImpl());
        if (DependencyUtil.isBlueprintAvailable()) {
            BLOCKERS.add(new BlueprintBlockerImpl());
        }

    }

    /**
     * The blockers that are currently blocking the test runners.
     */
    private Map<Blocker, Boolean> activeBlockers = new ConcurrentHashMap<Blocker, Boolean>();

    /**
     * The thread that starts waits for all block causes and starts the tests when there is no more cause. This has to
     * be on a new thread as otherwise the whole framework starting would be blocked and there would be a deadlock. This
     * thread is interrupted when the manager is stopped (e.g. due to a timeout).
     */
    private Thread blockingManagerThread;

    /**
     * Test runners that were started by this manager.
     */
    private Map<TestRunner, Boolean> startedTestRunners = new ConcurrentHashMap<TestRunner, Boolean>();

    /**
     * A flag that indicates whether this manager is stopped or not.
     */
    private AtomicBoolean stopped = new AtomicBoolean(true);

    /**
     * The system bundle of this OSGI container. Tests may not start until the framework is started.
     */
    private final Bundle systemBundle;

    /**
     * Helper object for synchronization of the threads that are waiting for test results.
     */
    private Object testResultGettingWaiter = new Object();

    /**
     * The queue of tests that will run.
     */
    private final Queue<TestRunner> testRunnerQueue = new ConcurrentLinkedQueue<TestRunner>();

    /**
     * Helper object to be able to wait until framework is launched and all blueprint bundles are either started or
     * failed. Tests may run after these events happened.
     */
    private Object testRunningWaiter = new Object();

    /**
     * Constructor of blocking manager.
     * 
     * @param bundleContext
     *            bundle context that will be used to get the system bundle.
     */
    public BlockingManagerImpl(final BundleContext bundleContext) {
        systemBundle = bundleContext.getBundle(0);
    }

    @Override
    public void addTestRunner(final TestRunner testRunner) {
        LOGGER.info("Adding test runner to the queue: " + testRunner.toString());
        testRunnerQueue.add(testRunner);
    }

    private void logBlockCauses() {
        StringBuilder sb = new StringBuilder("Test running is blocked due to the following reasons:\n");
        for (Blocker blocker : activeBlockers.keySet()) {
            sb.append("Blocker").append(blocker.toString()).append("\n");
            blocker.logBlockCauses(sb);
        }
        LOGGER.info(sb.toString());
    }

    /**
     * In case not all of the tests were run, a cause can be that not all of the bundles started properly. This function
     * logs out the bundle names that are not in active state after the framework is started.
     */
    private void logNonStartedBundles() {
        Bundle[] bundles = systemBundle.getBundleContext().getBundles();
        List<Bundle> nonStartedBundles = new ArrayList<Bundle>();
        for (Bundle bundle : bundles) {
            if (bundle.getState() != Bundle.ACTIVE) {
                Object fragmentHostHeader = bundle.getHeaders().get("Fragment-Host");
                if ((fragmentHostHeader == null) || "".equals(fragmentHostHeader.toString().trim())) {
                    nonStartedBundles.add(bundle);
                }
            }
        }

        if (nonStartedBundles.size() > 0) {
            StringBuilder sb = new StringBuilder("The following bundles are not started (this can be a cause if")
                    .append(" your tests fail to run): \n");
            for (Bundle bundle : nonStartedBundles) {
                String bundleId = String.valueOf(bundle.getBundleId());
                sb.append(bundleId);
                for (int j = 0, n = (BUNDLE_ID_MAX_LENGTH - bundleId.length()); j < n; j++) {
                    sb.append(' ');
                }
                String bundleStateName = BundleUtil.getBundleStateName(bundle.getState());
                sb.append(bundleStateName);
                for (int j = 0, n = (BUNDLE_STATE_NAME_MAX_LENGTH - bundleStateName.length()); j < n; j++) {
                    sb.append(' ');
                }
                sb.append(bundle.getSymbolicName()).append("_").append(bundle.getVersion().toString()).append("\n");
            }
            LOGGER.warn(sb.toString());
        }
    }

    @Override
    public void start(final BundleContext context) {
        if (stopped.compareAndSet(true, false)) {
            for (final Blocker blocker : BLOCKERS) {
                blocker.start(new BlockListener() {

                    @Override
                    public void block() {
                        activeBlockers.put(blocker, true);

                    }

                    @Override
                    public void unblock() {
                        activeBlockers.remove(blocker);
                        if (activeBlockers.size() == 0) {
                            synchronized (testRunningWaiter) {
                                testRunningWaiter.notify();
                            }
                        }
                    }
                }, context);
            }
            blockingManagerThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    waitForTestsToStart();
                    if (!stopped.get()) {
                        logNonStartedBundles();
                        TestRunner testRunner = testRunnerQueue.peek();
                        while ((testRunner != null) && !stopped.get()) {
                            testRunner.start();
                            startedTestRunners.put(testRunner, Boolean.TRUE);
                            testRunnerQueue.remove();
                            testRunner = testRunnerQueue.peek();
                        }
                    }
                    synchronized (testResultGettingWaiter) {
                        testResultGettingWaiter.notify();
                    }
                }
            });
            blockingManagerThread.start();
        } else {
            LOGGER.warn("Trying to start an already started BlockingManager");
        }
    }

    @Override
    public void stop() {
        if (stopped.compareAndSet(false, true)) {
            stopStartedTestRunners();
            blockingManagerThread.interrupt();
            synchronized (testResultGettingWaiter) {
                testResultGettingWaiter.notify();
            }
            synchronized (testRunningWaiter) {
                testRunningWaiter.notify();
            }
        } else {
            LOGGER.warn("Stop called on Test Runner BlockingManager while it was already stopped");
        }

    }

    private void stopStartedTestRunners() {
        for (TestRunner testRunner : startedTestRunners.keySet()) {
            testRunner.stop();
        }

    }

    @Override
    public void waitForTestResultsAfterStartup() {
        synchronized (testResultGettingWaiter) {
            while (((activeBlockers.size() > 0) || (testRunnerQueue.size() > 0)) && !stopped.get()) {
                try {
                    testResultGettingWaiter.wait(BLOCKING_CAUSE_LOG_PERIOD);
                    if (systemBundle.getState() != Bundle.ACTIVE) {
                        LOGGER.info("Waiting for the framework to start");
                    }
                } catch (InterruptedException e) {
                    LOGGER.error("Test result waiting  was interrupted", e);
                    stop();
                }
            }
        }
    }

    private void waitForTestsToStart() {
        synchronized (testRunningWaiter) {
            while (!stopped.get()
                    && (activeBlockers.size() > 0)) {
                LOGGER.info(testRunnerQueue.size() + " test runners are waiting to run");
                try {
                    testRunningWaiter.wait(BLOCKING_CAUSE_LOG_PERIOD);
                    if ((activeBlockers.size() > 0) && !stopped.get()) {
                        logBlockCauses();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOGGER.error("Test running waiting was interrupted");
                    stop();
                }
            }
        }
    }
}
