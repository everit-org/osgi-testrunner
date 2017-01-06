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
package org.everit.osgi.dev.testrunner.blocking;

/**
 * A {@link ShutdownBlocker} can notify the test runner that it should not stop the JVM yet as some
 * process are still running or not all tests has been executed yet.
 */
public interface ShutdownBlockListener {

  /**
   * The test runner should not shut down the JVM until unblock is called.
   */
  void block();

  /**
   * The test runner can shut down the JVM if no other {@link ShutdownBlocker}s are blocking.
   */
  void unblock();
}
