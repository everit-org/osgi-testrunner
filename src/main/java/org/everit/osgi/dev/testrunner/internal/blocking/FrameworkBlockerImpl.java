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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;

/**
 * One of the main {@link Blocker}s of this technology that blocks test running until the framework bundle is started.
 */
public class FrameworkBlockerImpl implements Blocker, FrameworkListener {

    /**
     * Whether this {@link Blocker} currently blocks the {@link BlockingManager} or not.
     */
    private boolean blocking = false;

    /**
     * The context of the testrunner bundle.
     */
    private BundleContext bundleContext;

    /**
     * The listener to notify the {@link BlockingManager} about blocking events.
     */
    private BlockListener blockListener;

    @Override
    public void frameworkEvent(final FrameworkEvent event) {
        if (event.getType() == FrameworkEvent.STARTED) {
            blockListener.unblock();
            blocking = false;
        }
    }

    @Override
    public void logBlockCauses(final StringBuilder sb) {
        if (blocking) {
            sb.append("    Framework is not started yet");
        }
    }

    @Override
    public void start(final BlockListener listener, final BundleContext context) {
        blockListener = listener;
        bundleContext = context;
        context.addFrameworkListener(this);
        Bundle frameworkBundle = context.getBundle(0);
        if (frameworkBundle.getState() != Bundle.ACTIVE) {
            blocking = true;
            listener.block();
        } else {
            blocking = false;
        }

    }

    @Override
    public void stop() {
        bundleContext.removeFrameworkListener(this);
    }

}
