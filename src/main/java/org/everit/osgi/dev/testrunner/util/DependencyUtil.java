package org.everit.osgi.dev.testrunner.util;

/**
 * Helper methods to check whether a technology is available in the OSGi framework.
 */
public class DependencyUtil {

    private static final boolean BLUEPRINT_AVAILABLE;
    
    static {
        ClassLoader classLoader = DependencyUtil.class.getClassLoader();
        boolean blueprintAvailable = true;
        try {
            classLoader.loadClass("org.osgi.service.blueprint.container.BlueprintListener");
        } catch (ClassNotFoundException e) {
            blueprintAvailable = false;
        }
        BLUEPRINT_AVAILABLE = blueprintAvailable;
    }

    public static final boolean isBlueprintAvailable() {
        return BLUEPRINT_AVAILABLE;
    }
}
