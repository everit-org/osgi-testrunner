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

import java.util.List;

import org.everit.osgi.dev.testrunner.engine.TestClassResult;
import org.osgi.framework.ServiceReference;

/**
 * Via the TestManager OSGi service 3rd party tools can specify which tests should run after a
 * bundle deployment.
 */
public interface TestManager {

  /**
   * Checking if the JVM is in development mode from the perspective of the test runner. By default,
   * JVM is in development mode if the {@link TestRunnerConstants#ENV_STOP_AFTER_TESTS} environment
   * variable is set to true.
   *
   * @return The development mode flag.
   */
  boolean isInDevelopmentMode();

  /**
   * Runs all tests that are found based on the service reference.
   *
   * @param reference
   *          The service reference that points to test objects.
   * @param force
   *          Force to run test methods in development mode even if they are not annotated to do so.
   *          See {@link #setInDevelopmentMode(boolean)}.
   *
   * @return The test results.
   */
  List<TestClassResult> runTest(ServiceReference<Object> reference, boolean force);

  /**
   * Setting the test runner to beleive that the JVM is in development mode or not. By default, JVM
   * is in development mode if the {@link TestRunnerConstants#ENV_STOP_AFTER_TESTS} environment
   * variable is set to true.
   *
   * @param inDevelopmentMode
   *          The development
   */
  void setInDevelopmentMode(boolean inDevelopmentMode);
}
