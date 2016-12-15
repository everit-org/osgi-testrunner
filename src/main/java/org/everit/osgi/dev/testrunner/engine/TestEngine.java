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
package org.everit.osgi.dev.testrunner.engine;

import java.util.List;

import org.everit.osgi.dev.testrunner.TestDuringDevelopment;
import org.osgi.framework.ServiceReference;

/**
 * The class that implements this interface can run tests after the framework is started.
 */
public interface TestEngine {

  /**
   * Runs a test based on the service reference. The function is not called multiple times parallel.
   *
   * @param reference
   *          The service reference that contains the test.
   * @param developmentMode
   *          In development mode tests should not run unless the class or method is annotated with
   *          {@link TestDuringDevelopment} annotation.
   *
   * @return The test result of all classes that belong to this reference.
   */
  List<TestClassResult> runTest(ServiceReference<Object> reference, boolean developmentMode);

}
