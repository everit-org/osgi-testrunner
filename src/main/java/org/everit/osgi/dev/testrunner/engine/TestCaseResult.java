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

public class TestCaseResult {

    private final Throwable failure;

    /**
     * The time the TestCase stopped to run.
     */
    private final Long finishTime;

    private final String testMethodName;

    /**
     * The starting time of the TestCase.
     */
    private final Long startTime;

    /**
     * Constructor of the class that sets the properties that should be available already.
     * 
     * @param startTime
     *            The time when the TestCase was started.
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
     * Gives back the amount of time while the TestCase was running or null if the TestCase has not finished yet.
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
        return "TestCaseResult [methodName=" + testMethodName + ", startTime=" + startTime + ", finishTime="
                + finishTime
                + ", failure=" + failure + ", running time=" + getRunningTime() + "ms]";
    }

}
