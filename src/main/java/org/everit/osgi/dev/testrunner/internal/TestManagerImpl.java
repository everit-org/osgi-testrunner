package org.everit.osgi.dev.testrunner.internal;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.everit.osgi.dev.testrunner.TestManager;
import org.osgi.framework.Filter;

public class TestManagerImpl implements TestManager {

    private Set<Filter> startupTestInclusionFilters = new HashSet<Filter>();

    private ReadWriteLock startupTestInclusionRWLock = new ReentrantReadWriteLock(false);

    private Set<Filter> startupTestExclusionFilters = new HashSet<Filter>();

    private ReadWriteLock startupTestExclusionRWLock = new ReentrantReadWriteLock(false);

    private Set<Filter> deployedTestInclusionFilters = new HashSet<Filter>();

    private ReadWriteLock deployedTestInclusionRWLock = new ReentrantReadWriteLock(false);

    private Set<Filter> deployedTestExclusionFilters = new HashSet<Filter>();

    private ReadWriteLock deployedTestExclusionRWLock = new ReentrantReadWriteLock(false);

    @Override
    public boolean addStartupTestInclusionFilter(Filter filter) {
        Lock writeLock = startupTestInclusionRWLock.writeLock();
        writeLock.lock();
        try {
            return startupTestInclusionFilters.add(filter);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean addStartupTestExclusionFilter(Filter filter) {
        Lock writeLock = startupTestExclusionRWLock.writeLock();
        writeLock.lock();
        try {
            return startupTestExclusionFilters.add(filter);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean removeStartupTestInclusion(Filter filter) {
        Lock writeLock = startupTestInclusionRWLock.writeLock();
        writeLock.lock();
        try {
            return startupTestInclusionFilters.remove(filter);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean removeStartupTestExclusionFilter(Filter filter) {
        Lock writeLock = startupTestExclusionRWLock.writeLock();
        writeLock.lock();
        try {
            return startupTestExclusionFilters.remove(filter);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Set<Filter> getStartupTestExclusionFilters() {
        Lock readLock = startupTestExclusionRWLock.readLock();
        readLock.lock();
        try {
            return new HashSet<Filter>(startupTestExclusionFilters);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Set<Filter> getStartupTestInclusionFilters() {
        Lock readLock = startupTestInclusionRWLock.readLock();
        readLock.lock();
        try {
            return new HashSet<Filter>(startupTestInclusionFilters);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean addDeployedTestInclusionFilter(Filter filter) {
        Lock writeLock = deployedTestInclusionRWLock.writeLock();
        writeLock.lock();
        try {
            return deployedTestInclusionFilters.add(filter);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean addDeployedTestExclusionFilter(Filter filter) {
        Lock writeLock = deployedTestExclusionRWLock.writeLock();
        writeLock.lock();
        try {
            return deployedTestExclusionFilters.add(filter);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean removeDeployedTestInclusionFilter(Filter filter) {
        Lock writeLock = deployedTestInclusionRWLock.writeLock();
        writeLock.lock();
        try {
            return deployedTestInclusionFilters.remove(filter);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean removeDeployedTestExclusionFilter(Filter filter) {
        Lock writeLock = deployedTestExclusionRWLock.writeLock();
        writeLock.lock();
        try {
            return deployedTestExclusionFilters.remove(filter);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Set<Filter> getDeployedTestExclusionFilters() {
        Lock readLock = deployedTestExclusionRWLock.readLock();
        readLock.lock();
        try {
            return new HashSet<Filter>(deployedTestExclusionFilters);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Set<Filter> getDeployedTestInclusionFilters() {
        Lock readLock = deployedTestInclusionRWLock.readLock();
        readLock.lock();
        try {
            return new HashSet<Filter>(deployedTestInclusionFilters);
        } finally {
            readLock.unlock();
        }
    }
}
