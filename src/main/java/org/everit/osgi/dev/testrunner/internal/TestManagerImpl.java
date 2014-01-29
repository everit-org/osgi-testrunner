/**
 * This file is part of Everit Test Runner Bundle.
 *
 * Everit Test Runner Bundle is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Everit Test Runner Bundle is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Everit Test Runner Bundle.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.everit.osgi.dev.testrunner.internal;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.everit.osgi.dev.testrunner.TestRunnerConstants;
import org.everit.osgi.dev.testrunner.TestManager;
import org.everit.osgi.dev.testrunner.engine.TestClassResult;
import org.everit.osgi.dev.testrunner.engine.TestEngine;
import org.osgi.framework.ServiceReference;

public class TestManagerImpl implements TestManager {

    private static final Logger LOGGER = Logger.getLogger(TestManagerImpl.class.getName());

    private final TestRunnerEngineTracker testRunnerEngineTracker;

    private boolean inDevelopmentMode;

    public TestManagerImpl(final TestRunnerEngineTracker testRunnerEngineTracker) {
        this.testRunnerEngineTracker = testRunnerEngineTracker;
        inDevelopmentMode = !Boolean.parseBoolean(System.getenv(TestRunnerConstants.ENV_STOP_AFTER_TESTS));
    }

    @Override
    public boolean isInDevelopmentMode() {
        return inDevelopmentMode;
    }

    @Override
    public List<TestClassResult> runTest(final ServiceReference<Object> reference, final boolean force) {

        Object engineTypeObject = reference.getProperty(TestRunnerConstants.SERVICE_PROPERTY_TESTRUNNER_ENGINE_TYPE);
        if ((engineTypeObject == null) || !(engineTypeObject instanceof String)) {
            LOGGER.log(Level.WARNING, "Unrecognized '" + TestRunnerConstants.SERVICE_PROPERTY_TESTRUNNER_ENGINE_TYPE
                    + "' service property value for test. Are you sure the test engine is available? Ignoring: "
                    + reference.toString());
            return null;
        }

        TestEngine runnerEngine = testRunnerEngineTracker.getEngineByType((String) engineTypeObject);
        if (runnerEngine == null) {
            LOGGER.log(Level.WARNING, "No test runner available for type '" + engineTypeObject + "'. Ignoring test: "
                    + reference.toString());
            return null;
        }

        List<TestClassResult> result = runnerEngine.runTest(reference, force || inDevelopmentMode);
        LOGGER.log(Level.FINER, "Test result: " + result.toString());
        return result;

    }

    @Override
    public void setInDevelopmentMode(final boolean inDevelopmentMode) {
        this.inDevelopmentMode = inDevelopmentMode;
    }
}
