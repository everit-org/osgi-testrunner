package org.everit.osgi.dev.testrunner.internal;

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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.everit.osgi.dev.testrunner.Constants;
import org.everit.osgi.dev.testrunner.TestManager;
import org.everit.osgi.dev.testrunner.engine.TestClassResult;
import org.everit.osgi.dev.testrunner.engine.TestRunnerEngine;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestManagerImpl implements TestManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestManagerImpl.class);

    private Set<Filter> testInclusionFilters = new HashSet<Filter>();

    private ReadWriteLock testInclusionRWLock = new ReentrantReadWriteLock(false);

    private Set<Filter> testExclusionFilters = new HashSet<Filter>();

    private ReadWriteLock testExclusionRWLock = new ReentrantReadWriteLock(false);

    private final TestRunnerEngineTracker testRunnerEngineTracker;

    public TestManagerImpl(TestRunnerEngineTracker testRunnerEngineTracker) {
        this.testRunnerEngineTracker = testRunnerEngineTracker;
    }

    @Override
    public boolean addTestInclusionFilter(Filter filter) {
        Lock writeLock = testInclusionRWLock.writeLock();
        writeLock.lock();
        try {
            return testInclusionFilters.add(filter);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean addTestExclusionFilter(Filter filter) {
        Lock writeLock = testExclusionRWLock.writeLock();
        writeLock.lock();
        try {
            return testExclusionFilters.add(filter);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean removeTestInclusionFilter(Filter filter) {
        Lock writeLock = testInclusionRWLock.writeLock();
        writeLock.lock();
        try {
            return testInclusionFilters.remove(filter);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean removeTestExclusionFilter(Filter filter) {
        Lock writeLock = testExclusionRWLock.writeLock();
        writeLock.lock();
        try {
            return testExclusionFilters.remove(filter);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Set<Filter> getTestExclusionFilters() {
        Lock readLock = testExclusionRWLock.readLock();
        readLock.lock();
        try {
            return new HashSet<Filter>(testExclusionFilters);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Set<Filter> getTestInclusionFilters() {
        Lock readLock = testInclusionRWLock.readLock();
        readLock.lock();
        try {
            return new HashSet<Filter>(testInclusionFilters);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public List<TestClassResult> runTest(ServiceReference<Object> reference, boolean evenIfExcluded) {
        if (!evenIfExcluded && !shouldTestRun(reference)) {
            return null;
        }

        Object engineTypeObject = reference.getProperty(Constants.SERVICE_PROPERTY_TESTRUNNER_ENGINE_TYPE);
        if (engineTypeObject == null || !(engineTypeObject instanceof String)) {
            LOGGER.warn("Unrecognized '" + Constants.SERVICE_PROPERTY_TESTRUNNER_ENGINE_TYPE
                    + "' service property value for test. Ignoring: " + reference.toString());
            return null;
        }

        TestRunnerEngine runnerEngine = testRunnerEngineTracker.getEngineByType((String) engineTypeObject);
        if (runnerEngine == null) {
            LOGGER.warn("No test runner available for type '" + engineTypeObject + "'. Ignoring test: "
                    + reference.toString());
            return null;
        }

        List<TestClassResult> result = runnerEngine.runTest(reference);
        LOGGER.info("Test result: " + result.toString());
        return result;

    }

    private boolean shouldTestRun(ServiceReference<Object> reference) {
        Lock inclusionReadLock = testInclusionRWLock.readLock();
        inclusionReadLock.lock();

        try {
            for (Filter filter : testInclusionFilters) {
                boolean match = filter.match(reference);
                if (match) {
                    return true;
                }
            }
        } finally {
            inclusionReadLock.unlock();
        }

        Lock exclusionReadLock = testExclusionRWLock.readLock();
        exclusionReadLock.lock();

        try {
            for (Filter filter : testExclusionFilters) {
                boolean match = filter.match(reference);
                if (match) {
                    LOGGER.info("Not running test [" + reference.toString() + "] due to exclusion filter ["
                            + filter.toString() + "].");
                    return false;
                }
            }
        } finally {
            exclusionReadLock.unlock();
        }

        return true;
    }
}
