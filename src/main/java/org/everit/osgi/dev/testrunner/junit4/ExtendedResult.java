package org.everit.osgi.dev.testrunner.junit4;

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
import java.util.Date;
import java.util.List;

/**
 * An extended type of {@link org.junit.runner.Result} to be able to provide more information after the tests of a
 * provided OSGI Test Service finished running.
 */
public class ExtendedResult {

    /**
     * The results of TestCases that ran.
     */
    private List<TestCaseResult> testCaseResults = new ArrayList<TestCaseResult>();

    /**
     * The count of failures.
     */
    private long failureCount = 0;

    /**
     * The count of errors.
     */
    private long errorCount = 0;

    /**
     * The time the test was started.
     */
    private long startTime;

    /**
     * The time when the test finished running.
     */
    private Long finishTime;

    /**
     * The count of tests that ran.
     */
    private long runCount = 0;

    /**
     * The count of tests that were ignored.
     */
    private long ignoreCount = 0;

    /**
     * Constructor of the class.
     * 
     * @param startTime
     *            The time when the test was started.
     */
    public ExtendedResult(final long startTime) {
        this.startTime = startTime;
    }

    /**
     * Called when all tests finished running.
     */
    public void finishRunning() {
        finishTime = new Date().getTime();
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

    /**
     * Getting the amount of time the tests were running.
     * 
     * @return Current time - startTime if the test running has not finished yet or the finishTime - startTime.
     */
    public long getRunTime() {
        if (finishTime == null) {
            return new Date().getTime() - startTime;
        } else {
            return finishTime.longValue() - startTime;
        }
    }

    public List<TestCaseResult> getTestCaseResults() {
        return testCaseResults;
    }

    /**
     * Incrementing the value of errorCount property.
     */
    public void incrementErrorCount() {
        errorCount++;
    }

    /**
     * Incrementing the value of failureCount property.
     */
    public void incrementFailureCount() {
        failureCount++;
    }

    /**
     * Incrementing the ignoreCount property value by one.
     */
    public void incrementIgnoreCount() {
        ignoreCount++;
    }

    /**
     * Incrementing the runCount property value by one.
     */
    public void incrementRunCount() {
        runCount++;
    }
}
