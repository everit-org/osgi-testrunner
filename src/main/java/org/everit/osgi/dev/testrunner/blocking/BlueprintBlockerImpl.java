package org.everit.osgi.dev.testrunner.blocking;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.osgi.service.blueprint.container.BlueprintListener;

public class BlueprintBlockerImpl implements Blocker, BlueprintListener {

    /**
     * Map for storing the blocker bundle ids with the Blueprint events that show what is blocking.
     */
    private final Map<Long, BlueprintEvent> blockerBlueprintBundles =
            new ConcurrentHashMap<Long, BlueprintEvent>();

    private BlockListener listener;

    private boolean blockingState = false;

    private volatile Object helper = new Object();

    @Override
    public void configure(BlockListener listener, BundleContext context) {
        this.listener = listener;
        // TODO Auto-generated method stub

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
                    listener.unblock();
                }
            }
        }
    }

    private void addBlockingBlueprintBundle(final BlueprintEvent event) {
        blockerBlueprintBundles.put(event.getBundle().getBundleId(), event);
        if (!blockingState) {
            synchronized (helper) {
                if (!blockingState && blockerBlueprintBundles.size() > 0) {
                    blockingState = true;
                    listener.block();
                }
            }
        }
    }

    @Override
    public void start() {
        // TODO Auto-generated method stub

    }

    @Override
    public void stop() {
        // TODO Auto-generated method stub

    }

    @Override
    public void logBlockCauses(StringBuilder sb) {
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

}
