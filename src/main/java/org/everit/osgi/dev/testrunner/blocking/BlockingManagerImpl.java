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
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.everit.osgi.dev.testrunner.util.BundleUtil;
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
public final class BlockingManagerImpl implements BlueprintListener, FrameworkListener, BlockingManager {

    /**
     * Map for storing the blocker bundle ids with the Blueprint events that show what is blocking.
     */
    private final Map<Long, BlueprintEvent> blockerBlueprintBundles =
            new ConcurrentHashMap<Long, BlueprintEvent>();

    /**
     * Logger of class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockingManagerImpl.class);

    /**
     * The period of time when the causes of the blocks are logged in millisecs.
     */
    public static final int BLOCKING_CAUSE_LOG_PERIOD = 30000;

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
    public void blueprintEvent(final BlueprintEvent event) {
        int eventType = event.getType();
        if (eventType == BlueprintEvent.CREATED) {
            removeBlockingBlueprintBundle(event);
        } else if (eventType == BlueprintEvent.CREATING) {
            blockerBlueprintBundles.put(event.getBundle().getBundleId(), event);
        } else if (eventType == BlueprintEvent.DESTROYED) {
            removeBlockingBlueprintBundle(event);
        } else if (eventType == BlueprintEvent.DESTROYING) {
            blockerBlueprintBundles.put(event.getBundle().getBundleId(), event);
        } else if (eventType == BlueprintEvent.FAILURE) {
            removeBlockingBlueprintBundle(event);
        } else if (eventType == BlueprintEvent.GRACE_PERIOD) {
            blockerBlueprintBundles.put(event.getBundle().getBundleId(), event);
        } else if (eventType == BlueprintEvent.WAITING) {
            blockerBlueprintBundles.put(event.getBundle().getBundleId(), event);
        }
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
        for (BlueprintEvent blueprintEvent : blockerBlueprintBundles.values()) {
            Bundle bundle = blueprintEvent.getBundle();
            sb.append("  Bundle ").append(bundle.getSymbolicName());
            sb.append("[").append(bundle.getBundleId()).append("]").append(":\n");
            String[] dependencies = blueprintEvent.getDependencies();
            if (dependencies != null) {
                for (String dependency : dependencies) {
                    sb.append("    ").append(dependency).append("\n");
                }
            }
        }
        LOGGER.info(sb.toString());
    }

    /**
     * Remove a bundle from the set of blocker bundles. This normally happens when a Blueprint state changes to CREATED
     * or FAILED.
     * 
     * @param bundleId
     *            The id of the bundle.
     */
    private void removeBlockingBlueprintBundle(final BlueprintEvent blueprintEvent) {
        blockerBlueprintBundles.remove(blueprintEvent.getBundle().getBundleId());
        if (blockerBlueprintBundles.size() == 0) {
            synchronized (testRunningWaiter) {
                testRunningWaiter.notify();
            }
            synchronized (testResultGettingWaiter) {
                testResultGettingWaiter.notify();
            }
        }
    }

    @Override
    public void start() {
        if (stopped.compareAndSet(true, false)) {
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
            while (((blockerBlueprintBundles.size() > 0) || (systemBundle.getState() != Bundle.ACTIVE))
                    && !stopped.get()) {
                LOGGER.info(testRunnerQueue.size() + " test runners are waiting to run");
                try {
                    testRunningWaiter.wait(BLOCKING_CAUSE_LOG_PERIOD);
                    if (blockerBlueprintBundles.size() > 0) {
                        logBlockCauses();
                    }
                } catch (InterruptedException e) {
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
