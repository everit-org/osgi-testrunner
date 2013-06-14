package org.everit.osgi.dev.testrunner.junit4;

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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;

import org.everit.osgi.dev.testrunner.GlobalResult;
import org.everit.osgi.dev.testrunner.TestResultContainer;
import org.everit.osgi.dev.testrunner.blocking.BlockedTestRunner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs all JUnit4 based tests that are provided as a service in this OSGI container.
 */
public class Junit4TestRunner implements BlockedTestRunner, ServiceTrackerCustomizer {

    /**
     * The logger of the class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Junit4TestRunner.class);

    /**
     * The bundle context of this bundle to be able to get the Junit test services.
     */
    private BundleContext bundleContext;

    /**
     * The service tracker that picks up junit4 test services. The customizer is the current object.
     */
    private ServiceTracker junit4ServiceTracker;

    /**
     * The folder where the test results should be appended in text format.
     */
    private String textResultDumpFolder;

    /**
     * The folder where the test results should be appended in XML format.
     */
    private String xmlResultDumpFolder;

    /**
     * Constructor.
     * 
     * @param textResultDumpFolder
     *            The folder where the test results should be appended in text format.
     * @param xmlResultDumpFolder
     *            The folder where the test results should be appended in XML format.
     * @param bundleContext
     *            The bundle context of this bundle to be able to get the Junit test services.
     */
    public Junit4TestRunner(final String textResultDumpFolder, final String xmlResultDumpFolder,
            final BundleContext bundleContext) {
        super();
        this.textResultDumpFolder = textResultDumpFolder;
        this.xmlResultDumpFolder = xmlResultDumpFolder;
        this.bundleContext = bundleContext;
    }

    @Override
    public Object addingService(final ServiceReference reference) {
        runTest(reference);
        return null;
    }

    @Override
    public void modifiedService(final ServiceReference reference, final Object service) {
        // Do nothing
    }

    @Override
    public void removedService(final ServiceReference reference, final Object service) {
        // Do nothing
    }

    private void runTest(final ServiceReference reference) {
        LOGGER.info("Test OSGI Service is caughed, will be run by JUnit: " + reference.toString());
        try {
            Object testIdObject = reference.getProperty("osgitest.id");
            String testId = (testIdObject == null) ? null : testIdObject.toString();
            Object service = bundleContext.getService(reference);
            String[] klassNames = (String[]) reference.getProperty(Constants.OBJECTCLASS);
            if (klassNames == null) {
                LOGGER.error("Cannot load interface names for Junit service");
                return;
            }
            for (String klassName : klassNames) {
                String testUniqueName = klassName;
                if (testId != null) {
                    testUniqueName += "_" + testId;
                }
                try {
                    Class<?> klass = reference.getBundle().loadClass(klassName);

                    BlockJUnit4ObjectRunner runner = new BlockJUnit4ObjectRunner(klass, service);
                    RunNotifier notifier = new RunNotifier();
                    ExtendedResultListener extendedResultListener = new ExtendedResultListener();
                    notifier.addListener(extendedResultListener);
                    runner.run(notifier);
                    ExtendedResult extendedResult = extendedResultListener.getResult();
                    extendedResult.finishRunning();
                    long failureCount = extendedResult.getFailureCount();
                    long errorCount = extendedResult.getErrorCount();
                    long runCount = extendedResult.getRunCount();
                    long ignoreCount = extendedResult.getIgnoreCount();
                    long runTime = extendedResult.getRunTime();

                    if (textResultDumpFolder != null) {
                        File folder = new File(textResultDumpFolder);
                        folder.mkdirs();
                        File textFile = new File(folder, testUniqueName + ".txt");
                        Writer fw = null;
                        try {
                            FileOutputStream fout = new FileOutputStream(textFile, false);
                            fw = new OutputStreamWriter(fout, "UTF8");
                            JunitResultUtil.dumpTextResult(klass, extendedResult, testId, fw);
                        } catch (IOException e) {
                            LOGGER.error("Exception during dumping test results", e);
                        } finally {
                            if (fw != null) {
                                try {
                                    fw.close();
                                } catch (IOException e) {
                                    LOGGER.error("Could not close File writer to dump test results", e);
                                }
                            }
                        }
                    }
                    if (xmlResultDumpFolder != null) {
                        File folder = new File(xmlResultDumpFolder);
                        folder.mkdirs();
                        File xmlFile = new File(folder, testUniqueName + ".xml");
                        JunitResultUtil.writeXmlResultToFile(klass, extendedResult, xmlFile, testId, false);
                    }

                    PrintWriter pw = new PrintWriter(System.out);
                    try {
                        JunitResultUtil.dumpTextResult(klass, extendedResult, testId, pw);
                    } catch (IOException e) {
                        LOGGER.error("Error during dumping the test results in TEXT format", e);
                    }
                    pw.flush();

                    GlobalResult globalPartResult = new GlobalResult();
                    globalPartResult.setErrorCount(errorCount);
                    globalPartResult.setFailureCount(failureCount);
                    globalPartResult.setIgnoreCount(ignoreCount);
                    globalPartResult.setRunCount(runCount);
                    globalPartResult.setRunTime(runTime);
                    TestResultContainer.addToGlobalResult(globalPartResult);
                } catch (InitializationError e) {
                    LOGGER.error("Could not initialize Junit runner", e);
                } catch (ClassNotFoundException e) {
                    LOGGER.error("Could not load the class of the test: " + testUniqueName, e);
                }
            }
        } finally {
            bundleContext.ungetService(reference);
        }

    }

    @Override
    public void start() {
        try {
            junit4ServiceTracker = new ServiceTracker(bundleContext, bundleContext.createFilter("(osgitest=junit4)"),
                    this);
            junit4ServiceTracker.open();
        } catch (InvalidSyntaxException e) {
            LOGGER.error("Error during creation JUnit4 TestServiceTracker", e);
        }
    }

    @Override
    public void stop() {
        if (junit4ServiceTracker != null) {
            junit4ServiceTracker.close();
        }
    }

}
