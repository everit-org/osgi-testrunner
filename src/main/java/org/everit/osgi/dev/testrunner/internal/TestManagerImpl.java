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

import java.awt.GraphicsEnvironment;
import java.util.List;

import org.everit.osgi.dev.testrunner.Constants;
import org.everit.osgi.dev.testrunner.TestManager;
import org.everit.osgi.dev.testrunner.engine.TestClassResult;
import org.everit.osgi.dev.testrunner.engine.TestEngine;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestManagerImpl implements TestManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestManagerImpl.class);

    private final TestRunnerEngineTracker testRunnerEngineTracker;
    
    private boolean inDevelopmentMode;

    public TestManagerImpl(final TestRunnerEngineTracker testRunnerEngineTracker) {
        this.testRunnerEngineTracker = testRunnerEngineTracker;
        inDevelopmentMode = !Boolean.parseBoolean(System.getenv(Constants.ENV_STOP_AFTER_TESTS));
    }

    @Override
    public List<TestClassResult> runTest(final ServiceReference<Object> reference, boolean force) {

        Object engineTypeObject = reference.getProperty(Constants.SERVICE_PROPERTY_TESTRUNNER_ENGINE_TYPE);
        if ((engineTypeObject == null) || !(engineTypeObject instanceof String)) {
            LOGGER.warn("Unrecognized '" + Constants.SERVICE_PROPERTY_TESTRUNNER_ENGINE_TYPE
                    + "' service property value for test. Are you sure the test engine is available? Ignoring: "
                    + reference.toString());
            return null;
        }

        
        TestEngine runnerEngine = testRunnerEngineTracker.getEngineByType((String) engineTypeObject);
        if (runnerEngine == null) {
            LOGGER.warn("No test runner available for type '" + engineTypeObject + "'. Ignoring test: "
                    + reference.toString());
            return null;
        }

        List<TestClassResult> result = runnerEngine.runTest(reference, force || inDevelopmentMode);
        LOGGER.debug("Test result: " + result.toString());
        return result;

    }
    
    @Override
    public void setInDevelopmentMode(boolean inDevelopmentMode) {
        this.inDevelopmentMode = inDevelopmentMode;
    }
    
    @Override
    public boolean isInDevelopmentMode() {
        return inDevelopmentMode;
    }
}
