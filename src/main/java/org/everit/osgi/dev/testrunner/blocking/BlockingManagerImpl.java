package org.everit.osgi.dev.testrunner.blocking;

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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.everit.osgi.dev.testrunner.util.BundleUtil;
import org.everit.osgi.dev.testrunner.util.DependencyUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.osgi.service.blueprint.container.BlueprintListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Container that holds a set of Bundle IDs which are in the state that the real startup of the whole OSGI container
 * probably not finished. As blueprint context is started asynchronously the test result checker thread has to wait
 * until all of the blueprint based bundles are set up correctly.
 */
public final class BlockingManagerImpl implements FrameworkListener, BlockingManager {

    /**
     * Logger of class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockingManagerImpl.class);

    private static final List<Blocker> BLOCKERS;

    /**
     * The period of time when the causes of the blocks are logged in millisecs.
     */
    public static final int BLOCKING_CAUSE_LOG_PERIOD = 30000;

    static {
        BLOCKERS = new ArrayList<Blocker>();
        if (DependencyUtil.isBlueprintAvailable()) {
            BLOCKERS.add(new BlueprintBlockerImpl());
        }
    }

    /**
     * Helper object for synchronization of the threads that are waiting for test results.
     */
    private Object testResultGettingWaiter = new Object();

    /**
     * The system bundle of this OSGI container. Tests may not start until the framework is started.
     */
    private final Bundle systemBundle;

    /**
     * The queue of tests that will run.
     */
    private final Queue<BlockedTestRunner> testRunnerQueue = new ConcurrentLinkedQueue<BlockedTestRunner>();

    /**
     * Helper object to be able to wait until framework is launched and all blueprint bundles are either started or
     * failed. Tests may run after these events happened.
     */
    private Object testRunningWaiter = new Object();

    private AtomicBoolean stopped = new AtomicBoolean(true);

    /**
     * Test runners that were started by this manager.
     */
    private Map<BlockedTestRunner, Boolean> startedTestRunners = new ConcurrentHashMap<BlockedTestRunner, Boolean>();

    private Thread blockingManagerThread;

    /**
     * The blockers that are currently blocking the test runners.
     */
    private Map<Blocker, Boolean> activeBlockers = new ConcurrentHashMap<Blocker, Boolean>();

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
    public void addTestRunner(final BlockedTestRunner testRunner) {
        LOGGER.info("Adding test runner to the queue: " + testRunner.toString());
        testRunnerQueue.add(testRunner);
    }

    @Override
    public void frameworkEvent(final FrameworkEvent event) {
        if (event.getType() == FrameworkEvent.STARTED) {
            synchronized (testRunningWaiter) {
                testRunningWaiter.notify();
            }

            synchronized (testResultGettingWaiter) {
                testResultGettingWaiter.notify();
            }
        }
    }

    private void logBlockCauses() {
        StringBuilder sb = new StringBuilder("Test running is blocked due to the following reasons:\n");
        for (Blocker blocker : activeBlockers.keySet()) {
            sb.append("Blocker").append(blocker.toString()).append("\n");
            blocker.logBlockCauses(sb);
        }
        LOGGER.info(sb.toString());
    }

    @Override
    public void start(BundleContext context) {
        if (stopped.compareAndSet(true, false)) {
            for (final Blocker blocker : BLOCKERS) {
                blocker.configure(new BlockListener() {

                    @Override
                    public void unblock() {
                        activeBlockers.put(blocker, true);
                    }

                    @Override
                    public void block() {
                        activeBlockers.remove(blocker);
                        if (activeBlockers.size() == 0) {
                            blockingManagerThread.notify();
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
                        BlockedTestRunner testRunner = testRunnerQueue.peek();
                        while (testRunner != null && !stopped.get()) {
                            testRunner.start();
                            startedTestRunners.put(testRunner, Boolean.TRUE);
                            testRunnerQueue.remove();
                            testRunner = testRunnerQueue.peek();
                        }
                        synchronized (testResultGettingWaiter) {
                            testResultGettingWaiter.notify();
                        }
                    }
                }
            });
            blockingManagerThread.start();
        } else {
            LOGGER.warn("Trying to start an already started BlockingManager");
        }
    }

    private void logNonStartedBundles() {
        Bundle[] bundles = systemBundle.getBundleContext().getBundles();
        List<Bundle> nonStartedBundles = new ArrayList<Bundle>();
        for (Bundle bundle : bundles) {
            if (bundle.getState() != Bundle.ACTIVE) {
                Object fragmentHostHeader = bundle.getHeaders().get("Fragment-Host");
                if (fragmentHostHeader == null || "".equals(fragmentHostHeader.toString().trim())) {
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
                for (int j = 0, n = (8 - bundleId.length()); j < n; j++) {
                    sb.append(' ');
                }
                String bundleStateName = BundleUtil.getBundleStateName(bundle.getState());
                sb.append(bundleStateName);
                for (int j = 0, n = (12 - bundleStateName.length()); j < n; j++) {
                    sb.append(' ');
                }
                sb.append(bundle.getSymbolicName()).append("_").append(bundle.getVersion().toString()).append("\n");
            }
            LOGGER.warn(sb.toString());
        }
    }

    private void stopStartedTestRunners() {
        for (BlockedTestRunner testRunner : startedTestRunners.keySet()) {
            testRunner.stop();
        }

    }

    @Override
    public void waitForTestResults() {
        synchronized (testResultGettingWaiter) {
            while (((blockerBlueprintBundles.size() > 0) || (systemBundle.getState() != Bundle.ACTIVE)
                    || (testRunnerQueue.size() > 0)) && !stopped.get()) {
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
                    && activeBlockers.size() > 0) {
                LOGGER.info(testRunnerQueue.size() + " test runners are waiting to run");
                try {
                    testRunningWaiter.wait(BLOCKING_CAUSE_LOG_PERIOD);
                    if (activeBlockers.size() > 0 && !stopped.get()) {
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

    @Override
    public void stop() {
        if (stopped.compareAndSet(false, true)) {
            stopStartedTestRunners();
            blockingManagerThread.interrupt();
        } else {
            LOGGER.warn("Stop called on Test Runner BlockingManager while it was already stopped");
        }

    }
}
