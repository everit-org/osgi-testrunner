package org.everit.osgi.dev.testrunner;

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
/**
 * Class that holds all of the summary of results belonging to a Class. These results are normally queried by the maven
 * plugin to be able to show what happened in the container.
 */
public class GlobalResult {

    /**
     * Number of failures.
     */
    private long failureCount = 0;

    /**
     * Number of errors.
     */
    private long errorCount = 0;

    /**
     * Number of tests that ran.
     */
    private long runCount = 0;

    /**
     * Number of tests that were ignored.
     */
    private long ignoreCount = 0;

    /**
     * The time until all of the tests were running.
     */
    private long runTime = 0;

    public long getErrorCount() {
        return errorCount;
    }

    public long getFailureCount() {
        return failureCount;
    }

    public long getIgnoreCount() {
        return ignoreCount;
    }

    public long getRunCount() {
        return runCount;
    }

    public long getRunTime() {
        return runTime;
    }

    public void setErrorCount(final long errorCount) {
        this.errorCount = errorCount;
    }

    public void setFailureCount(final long failureCount) {
        this.failureCount = failureCount;
    }

    public void setIgnoreCount(final long ignoreCount) {
        this.ignoreCount = ignoreCount;
    }

    public void setRunCount(final long runCount) {
        this.runCount = runCount;
    }

    public void setRunTime(final long runTime) {
        this.runTime = runTime;
    }

}
