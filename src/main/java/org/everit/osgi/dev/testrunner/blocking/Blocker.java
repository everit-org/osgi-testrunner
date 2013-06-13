package org.everit.osgi.dev.testrunner.blocking;

import org.osgi.framework.BundleContext;

public interface Blocker {

    void configure(BlockListener listener, BundleContext context);

    void logBlockCauses(StringBuilder sb);
    
    void start();
    
    void stop();
}
