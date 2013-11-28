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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Logger;

import org.everit.osgi.dev.testrunner.Constants;
import org.everit.osgi.dev.testrunner.TestManager;
import org.everit.osgi.dev.testrunner.blocking.ShutdownBlocker;
import org.everit.osgi.dev.testrunner.internal.blocking.BlockingManagerImpl;
import org.everit.osgi.dev.testrunner.internal.blocking.FrameworkStartingShutdownBlockerImpl;
import org.everit.osgi.dev.testrunner.internal.blocking.RunnableThreadShutdownBlockerImpl;
import org.everit.osgi.dev.testrunner.internal.util.ThreadUtil;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.launch.Framework;

/**
 * Activator of the bundle that activates the listeners for the different testing technologies.
 */
public class TestRunnerActivator implements BundleActivator {

    /**
     * In case the tests are running during a build the test runner stops the system after every possible test were ran.
     * This is the thread class that monitors the state of the test runs and logs out notifications if the build has to
     * wait too much.
     */
    private class TestFinalizationWaitingShutdownThread extends Thread {

        /**
         * The context of the testrunner bundle.
         */
        private BundleContext context;

        /**
         * The folder where the {@link TestRunnerActivator#SYSTEM_EXIT_ERROR_FILE_NAME} file will be written.
         */
        private String resultFolder;

        public TestFinalizationWaitingShutdownThread(final BundleContext context, final String resultFolder) {
            super();
            this.context = context;
            this.resultFolder = resultFolder;
        }

        private void logShutdownBlockingThreadsError(final List<Thread> blockingThreads) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println("THERE ARE NON-DEAMON THREADS THAT BLOCK STOPPING THE OSGi CONTAINER\n");
            pw.println("Calling interrupt on blocking threads. "
                    + "If the JVM does not stop after this well there is a serious problem in the code.");

            for (Thread thread : blockingThreads) {
                pw.println("[WARN] Thread [name=" + thread.getName() + ", id=" + thread.getId() + ", state="
                        + thread.getState().name() + "]");
                StackTraceElement[] stackTrace = thread.getStackTrace();
                for (StackTraceElement stackTraceElement : stackTrace) {
                    pw.println("\t" + stackTraceElement);
                }
                try {
                    thread.interrupt();
                } catch (SecurityException e) {
                    pw.println("Error during interrupting the thread");
                    logStackTrace(e, pw);
                }
            }
            System.err.print(sw.toString());
            File resultFolderFile = new File(resultFolder, Constants.SYSTEM_EXIT_ERROR_FILE_NAME);
            FileOutputStream fout = null;
            try {
                fout = new FileOutputStream(resultFolderFile);
                try {
                    fout.write(sw.toString().getBytes(Charset.forName("UTF8")));
                } catch (IOException e) {
                    logStackTrace(e);
                }
            } catch (FileNotFoundException e1) {
                logStackTrace(e1);
            } finally {
                try {
                    if (fout != null) {
                        fout.close();
                    }
                } catch (IOException e) {
                    logStackTrace(e);
                }
            }
        }

        private void logStackTrace(final Exception e) {
            logStackTrace(e, null);
        }

        private void logStackTrace(final Exception e, final PrintWriter pw) {
            if (pw != null) {
                e.printStackTrace(pw);
            } else {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            blockingManager.waitForNoBlockCause(0);

            stopFramework();

            List<Thread> blockingThreads = ThreadUtil.countDeamonThreads();
            boolean canBeStopped = blockingThreads.size() == 0;
            long threadBlockCheckStartTime = new Date().getTime();
            while (!canBeStopped) {
                try {
                    final int stoppingWaitingPeriodInMs = 100;
                    Thread.sleep(stoppingWaitingPeriodInMs);
                    blockingThreads = ThreadUtil.countDeamonThreads();
                    canBeStopped = blockingThreads.size() == 0;
                    if (!canBeStopped) {
                        long currentTime = new Date().getTime();
                        if ((currentTime - threadBlockCheckStartTime) > shutdownTimeout) {
                            canBeStopped = true;
                        }
                    }
                } catch (InterruptedException e) {
                    logStackTrace(e);
                    canBeStopped = true;
                }
            }

            if (blockingThreads.size() > 0) {
                logShutdownBlockingThreadsError(blockingThreads);
            }
            System.exit(0);
        }

        private void stopFramework() {
            Framework framework = (Framework) context.getBundle(0);
            LOGGER.info("Tests had been ran, stopping framework");
            try {
                framework.stop();
                System.out.println("Starting to wait for stop framework");
                framework.waitForStop(0);
                System.out.println("framework stopped");
                System.out.flush();
            } catch (BundleException e) {
                logStackTrace(e);
            } catch (InterruptedException e) {
                logStackTrace(e);
            }
        }
    }

    /**
     * Logger of class.
     */
    private static final Logger LOGGER = Logger.getLogger(TestRunnerActivator.class.getName());

    /**
     * The blocking manager instance that is registered as a framework listener, a blueprint listener and as a service.
     */
    private BlockingManagerImpl blockingManager;

    private FrameworkStartingShutdownBlockerImpl frameworkStartBlocker;

    private ServiceRegistration<ShutdownBlocker> frameworkStartBlockerSR;

    /**
     * The timeout while the test runner will wait for blocking threads before starting to interrupt them.
     */
    private int shutdownTimeout = Constants.DEFAULT_SHUTDOWN_TIMEOUT;

    private ServiceRegistration<TestManager> testManagerSR;

    private TestRunnerEngineServiceTracker testRunnerEngineServiceTracker;

    private TestServiceTracker testServiceTracker;

    private RunnableThreadShutdownBlockerImpl runnableThreadBlocker;

    private ServiceRegistration<ShutdownBlocker> runnableThreadBlockerSR;

    private Thread waitingTestsToRunThread;

    @Override
    public void start(final BundleContext context) throws Exception {

        String resultDumpFolder = System.getenv(Constants.ENV_TEST_RESULT_FOLDER);

        frameworkStartBlocker = new FrameworkStartingShutdownBlockerImpl(context);
        frameworkStartBlocker.start();
        frameworkStartBlockerSR =
                context.registerService(ShutdownBlocker.class, frameworkStartBlocker, new Hashtable<String, Object>());

        runnableThreadBlocker = new RunnableThreadShutdownBlockerImpl();
        runnableThreadBlocker.start();
        runnableThreadBlockerSR =
                context.registerService(ShutdownBlocker.class, runnableThreadBlocker, new Hashtable<String, Object>());

        blockingManager = new BlockingManagerImpl(context);
        blockingManager.start();

        testRunnerEngineServiceTracker = new TestRunnerEngineServiceTracker(context);
        testRunnerEngineServiceTracker.open();

        final TestManagerImpl testManager = new TestManagerImpl(testRunnerEngineServiceTracker);
        testManagerSR = context.registerService(TestManager.class, testManager, new Hashtable<String, Object>());

        waitingTestsToRunThread = new Thread(new Runnable() {

            @Override
            public void run() {
                testServiceTracker = TestServiceTracker.createTestServiceTracker(context, testManager);
                testServiceTracker.open();
            }
        });
        waitingTestsToRunThread.start();

        String stopAfterTests = System.getenv(Constants.ENV_STOP_AFTER_TESTS);
        if (Boolean.parseBoolean(stopAfterTests)) {
            new TestFinalizationWaitingShutdownThread(context, resultDumpFolder).start();
        }
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        if (testServiceTracker != null) {
            testServiceTracker.close();
        }
        if (testManagerSR != null) {
            testManagerSR.unregister();
        }
        if (testRunnerEngineServiceTracker != null) {
            testRunnerEngineServiceTracker.close();
        }
        if (blockingManager != null) {
            blockingManager.stop();
        }

        if (runnableThreadBlocker != null) {
            runnableThreadBlocker.stop();
        }

        if (runnableThreadBlockerSR != null) {
            runnableThreadBlockerSR.unregister();
        }

        if (frameworkStartBlocker != null) {
            frameworkStartBlocker.stop();
        }

        if (frameworkStartBlockerSR != null) {
            frameworkStartBlockerSR.unregister();
        }
        waitingTestsToRunThread.interrupt();
    }
}
