package org.everit.osgi.dev.testrunner.engine;

/*
 * Copyright (c) 2011, Everit Kft.
 *
 * All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

import java.util.ArrayList;
import java.util.List;

public class TestClassResult {

    private List<TestCaseResult> testCaseResults = new ArrayList<TestCaseResult>();

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

    public TestClassResult(String className, long runCount, long errorCount, long failureCount, long ignoreCount,
            long startTime, long finishTime, List<TestCaseResult> testCaseResults) {
        this.className = className;
        this.runCount = runCount;
        this.errorCount = errorCount;
        this.failureCount = failureCount;
        this.ignoreCount = ignoreCount;
        this.startTime = startTime;
        this.finishTime = finishTime;
        this.testCaseResults = testCaseResults;
    }

    public List<TestCaseResult> getTestCaseResults() {
        return new ArrayList<TestCaseResult>(testCaseResults);
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

    public long getStartTime() {
        return startTime;
    }

    public long getRunTime() {
        return finishTime - startTime;
    }
    
    public String getClassName() {
        return className;
    }

    @Override
    public String toString() {
        return "TestClassResult [className=" + className + ", errorCount=" + errorCount + ", failureCount="
                + failureCount + ", ignoreCount=" + ignoreCount + ", runCount=" + runCount + ", startTime=" + startTime
                + ", finishTime=" + finishTime + ", testCaseResults=" + testCaseResults + ", running time="
                + getRunTime() + "ms]";
    }
    
    

}
