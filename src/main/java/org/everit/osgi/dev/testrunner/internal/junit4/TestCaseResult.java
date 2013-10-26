package org.everit.osgi.dev.testrunner.internal.junit4;

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

import java.util.Date;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;

/**
 * Result of a JUnit TestCase.
 */
public class TestCaseResult {

    /**
     * The description of the TestCase.
     */
    private Description description;

    /**
     * The Failure description if the TestCase failed.
     */
    private Failure failure;

    /**
     * The time the TestCase stopped to run.
     */
    private Long finishTime;

    /**
     * The starting time of the TestCase.
     */
    private Long startTime;

    /**
     * Constructor of the class that sets the properties that should be available already.
     * 
     * @param description
     *            The description of the TestCase.
     * @param startTime
     *            The time when the TestCase was started.
     */
    public TestCaseResult(final Description description, final Long startTime) {
        super();
        this.description = description;
        this.startTime = startTime;
    }

    /**
     * Should be called when the TestCase running has finished. Sets the finishTime property.
     */
    public void finishRun() {
        finishTime = new Date().getTime();
    }

    public Description getDescription() {
        return description;
    }

    public Failure getFailure() {
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

    public void setDescription(final Description description) {
        this.description = description;
    }

    public void setFailure(final Failure failure) {
        this.failure = failure;
    }
}
