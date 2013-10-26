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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 * Extended listener for test running to be able to provide more information that JUnit provides by default.
 */
public class ExtendedResultListener extends RunListener {

    /**
     * The extended result.
     */
    private ExtendedResult result = new ExtendedResult(new Date().getTime());

    /**
     * The result of each test cases by their {@link Description}s.
     */
    private Map<Description, TestCaseResult> testCaseResults = new ConcurrentHashMap<Description, TestCaseResult>();

    public ExtendedResult getResult() {
        return result;
    }

    /**
     * Handling assumption and simple failures.
     * 
     * @param failure
     *            The failure object.
     */
    protected void handleFailure(final Failure failure) {
        TestCaseResult testCaseResult = testCaseResults.get(failure.getDescription());
        testCaseResult.finishRun();
        testCaseResult.setFailure(failure);
        Throwable exception = failure.getException();
        if (exception instanceof AssertionError) {
            result.incrementFailureCount();
        } else {
            result.incrementErrorCount();
        }
    }

    @Override
    public void testAssumptionFailure(final Failure failure) {
        handleFailure(failure);
    }

    @Override
    public void testFailure(final Failure failure) throws Exception {
        handleFailure(failure);
    }

    @Override
    public void testFinished(final Description description) throws Exception {
        TestCaseResult testCaseResult = testCaseResults.get(description);
        testCaseResult.finishRun();
    }

    @Override
    public void testIgnored(final Description description) throws Exception {
        TestCaseResult testCaseResult = new TestCaseResult(description, null);
        testCaseResults.put(description, testCaseResult);
        result.incrementIgnoreCount();
    }

    @Override
    public void testRunFinished(final Result presult) throws Exception {
        // this.result.setResult(result);
    }

    @Override
    public void testRunStarted(final Description description) throws Exception {
        // result = new ExtendedResult(description, new Date().getTime());
    }

    @Override
    public void testStarted(final Description description) throws Exception {
        TestCaseResult testCaseResult = new TestCaseResult(description, new Date().getTime());
        testCaseResults.put(description, testCaseResult);
        result.getTestCaseResults().add(testCaseResult);
        result.incrementRunCount();
    }

}
