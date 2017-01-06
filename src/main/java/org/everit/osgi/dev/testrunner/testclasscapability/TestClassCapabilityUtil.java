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
package org.everit.osgi.dev.testrunner.testclasscapability;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.everit.osgi.dev.testrunner.TestRunnerConstants;

/**
 * Util methods to parse TestCase capabilities from bundle headers.
 */
public final class TestClassCapabilityUtil {

  private static TestClassCapabilityDTO processTestCaseCapabilityClause(final Clause clause) {
    String clazz = clause.getAttribute(TestRunnerConstants.CAPABILITY_TESTCLASS_NAMESPACE);
    if (clazz == null) {
      throw new TestClassCapabilitySyntaxException("Missing class attribute in "
          + TestRunnerConstants.CAPABILITY_TESTCLASS_NAMESPACE + " capability: " + clause);
    }
    String countAttr =
        clause.getAttribute(TestRunnerConstants.CAPABILITY_TESTCLASS_ATTR_EXECUTION_COUNT);

    if (countAttr == null) {
      countAttr = clause
          .getAttribute(TestRunnerConstants.CAPABILITY_TESTCLASS_ATTR_EXECUTION_COUNT + ":Long");
    }

    int count = 1;
    if (countAttr != null) {
      try {
        count = Integer.parseInt(countAttr);
        if (count < 0) {
          throwCountMustBeNonNegativeException(clause);
        }
      } catch (NumberFormatException e) {
        throwCountMustBeNonNegativeException(clause);
      }
    }

    TestClassCapabilityDTO testCaseCapability = new TestClassCapabilityDTO();
    testCaseCapability.clazz = clazz;
    testCaseCapability.count = count;
    return testCaseCapability;
  }

  /**
   * Resolves the {@link TestRunnerConstants#CAPABILITY_TESTCLASS_NAMESPACE} capabilities of a
   * specific Provide-Capability MANIFEST header.
   *
   * @param provideCapabilityHeader
   *          The header as a string.
   * @return The resolved eosgi.testClass capabilities.
   * @throws TestClassCapabilitySyntaxException
   *           if {@link TestRunnerConstants#CAPABILITY_TESTCLASS_NAMESPACE} attribute is missing or
   *           {@link TestRunnerConstants#CAPABILITY_TESTCLASS_ATTR_EXECUTION_COUNT} attribute is
   *           not a number.
   */
  public static Collection<TestClassCapabilityDTO> resolveTestCaseCapabilities(
      final String provideCapabilityHeader) {

    if (provideCapabilityHeader == null) {
      return Collections.emptySet();
    }
    Clause[] clauses = Parser.parseHeader(provideCapabilityHeader);

    List<TestClassCapabilityDTO> testCaseCapabilities = new ArrayList<>();

    for (Clause clause : clauses) {
      if (TestRunnerConstants.CAPABILITY_TESTCLASS_NAMESPACE.equals(clause.getName())) {
        TestClassCapabilityDTO testCaseCapability = processTestCaseCapabilityClause(clause);
        testCaseCapabilities.add(testCaseCapability);
      }
    }
    return testCaseCapabilities;
  }

  private static void throwCountMustBeNonNegativeException(final Clause clause) {
    throw new TestClassCapabilitySyntaxException(
        "Count attribute must be a non-negative number in testCase clause: "
            + clause.toString());
  }

  private TestClassCapabilityUtil() {
  }
}
