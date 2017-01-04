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
   * The attribute of the {@link #CAPABILITY_TESTCLASS_NAMESPACE} capability that tells how many
   * times the specified class should be executed before the test runner shuts down the VM. If not
   * specified, that means that the test class should run once.
   */
  public static final String CAPABILITY_TESTCLASS_ATTR_INSTANCE_COUNT = "instanceCount";

  /**
   * The name of the capability that tells the test runner that the bundle contains test cases that
   * the test runner should wait for before shutting down the OSGi container.
   */
  public static final String CAPABILITY_TESTCLASS_NAMESPACE = "eosgi.testClass";

  /**
   * The time in ms until the testrunner will wait for non-deamon threads stopping before exiting
   * the vm when {@link #PROP_STOP_AFTER_TESTS} environment variable is set to "true".
   */
  public static final int DEFAULT_SHUTDOWN_TIMEOUT = 5000;

  /**
   * Name of the System or Framework property that specifies if the framework is started in
   * development mode. If the OSGi container is in development mode (not during the integration-test
   * phase of the build), only those tests are executed after a re-deployment that are annotated
   * with @{@link TestDuringDevelopment}. The {@link Boolean#parseBoolean(String)} is used to
   * determine the value of this setting.
   */
  public static final String PROP_DEVELOPMENT_MODE = "eosgi.developmentMode";

  /**
   * System property that indicates that the framework should be stopped * after running the tests.
   */
  public static final String PROP_STOP_AFTER_TESTS = "eosgi.stopAfterTests";

  /**
   * The name of the system property that points to the folder where TEXT and XML based test results
   * should be dumped.
   */
  public static final String PROP_TEST_RESULT_FOLDER = "eosgi.testResultFolder";

  /**
   * The key of the property that contains the id of the test. Those OSGi services are picked up
   * that have this service property.
   */
  public static final String SERVICE_PROPERTY_TEST_ID = "eosgi.testId";

  /**
   * Required service property for test services and test engine services.
   */
  public static final String SERVICE_PROPERTY_TESTRUNNER_ENGINE = "eosgi.testEngine";

  /**
   * The name of the file that is written if there is an error during system exit.
   */
  public static final String SYSTEM_EXIT_ERROR_FILE_NAME = "system-exit-error.txt";

  private TestRunnerConstants() {
    // Do nothing
  }
}
