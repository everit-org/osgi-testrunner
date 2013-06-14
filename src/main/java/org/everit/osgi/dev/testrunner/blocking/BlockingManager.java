package org.everit.osgi.dev.testrunner.blocking;

import org.osgi.framework.BundleContext;

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

/**
 * Helps waiting until Framework is started and all Blueprint code did run. Tests will be started after all these events
 * happened.
 */
public interface BlockingManager {

    /**
     * Environment variable that indicates that the framework should be stopped after running the tests.
     */
    String ENV_STOP_AFTER_TESTS = "EOSGI_STOP_AFTER_TESTS";

    /**
     * Adding a new test runner that may run tests after the framework fully started.
     * 
     * @param testRunner
     *            The test runner.
     */
    void addTestRunner(BlockedTestRunner testRunner);

    /**
     * Running tests from the queue. This method should be called from a new thread as it waits until all of the events
     * are finished that may make tests not available.
     * 
     * @param context
     *            The {@link BundleContext} of the testrunner bundle.
     */
    void start(BundleContext context);

    /**
     * Stopping the blocking manager. No tests will be run after this.
     */
    void stop();

    /**
     * This function takes the thread into waiting state until the set of blocker bundles is empty.
     */
    void waitForTestResults();

}
