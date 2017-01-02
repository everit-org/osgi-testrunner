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

import java.util.Map;

import aQute.bnd.annotation.ConsumerType;

/**
 * The class that implements this interface can run tests after the framework is started.
 */
@ConsumerType
public interface TestEngine {

  /**
   * Runs a test based on a test object instance.
   *
   * @param testObject
   *          The test object instance that contains the test cases.
   *
   * @return The test results of the specified test object.
   */
  TestClassResult runTestsOfInstance(Object testObject, Map<String, ?> properties);

}
