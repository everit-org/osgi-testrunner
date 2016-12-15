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
package org.everit.osgi.dev.testrunner.internal;

import org.everit.osgi.dev.testrunner.engine.TestEngine;

/**
 * Interface of tracking test runner engines.
 */
public interface TestRunnerEngineTracker {

  /**
   * Returns a test runner for the engine type if available, otherwise null.
   *
   * @return a test runner for the engine type if available, otherwise null.
   */
  TestEngine getEngineByType(String testEngineType);
}
