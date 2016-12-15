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

/**
 * Stores the data of the result of a test case.
 */
public class TestCaseResult {

  private final Throwable failure;

  /**
   * The time the TestCase stopped to run.
   */
  private final Long finishTime;

  /**
   * The starting time of the TestCase.
   */
  private final Long startTime;

  private final String testMethodName;

  /**
   * Constructor of the class that sets the properties that should be available already.
   *
   * @param startTime
   *          The time when the TestCase was started.
   */
  public TestCaseResult(final String testMethodName, final Long startTime, final Long finishTime,
      final Throwable failure) {
    this.testMethodName = testMethodName;
    this.startTime = startTime;
    this.finishTime = finishTime;
    this.failure = failure;
  }

  public Throwable getFailure() {
    return failure;
  }

  public Long getFinishTime() {
    return finishTime;
  }

  /**
   * Gives back the amount of time while the TestCase was running or null if the TestCase has not
   * finished yet.
   *
   * @return FinishTime - StartTime.
   */
  public Long getRunningTime() {
    if ((startTime == null) || (finishTime == null)) {
      return null;
    } else {
      return finishTime.longValue() - startTime.longValue();
    }
  }

  public long getStartTime() {
    return startTime;
  }

  public String getTestMethodName() {
    return testMethodName;
  }

  @Override
  public String toString() {
    return "TestCaseResult [methodName=" + testMethodName + ", startTime=" + startTime
        + ", finishTime="
        + finishTime
        + ", failure=" + failure + ", running time=" + getRunningTime() + "ms]";
  }

}
