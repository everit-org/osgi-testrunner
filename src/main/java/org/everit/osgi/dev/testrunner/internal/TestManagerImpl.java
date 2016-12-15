/*
 * Copyright (C) 2011 Everit Kft. (http://www.everit.biz)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.everit.osgi.dev.testrunner.internal;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.everit.osgi.dev.testrunner.TestManager;
import org.everit.osgi.dev.testrunner.TestRunnerConstants;
import org.everit.osgi.dev.testrunner.engine.TestClassResult;
import org.everit.osgi.dev.testrunner.engine.TestEngine;
import org.osgi.framework.ServiceReference;

/**
 * The test manager implementation.
 */
public class TestManagerImpl implements TestManager {

  private static final Logger LOGGER = Logger.getLogger(TestManagerImpl.class.getName());

  private boolean inDevelopmentMode;

  private final TestRunnerEngineTracker testRunnerEngineTracker;

  /**
   * Constructor.
   *
   * @param testRunnerEngineTracker
   *          The tracker of the test runner engines.
   */
  public TestManagerImpl(final TestRunnerEngineTracker testRunnerEngineTracker) {
    this.testRunnerEngineTracker = testRunnerEngineTracker;
    inDevelopmentMode =
        !Boolean.parseBoolean(System.getenv(TestRunnerConstants.ENV_STOP_AFTER_TESTS));
  }

  @Override
  public boolean isInDevelopmentMode() {
    return inDevelopmentMode;
  }

  @Override
  public List<TestClassResult> runTest(final ServiceReference<Object> reference,
      final boolean force) {

    Object engineTypeObject =
        reference.getProperty(TestRunnerConstants.SERVICE_PROPERTY_TESTRUNNER_ENGINE_TYPE);
    if ((engineTypeObject == null) || !(engineTypeObject instanceof String)) {
      LOGGER.log(Level.WARNING,
          "Unrecognized '" + TestRunnerConstants.SERVICE_PROPERTY_TESTRUNNER_ENGINE_TYPE
              + "' service property value for test. Are you sure the test engine is available?"
              + " Ignoring: " + reference.toString());
      return null;
    }

    TestEngine runnerEngine = testRunnerEngineTracker.getEngineByType((String) engineTypeObject);
    if (runnerEngine == null) {
      LOGGER.log(Level.WARNING,
          "No test runner available for type '" + engineTypeObject + "'. Ignoring test: "
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
