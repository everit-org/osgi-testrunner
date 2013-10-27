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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.everit.osgi.dev.testrunner.Constants;
import org.everit.osgi.dev.testrunner.TestManager;
import org.everit.osgi.dev.testrunner.engine.TestClassResult;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestServiceTracker extends ServiceTracker<Object, Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestServiceTracker.class);

    private static final File TEST_RESULT_FOLDER_FILE;

    static {
        String testResultFolder = System.getenv(Constants.ENV_TEST_RESULT_FOLDER);
        if (testResultFolder != null) {
            TEST_RESULT_FOLDER_FILE = new File(testResultFolder);
        } else {
            TEST_RESULT_FOLDER_FILE = null;
        }
    }

    public static TestServiceTracker createTestServiceTracker(BundleContext bundleContext, TestManager testManager) {
        try {
            Filter filter = bundleContext.createFilter("(" + Constants.SERVICE_PROPERTY_TEST_ID + "=*)");
            return new TestServiceTracker(bundleContext, testManager, filter);

        } catch (InvalidSyntaxException e) {
            throw new RuntimeException("An exception is thrown that should never happen", e);
        }
    }

    private final TestManager testManager;

    private TestServiceTracker(BundleContext bundleContext, TestManager testManager, Filter filter) {
        super(bundleContext, filter, null);
        this.testManager = testManager;
    }

    @Override
    public Object addingService(ServiceReference<Object> reference) {
        List<TestClassResult> testClassResults = testManager.runTest(reference, false);
        dumpTestResults(reference, testClassResults);
        return null;
    }

    private void dumpTestResults(ServiceReference<Object> reference, List<TestClassResult> testClassResults) {
        String testId = ResultUtil.getTestIdFromReference(reference);
        for (TestClassResult testClassResult : testClassResults) {
            if (TEST_RESULT_FOLDER_FILE != null) {
                String fileName =
                        ResultUtil.generateFileNameWithoutExtension(testClassResult.getClassName(), testId, true);

                File textFile = new File(TEST_RESULT_FOLDER_FILE, fileName + ".txt");
                try {
                    ResultUtil.writeTextResultToFile(testClassResult, testId, textFile, true);
                } catch (IOException e) {
                    LOGGER.error(
                            "Error during text test result " + testClassResult.toString() + " to file "
                                    + textFile.getAbsolutePath(), e);
                }

                File xmlFile = new File(TEST_RESULT_FOLDER_FILE, fileName + ".xml");

                ResultUtil.writeXmlResultToFile(testClassResult, xmlFile, testId, true);
            }
            try {
                ResultUtil.dumpTextResult(testClassResult, testId, new PrintWriter(System.out));
            } catch (IOException e) {
                LOGGER.error("Error dumping text result to standard output", e);
            }

        }
    }

    @Override
    public void modifiedService(ServiceReference<Object> reference, Object service) {
    }

    @Override
    public void removedService(ServiceReference<Object> reference, Object service) {
    }

}