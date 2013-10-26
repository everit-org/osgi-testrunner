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

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.osgi.service.blueprint.container.BlueprintListener;

/**
 * {@link Blocker} implementation of Blueprint technology. Please note that in future versions this will be moved to a
 * separate project.
 */
public class BlueprintBlockerImpl implements Blocker, BlueprintListener {

    /**
     * Map for storing the blocker bundle ids with the Blueprint events that show what is blocking.
     */
    private final Map<Long, BlueprintEvent> blockerBlueprintBundles =
            new ConcurrentHashMap<Long, BlueprintEvent>();

    /**
     * Whether or not this {@link Blocker} currently blocks the {@link BlockingManager}.
     */
    private boolean blockingState = false;

    /**
     * A service is created by this activator that listens for blueprint events. This is necessary in order to block the
     * test runner code until each bundle arrives to the state of CREATED or FAILED in case of Blueprint bundles.
     */
    private ServiceRegistration<BlueprintListener> blueprintServiceRegistration;

    /**
     * Helper object for thread synchronization.
     */
    private volatile Object helper = new Object();

    /**
     * The listener to notify the {@link BlockingManager} about blocking related events.
     */
    private BlockListener blockListener;

    private void addBlockingBlueprintBundle(final BlueprintEvent event) {
        blockerBlueprintBundles.put(event.getBundle().getBundleId(), event);
        if (!blockingState) {
            synchronized (helper) {
                if (!blockingState && (blockerBlueprintBundles.size() > 0)) {
                    blockingState = true;
                    blockListener.block();
                }
            }
        }
    }

    @Override
    public void blueprintEvent(final BlueprintEvent event) {
        int eventType = event.getType();
        if (eventType == BlueprintEvent.CREATED) {
            removeBlockingBlueprintBundle(event);
        } else if (eventType == BlueprintEvent.CREATING) {
            addBlockingBlueprintBundle(event);
        } else if (eventType == BlueprintEvent.DESTROYED) {
            removeBlockingBlueprintBundle(event);
        } else if (eventType == BlueprintEvent.DESTROYING) {
            addBlockingBlueprintBundle(event);
        } else if (eventType == BlueprintEvent.FAILURE) {
            removeBlockingBlueprintBundle(event);
        } else if (eventType == BlueprintEvent.GRACE_PERIOD) {
            addBlockingBlueprintBundle(event);
        } else if (eventType == BlueprintEvent.WAITING) {
            addBlockingBlueprintBundle(event);
        }
    }

    @Override
    public void logBlockCauses(final StringBuilder sb) {
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
            synchronized (helper) {
                if (blockerBlueprintBundles.size() == 0) {
                    blockingState = false;
                    blockListener.unblock();
                }
            }
        }
    }

    @Override
    public void start(final BlockListener listener, final BundleContext context) {
        blockListener = listener;
        blueprintServiceRegistration = context.registerService(BlueprintListener.class,
                this,
                new Hashtable<String, Object>());
    }

    @Override
    public void stop() {
        blueprintServiceRegistration.unregister();
    }

}
