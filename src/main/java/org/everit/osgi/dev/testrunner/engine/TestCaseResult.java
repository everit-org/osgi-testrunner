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

  public Throwable failure;

  /**
   * The time the TestCase stopped to run.
   */
  public long finishTime;

  /**
   * The starting time of the TestCase.
   */
  public long startTime;

  public String testMethodName;

  @Override
  public String toString() {
    return "TestCaseResult [methodName=" + testMethodName + ", startTime=" + startTime
        + ", finishTime="
        + finishTime
        + ", failure=" + failure + ", running time=" + (finishTime - startTime) + "ms]";
  }

}
