package org.everit.osgi.dev.testrunner;

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

import java.util.HashSet;
import java.util.Set;

public final class Constants {

    /**
     * Environment variable that indicates that the framework should be stopped after running the tests.
     */
    public static String ENV_STOP_AFTER_TESTS = "EOSGI_STOP_AFTER_TESTS";

    /**
     * Required service property for test services and test engine services.
     */
    public static String SERVICE_PROPERTY_TESTRUNNER_ENGINE_TYPE = "osgitest.testEngine";

    /**
     * Optional property for test services. The property must be available if multiple tests are registered as OSGi
     * service based on the same interface.
     */
    public static String SERVICE_PROPERTY_TEST_ID = "osgitest.id";

    /**
     * The time in ms until the testrunner will wait for non-deamon threads stopping before exiting the vm when
     * {@link #ENV_STOP_AFTER_TESTS} environment variable is set to "true".
     */
    public static final int DEFAULT_SHUTDOWN_TIMEOUT = 5000;

    /**
     * The name of the Environment Variable that points to the folder where TEXT and XML based test results should be
     * dumped.
     */
    public static final String ENV_TEST_RESULT_FOLDER = "EOSGI_TEST_RESULT_FOLDER";

    /**
     * The name of the file that is written if there is an error during system exit.
     */
    public static final String SYSTEM_EXIT_ERROR_FILE_NAME = "system-exit-error.txt";

    /**
     * The name of non-daemon threads that are started by the system. These threads do not have to be interrupted before
     * a system exit.
     */
    public static final Set<String> SYSTEM_NON_DAEMON_THREAD_NAMES;

    static {
        SYSTEM_NON_DAEMON_THREAD_NAMES = new HashSet<String>();
        SYSTEM_NON_DAEMON_THREAD_NAMES.add("DestroyJavaVM");
    }

    private Constants() {
        // Do nothing
    }
}
