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

import org.everit.osgi.dev.testrunner.blocking.AbstractShutdownBlocker;
import org.everit.osgi.dev.testrunner.blocking.ShutdownBlocker;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;

/**
 * One of the main {@link ShutdownBlocker}s of this technology that blocks test running until the
 * framework bundle is started.
 */
public class FrameworkStartingShutdownBlockerImpl extends AbstractShutdownBlocker {

  /**
   * Whether this {@link ShutdownBlocker} currently blocks the {@link BlockingManager} or not.
   */
  private boolean blocking = false;

  /**
   * The context of the testrunner bundle.
   */
  private final BundleContext bundleContext;

  /**
   * The framework listener that notifies the blocking listeners when the framework is started.
   */
  private FrameworkListener frameworkListener;

  public FrameworkStartingShutdownBlockerImpl(final BundleContext bundleContext) {
    this.bundleContext = bundleContext;
  }

  @Override
  public void logBlockCauses(final StringBuilder sb) {
    if (blocking) {
      sb.append("  Framework has not been started yet");
    }
  }

  /**
   * Starting the blocker instance.
   */
  public void start() {
    frameworkListener = new FrameworkListener() {

      @Override
      public void frameworkEvent(final FrameworkEvent event) {
        if (event.getType() == FrameworkEvent.STARTED) {
          blocking = false;
          unblock();
        }

      }
    };
    bundleContext.addFrameworkListener(frameworkListener);

    Bundle frameworkBundle = bundleContext.getBundle(0);
    if (frameworkBundle.getState() != Bundle.ACTIVE) {
      blocking = true;
      block();
    } else {
      blocking = false;
    }

  }

  public void stop() {
    bundleContext.removeFrameworkListener(frameworkListener);
  }

}
