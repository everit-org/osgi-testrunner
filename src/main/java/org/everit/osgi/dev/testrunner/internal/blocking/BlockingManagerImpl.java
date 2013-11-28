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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import org.everit.osgi.dev.testrunner.blocking.BlockListener;
import org.everit.osgi.dev.testrunner.blocking.ShutdownBlocker;
import org.everit.osgi.dev.testrunner.internal.util.BundleUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * A manager that handles all the blocking causes why the tests should not start and starts them when there is no more
 * cause. Many technologies can have their starting process asynchronously. In that case technology based
 * {@link ShutdownBlocker} implementations can monitor their state and notify the {@link BlockingManager} when the tests
 * are ready to run.
 */
public final class BlockingManagerImpl {

    private class BlockerServiceTrackerCustomizer implements ServiceTrackerCustomizer<ShutdownBlocker, ShutdownBlocker> {

        private Map<ShutdownBlocker, BlockListener> listenersByBlockers =
                new ConcurrentHashMap<ShutdownBlocker, BlockListener>();

        @Override
        public ShutdownBlocker addingService(final ServiceReference<ShutdownBlocker> reference) {
            final ShutdownBlocker blocker = bundleContext.getService(reference);
            BlockListener blockListener = new BlockListener() {

                @Override
                public void block() {
                    activeBlockersLock.lock();

                    activeBlockers.put(blocker, true);

                    activeBlockersLock.unlock();
                }

                @Override
                public void unblock() {
                    activeBlockersLock.lock();

                    activeBlockers.remove(blocker);
                    if (activeBlockers.size() == 0) {
                        activeBlockersEmptyCondition.signalAll();
                    }

                    activeBlockersLock.unlock();
                }
            };
            listenersByBlockers.put(blocker, blockListener);

            blocker.addBlockListener(blockListener);
            return blocker;
        }

        @Override
        public void modifiedService(final ServiceReference<ShutdownBlocker> reference, final ShutdownBlocker service) {
        }

        @Override
        public void removedService(final ServiceReference<ShutdownBlocker> reference, final ShutdownBlocker blocker) {
            BlockListener blockListener = listenersByBlockers.remove(blocker);
            if (blockListener != null) {
                blocker.removeBlockListener(blockListener);
            }
            activeBlockersLock.lock();
            activeBlockers.remove(blocker);
            activeBlockersLock.unlock();
            bundleContext.ungetService(reference);
        }

    }

    /**
     * The period of time when the causes of the blocks are logged in millisecs.
     */
    public static final long BLOCKING_CAUSE_LOG_PERIOD = 30000;

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
    private static final Logger LOGGER = Logger.getLogger(BlockingManagerImpl.class.getName());

    /**
     * The blockers that are currently blocking the test runners.
     */
    private Map<ShutdownBlocker, Boolean> activeBlockers = new HashMap<ShutdownBlocker, Boolean>();

    private final ReentrantLock activeBlockersLock = new ReentrantLock();

    private Condition activeBlockersEmptyCondition = activeBlockersLock.newCondition();

    private ServiceTracker<ShutdownBlocker, ShutdownBlocker> blockerTracker;

    /**
     * The thread that starts waits for all block causes and starts the tests when there is no more cause. This has to
     * be on a new thread as otherwise the whole framework starting would be blocked and there would be a deadlock. This
     * thread is interrupted when the manager is stopped (e.g. due to a timeout).
     */
    private Thread blockingManagerThread;

    /**
     * The context of the current bundle.
     */
    private BundleContext bundleContext;

    /**
     * A flag that indicates whether this manager is stopped or not.
     */
    private AtomicBoolean stopped = new AtomicBoolean(true);

    /**
     * Constructor of blocking manager.
     * 
     * @param bundleContext
     *            bundle context that will be used to get the system bundle.
     */
    public BlockingManagerImpl(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    private void logBlockCauses() {
        StringBuilder sb = new StringBuilder("Test running is blocked due to the following reasons:\n");
        for (ShutdownBlocker blocker : activeBlockers.keySet()) {
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
        Bundle[] bundles = bundleContext.getBundles();
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
            StringBuilder sb =
                    new StringBuilder("The following bundles are not started (this can be a cause if")
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
            LOGGER.warning(sb.toString());
        }
    }

    public void start() {

        if (stopped.compareAndSet(true, false)) {
            blockerTracker =
                    new ServiceTracker<ShutdownBlocker, ShutdownBlocker>(bundleContext, ShutdownBlocker.class,
                            new BlockerServiceTrackerCustomizer());
            blockerTracker.open();

            blockingManagerThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    while (!stopped.get() && !waitForNoBlockCause(BLOCKING_CAUSE_LOG_PERIOD)) {
                        logBlockCauses();
                    }

                    if (!stopped.get()) {
                        logNonStartedBundles();
                    }
                }
            });
            blockingManagerThread.start();
        } else {
            LOGGER.warning("Trying to start an already started BlockingManager");
        }
    }

    public void stop() {
        if (stopped.compareAndSet(false, true)) {
            blockerTracker.close();
            activeBlockersLock.lock();
            activeBlockersEmptyCondition.signalAll();
            activeBlockersLock.unlock();
            blockingManagerThread.interrupt();
        } else {
            LOGGER.warning("Stop called on Test Runner BlockingManager while it was already stopped");
        }

    }

    public boolean waitForNoBlockCause(final long timeout) {
        activeBlockersLock.lock();
        try {
            if (!stopped.get() && (activeBlockers.size() > 0)) {
                if (timeout == 0) {
                    activeBlockersEmptyCondition.await();
                    return true;
                } else {
                    return activeBlockersEmptyCondition.await(timeout, TimeUnit.MILLISECONDS);
                }
            } else {
                return true;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.severe("Test running waiting was interrupted");
            stop();
            return true;
        } finally {
            activeBlockersLock.unlock();
        }
    }
}
