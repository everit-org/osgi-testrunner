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
package org.everit.osgi.dev.testrunner;

/**
 * Constants of the test runner.
 */
public final class TestRunnerConstants {

  /**
   * The time in ms until the testrunner will wait for non-deamon threads stopping before exiting
   * the vm when {@link #ENV_STOP_AFTER_TESTS} environment variable is set to "true".
   */
  public static final int DEFAULT_SHUTDOWN_TIMEOUT = 5000;

  /**
   * Environment variable that indicates that the framework should be stopped after running the
   * tests.
   */
  public static final String ENV_STOP_AFTER_TESTS = "EOSGI_STOP_AFTER_TESTS";

  /**
   * The name of the Environment Variable that points to the folder where TEXT and XML based test
   * results should be dumped.
   */
  public static final String ENV_TEST_RESULT_FOLDER = "EOSGI_TEST_RESULT_FOLDER";

  /**
   * Constant of the MANIFEST header key to count expected number of tests per bundle.
   */
  public static final String HEADER_EXPECTED_NUMBER_OF_TESTS = "EOSGi-TestNum";

  /**
   * Optional property for test services. The property must be available if multiple tests are
   * registered as OSGi service based on the same interface.
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

  private TestRunnerConstants() {
    // Do nothing
  }
}
