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
import java.io.StringWriter;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.everit.osgi.dev.testrunner.Constants;
import org.everit.osgi.dev.testrunner.TestManager;
import org.everit.osgi.dev.testrunner.blocking.AbstractShutdownBlocker;
import org.everit.osgi.dev.testrunner.blocking.ShutdownBlocker;
import org.everit.osgi.dev.testrunner.engine.TestClassResult;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

public class TestServiceTracker extends ServiceTracker<Object, Object> {

    private static class ActiveTestShutdownBlocker extends AbstractShutdownBlocker {

        private AtomicReference<ServiceReference<?>> activeTestReference = new AtomicReference<ServiceReference<?>>();

        public void dropActiveTestReference() {
            super.notifyListenersAboutUnblock();
            activeTestReference.set(null);
        }

        @Override
        public void logBlockCauses(final StringBuilder sb) {
            ServiceReference<?> testReference = activeTestReference.get();
            if (testReference != null) {
                sb.append("  A test is running based on the reference: ").append(testReference).append("\n");
            }
        }

        public void setActiveTestReference(final ServiceReference<?> testReference) {
            activeTestReference.set(testReference);
            super.notifyListenersAboutBlock();
        }
    }

    private static final Logger LOGGER = Logger.getLogger(TestServiceTracker.class.getName());

    private static final File TEST_RESULT_FOLDER_FILE;

    static {
        String testResultFolder = System.getenv(Constants.ENV_TEST_RESULT_FOLDER);
        if (testResultFolder != null) {
            TEST_RESULT_FOLDER_FILE = new File(testResultFolder);
        } else {
            TEST_RESULT_FOLDER_FILE = null;
        }
    }

    public static TestServiceTracker createTestServiceTracker(final BundleContext bundleContext,
            final TestManager testManager) {
        try {
            Filter filter = bundleContext.createFilter("(" + Constants.SERVICE_PROPERTY_TEST_ID + "=*)");
            return new TestServiceTracker(bundleContext, testManager, filter);

        } catch (InvalidSyntaxException e) {
            throw new RuntimeException("An exception is thrown that should never happen", e);
        }
    }

    private final TestManager testManager;

    private final ActiveTestShutdownBlocker activeTestShutdownBlocker;

    private ServiceRegistration<ShutdownBlocker> activeTestShutdownBlockerSR;

    private TestServiceTracker(final BundleContext bundleContext, final TestManager testManager, final Filter filter) {
        super(bundleContext, filter, null);
        this.testManager = testManager;
        activeTestShutdownBlocker = new ActiveTestShutdownBlocker();
    }

    @Override
    public Object addingService(final ServiceReference<Object> reference) {

        List<TestClassResult> testClassResults = testManager.runTest(reference, false);
        if (testClassResults != null) {
            dumpTestResults(reference, testClassResults);
        } else {
            LOGGER.info("Tests for reference has no result. The cause should be in the log before this entry: "
                    + reference.toString());
        }
        return null;
    }

    @Override
    public void close() {
        super.close();
        if (activeTestShutdownBlockerSR != null) {
            activeTestShutdownBlockerSR.unregister();
        }
    }

    private void dumpTestResults(final ServiceReference<Object> reference, final List<TestClassResult> testClassResults) {
        String testId = ResultUtil.getTestIdFromReference(reference);
        for (TestClassResult testClassResult : testClassResults) {
            if (TEST_RESULT_FOLDER_FILE != null) {
                String fileName =
                        ResultUtil.generateFileNameWithoutExtension(testClassResult.getClassName(), testId, true);

                File textFile = new File(TEST_RESULT_FOLDER_FILE, fileName + ".txt");
                try {
                    ResultUtil.writeTextResultToFile(testClassResult, testId, textFile, true);
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error during text test result " + testClassResult.toString()
                            + " to file " + textFile.getAbsolutePath(), e);
                }

                File xmlFile = new File(TEST_RESULT_FOLDER_FILE, fileName + ".xml");

                ResultUtil.writeXmlResultToFile(testClassResult, xmlFile, testId, true);
            }
            try {
                StringWriter sw = new StringWriter();
                sw.write("\n");
                ResultUtil.dumpTextResult(testClassResult, testId, sw);
                LOGGER.info(sw.toString());
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error dumping text result to standard output", e);
            }

        }
    }

    @Override
    public void modifiedService(final ServiceReference<Object> reference, final Object service) {
    }

    @Override
    public void open() {
        activeTestShutdownBlockerSR =
                context.registerService(ShutdownBlocker.class, activeTestShutdownBlocker,
                        new Hashtable<String, Object>());
        super.open();
    }

    @Override
    public void removedService(final ServiceReference<Object> reference, final Object service) {
    }

}
