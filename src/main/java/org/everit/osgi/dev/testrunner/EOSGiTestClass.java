/*
 * Copyright (C) 2011 Everit Kft. (http://www.everit.org)
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
package org.everit.osgi.dev.testrunner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import aQute.bnd.annotation.headers.ProvideCapability;

/**
 * Add this annotation to a test class that is executed with test runner. By having this annotation
 * a {@value TestRunnerConstants#CAPABILITY_TESTCLASS_NAMESPACE} capability will be added the bundle
 * so the test runner will now that it should wait for this class to be executed.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@ProvideCapability(ns = TestRunnerConstants.CAPABILITY_TESTCLASS_NAMESPACE, name = "${@class}")
public @interface EOSGiTestClass {

  /**
   * The number how many times the class should be executed. It is possible that that several
   * instances of the class should be executed before the test runner shuts down the JVM.
   */
  int executionCount() default 1;
}
