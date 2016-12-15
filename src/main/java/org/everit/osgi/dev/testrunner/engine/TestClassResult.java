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
  private final String className;

  /**
   * The count of errors.
   */
  private final long errorCount;

  /**
   * The count of failures.
   */
  private final long failureCount;

  /**
   * The time when the test finished running.
   */
  private final long finishTime;

  /**
   * The count of tests that were ignored.
   */
  private final long ignoreCount;

  /**
   * The count of tests that ran.
   */
  private final long runCount;

  /**
   * The time the test was started.
   */
  private final long startTime;

  private List<TestCaseResult> testCaseResults = new ArrayList<TestCaseResult>();

  /**
   * Constructor.
   *
   * @param parameterObject
   *          The data of the test result.
   */
  public TestClassResult(final TestClassResultParameter parameterObject) {
    className = parameterObject.className;
    runCount = parameterObject.runCount;
    errorCount = parameterObject.errorCount;
    failureCount = parameterObject.failureCount;
    ignoreCount = parameterObject.ignoreCount;
    startTime = parameterObject.startTime;
    finishTime = parameterObject.finishTime;
    testCaseResults = parameterObject.testCaseResults;
  }

  public String getClassName() {
    return className;
  }

  public long getErrorCount() {
    return errorCount;
  }

  public long getFailureCount() {
    return failureCount;
  }

  public Long getFinishTime() {
    return finishTime;
  }

  public long getIgnoreCount() {
    return ignoreCount;
  }

  public long getRunCount() {
    return runCount;
  }

  public long getRunTime() {
    return finishTime - startTime;
  }

  public long getStartTime() {
    return startTime;
  }

  public List<TestCaseResult> getTestCaseResults() {
    return new ArrayList<TestCaseResult>(testCaseResults);
  }

  @Override
  public String toString() {
    return "TestClassResult [className=" + className + ", errorCount=" + errorCount
        + ", failureCount="
        + failureCount + ", ignoreCount=" + ignoreCount + ", runCount=" + runCount + ", startTime="
        + startTime
        + ", finishTime=" + finishTime + ", testCaseResults=" + testCaseResults + ", running time="
        + getRunTime() + "ms]";
  }

}
