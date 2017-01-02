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

import java.util.ArrayList;
import java.util.List;

/**
 * Test results of a whole test class.
 */
public class TestClassResult {

  /**
   * The name of the class that contained the test (in many cases this is an interface name).
   */
  public String className;

  /**
   * The count of errors.
   */
  public long errorCount;

  /**
   * The count of failures.
   */
  public long failureCount;

  /**
   * The time when the test finished running.
   */
  public long finishTime;

  /**
   * The count of tests that were ignored.
   */
  public long ignoreCount;

  /**
   * The count of tests that ran.
   */
  public long runCount;

  /**
   * The time the test was started.
   */
  public long startTime;

  public List<TestCaseResult> testCaseResults = new ArrayList<>();

  @Override
  public String toString() {
    return "TestClassResult [className=" + className + ", errorCount=" + errorCount
        + ", failureCount="
        + failureCount + ", ignoreCount=" + ignoreCount + ", runCount=" + runCount + ", startTime="
        + startTime
        + ", finishTime=" + finishTime + ", testCaseResults=" + testCaseResults + ", running time="
        + (finishTime - startTime) + "ms]";
  }

}
