/**
 * This file is part of OSGi Test Runner Bundle.
 *
 * OSGi Test Runner Bundle is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OSGi Test Runner Bundle is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with OSGi Test Runner Bundle.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.everit.osgi.dev.testrunner;

public final class Constants {

    /**
     * The time in ms until the testrunner will wait for non-deamon threads stopping before exiting the vm when
     * {@link #ENV_STOP_AFTER_TESTS} environment variable is set to "true".
     */
    public static final int DEFAULT_SHUTDOWN_TIMEOUT = 5000;

    /**
     * Environment variable that indicates that the framework should be stopped after running the tests.
     */
    public static final String ENV_STOP_AFTER_TESTS = "EOSGI_STOP_AFTER_TESTS";

    /**
     * The name of the Environment Variable that points to the folder where TEXT and XML based test results should be
     * dumped.
     */
    public static final String ENV_TEST_RESULT_FOLDER = "EOSGI_TEST_RESULT_FOLDER";

    /**
     * Optional property for test services. The property must be available if multiple tests are registered as OSGi
     * service based on the same interface.
     */
    public static final String SERVICE_PROPERTY_TEST_ID = "eosgi.testId";

    /**
     * Required service property for test services and test engine services.
     */
    public static final String SERVICE_PROPERTY_TESTRUNNER_ENGINE_TYPE = "eosgi.testEngine";

    /**
     * The name of the file that is written if there is an error during system exit.
     */
    public static final String SYSTEM_EXIT_ERROR_FILE_NAME = "system-exit-error.txt";

    /**
     * Constant of the MANIFEST header key to count expected number of tests per bundle.
     */
    public static final String HEADER_EXPECTED_NUMBER_OF_TESTS = "EOSGi-TestNum";

    private Constants() {
        // Do nothing
    }
}
