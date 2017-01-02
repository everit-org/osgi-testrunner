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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import org.everit.osgi.dev.testrunner.blocking.BlockListener;
import org.everit.osgi.dev.testrunner.blocking.ShutdownBlocker;
import org.everit.osgi.dev.testrunner.engine.TestClassResult;
import org.everit.osgi.dev.testrunner.internal.util.BundleUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * A manager that handles all the blocking causes why the tests should not start and starts them
 * when there is no more cause. Many technologies can have their starting process asynchronously. In
 * that case technology based {@link ShutdownBlocker} implementations can monitor their state and
 * notify the {@link BlockingManager} when the tests are ready to run.
 */
public final class BlockingManagerImpl {

  /**
   * Handles the event of blocker OSGi service appearance and disappearance.
   */
  private class BlockerServiceTrackerCustomizer
      implements ServiceTrackerCustomizer<ShutdownBlocker, ShutdownBlocker> {

    @Override
    public ShutdownBlocker addingService(final ServiceReference<ShutdownBlocker> reference) {
      final ShutdownBlocker blocker = bundleContext.getService(reference);

      BlockListener blockListener = new BlockListener() {

        @Override
        public void block() {
          activeBlockersLock.lock();

          try {
            activeBlockers.add(blocker);
          } finally {
            activeBlockersLock.unlock();
          }
        }

        @Override
        public void unblock() {
          activeBlockersLock.lock();
          try {
            activeBlockers.remove(blocker);
            if (activeBlockers.size() == 0) {
              activeBlockersEmptyCondition.signalAll();
            }
          } finally {
            activeBlockersLock.unlock();
          }
        }
      };

      listenersByBlockers.put(blocker, blockListener);

      blocker.addBlockListener(blockListener);
      return blocker;
    }

    @Override
    public void modifiedService(final ServiceReference<ShutdownBlocker> reference,
        final ShutdownBlocker service) {
    }

    @Override
    public void removedService(final ServiceReference<ShutdownBlocker> reference,
        final ShutdownBlocker blocker) {
      BlockListener blockListener = listenersByBlockers.remove(blocker);
      if (blockListener != null) {
        blocker.removeBlockListener(blockListener);
      }
      activeBlockersLock.lock();
      try {
        activeBlockers.remove(blocker);
      } finally {
        activeBlockersLock.unlock();
      }
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
   * The max. length in a string of a bundle state name. This is used for pretty output during
   * logging.
   */
  private static final int BUNDLE_STATE_NAME_MAX_LENGTH = 12;

  /**
   * Logger of class.
   */
  private static final Logger LOGGER = Logger.getLogger(BlockingManagerImpl.class.getName());

  /**
   * The blockers that are currently blocking the test runners.
   */
  private final Set<ShutdownBlocker> activeBlockers =
      new HashSet<>();

  private final Condition activeBlockersEmptyCondition;

  private final ReentrantLock activeBlockersLock;

  private ServiceTracker<ShutdownBlocker, ShutdownBlocker> blockerTracker;

  /**
   * The thread that starts waits for all block causes and starts the tests when there is no more
   * cause. This has to be on a new thread as otherwise the whole framework starting would be
   * blocked and there would be a deadlock. This thread is interrupted when the manager is stopped
   * (e.g. due to a timeout).
   */
  private Thread blockingManagerThread;

  /**
   * The context of the current bundle.
   */
  private final BundleContext bundleContext;

  private final Map<ShutdownBlocker, BlockListener> listenersByBlockers =
      new ConcurrentHashMap<>();

  /**
   * A flag that indicates whether this manager is stopped or not.
   */
  private final AtomicBoolean stopped = new AtomicBoolean(true);

  {
    activeBlockersLock = new ReentrantLock();
    activeBlockersEmptyCondition = activeBlockersLock.newCondition();
  }

  /**
   * Constructor of blocking manager.
   *
   * @param bundleContext
   *          bundle context that will be used to get the system bundle.
   */
  public BlockingManagerImpl(final BundleContext bundleContext) {
    this.bundleContext = bundleContext;
  }

  /**
   * Notify all tracked blockers about new test result.
   *
   * @param testClassResult
   *          The execution result of the test class.
   */
  public void handleTestClassResult(final TestClassResult testClassResult) {
    Set<ShutdownBlocker> trackedBlockers = listenersByBlockers.keySet();
    for (ShutdownBlocker shutdownBlocker : trackedBlockers) {
      shutdownBlocker.handleTestClassResult(testClassResult);
    }
  }

  private void logBlockCauses() {
    StringBuilder sb = new StringBuilder("Test running is blocked due to the following reasons:\n");
    for (ShutdownBlocker blocker : activeBlockers) {
      sb.append("Blocker ").append(blocker.toString()).append('\n');
      blocker.logBlockCauses(sb);
    }
    LOGGER.info(sb.toString());
  }

  /**
   * In case not all of the tests were run, a cause can be that not all of the bundles started
   * properly. This function logs out the bundle names that are not in active state after the
   * framework is started.
   */
  private void logNonStartedBundles() {
    Bundle[] bundles = bundleContext.getBundles();
    List<Bundle> nonStartedBundles = new ArrayList<>();
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
        sb.append(bundle.getSymbolicName()).append("_").append(bundle.getVersion().toString())
            .append("\n");
      }
      LOGGER.warning(sb.toString());
    }
  }

  /**
   * Starting the blocking manager, it will track for blocker services and will have a blocker
   * thread.
   */
  public void start() {

    if (stopped.compareAndSet(true, false)) {
      blockerTracker =
          new ServiceTracker<>(bundleContext, ShutdownBlocker.class,
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

  /**
   * Stopping the blocking manager.
   */
  public void stop() {
    if (stopped.compareAndSet(false, true)) {
      blockerTracker.close();
      activeBlockersLock.lock();
      try {
        activeBlockersEmptyCondition.signalAll();
      } finally {
        activeBlockersLock.unlock();
      }
      blockingManagerThread.interrupt();
    } else {
      LOGGER.warning("Stop called on Test Runner BlockingManager while it was already stopped");
    }

  }

  /**
   * Waiting for no blocking cause to have in the system (every tests ran).
   *
   * @param timeout
   *          The timeout while the function should wait.
   * @return True if there is no blocking cause when this function returns, false otherwise.
   */
  public boolean waitForNoBlockCause(final long timeout) {
    activeBlockersLock.lock();
    try {
      if (!stopped.get() && (activeBlockers.size() > 0)) {
        if (timeout == 0) {
          while (activeBlockers.size() > 0) {
            activeBlockersEmptyCondition.await();
          }
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
