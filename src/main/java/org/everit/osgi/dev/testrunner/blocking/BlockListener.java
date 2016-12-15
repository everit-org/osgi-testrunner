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
 * A {@link ShutdownBlocker} can notify the {@link BlockingManager} that is should block or let
 * starting the test runners via this listener.
 */
public interface BlockListener {

  /**
   * The {@link BlockingManager} should not start the test runners yet.
   */
  void block();

  /**
   * The {@link BlockingManager} should start the test runners if no other {@link ShutdownBlocker}
   * blocks.
   */
  void unblock();
}
