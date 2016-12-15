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
 * When the JVM is started in the way that the
 * {@link org.everit.osgi.dev.testrunner.TestRunnerConstants#ENV_STOP_AFTER_TESTS} environment
 * variable is set, the testrunner stops the JVM as soon as there is no thread that is in
 * {@link java.lang.Thread.State#RUNNABLE}. By implementing a {@link ShutdownBlocker}, it is
 * possible to make the testrunner waiting a bit more. This can be useful when the JVM has to wait
 * for pheripherials during startup.
 */
public interface ShutdownBlocker {

  /**
   * Adding a listener to the Blocker so it can notify the listener about blocking and unblocking.
   * Normally at least there is one listener that is the test runner itself.
   */
  void addBlockListener(BlockListener blockListener);

  /**
   * The {@link BlockingManager} calls this function periodically to be able to log out the causes
   * of the blocked tests.
   *
   * @param sb
   *          The causes should be written into the {@link StringBuilder}. It is recommended to
   *          start every line with two spaces and write a line break onto the end of the message to
   *          have pretty output.
   */
  void logBlockCauses(StringBuilder sb);

  /**
   * Removing a blocking listener so it does not have to be notified anymore.
   *
   * @param blockListener
   *          The listener instance.
   */
  void removeBlockListener(BlockListener blockListener);
}
