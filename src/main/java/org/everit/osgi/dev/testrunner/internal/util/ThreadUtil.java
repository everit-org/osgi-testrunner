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
package org.everit.osgi.dev.testrunner.internal.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Util methods for JVM thread handling.
 */
public final class ThreadUtil {

  /**
   * Returns all of the running daemon threads.
   *
   * @return The daemon threads.
   */
  public List<Thread> countDeamonThreads() {
    List<Thread> result = new ArrayList<Thread>();
    Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
    for (Entry<Thread, StackTraceElement[]> threadAndStackTrace : allStackTraces.entrySet()) {
      Thread thread = threadAndStackTrace.getKey();
      if (!thread.isDaemon() && !Thread.State.TERMINATED.equals(thread.getState())
          && !thread.equals(Thread.currentThread())
          && (threadAndStackTrace.getValue().length > 0)) {
        result.add(thread);
      }
    }
    return result;
  }

}
