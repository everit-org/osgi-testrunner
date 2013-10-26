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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.everit.osgi.dev.testrunner.internal.blocking.BlockingManager;
import org.everit.osgi.dev.testrunner.internal.blocking.BlockingManagerImpl;
import org.everit.osgi.dev.testrunner.internal.junit4.Junit4TestRunner;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        private List<Thread> countBlockingThreads() {
            List<Thread> result = new ArrayList<Thread>();
            Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
            for (Entry<Thread, StackTraceElement[]> threadAndStackTrace : allStackTraces.entrySet()) {
                Thread thread = threadAndStackTrace.getKey();
                if (!thread.isDaemon() && !Thread.State.TERMINATED.equals(thread.getState())
                        && !thread.equals(Thread.currentThread())
                        && !SYSTEM_NON_DAEMON_THREAD_NAMES.contains(thread.getName())) {
                    result.add(thread);
                }
            }
            return result;
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
            blockingManager.start(context);
            blockingManager.waitForTestResultsAfterStartup();

            stopFramework();

            List<Thread> blockingThreads = countBlockingThreads();
            boolean canBeStopped = blockingThreads.size() == 0;
            long threadBlockCheckStartTime = new Date().getTime();
            while (!canBeStopped) {
                try {
                    final int stoppingWaitingPeriodInMs = 100;
                    Thread.sleep(stoppingWaitingPeriodInMs);
                    blockingThreads = countBlockingThreads();
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

        private void logShutdownBlockingThreadsError(final List<Thread> blockingThreads) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println("THERE ARE NON-DEAMON THREADS THAT BLOCK STOPPING THE OSGi CONTAINER\n");
            pw.println("Calling interrupt on blocking threads. "
                    + "If the JVM does not stop after this well there is a serious problem in the code.");

            for (Thread thread : blockingThreads) {
                pw.println("[WARN] Thread [name="
                        + thread.getName()
                        + ", id=" + thread.getId() + ", state=" + thread.getState().name() + "]");
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
            System.out.print(sw.toString());
            File resultFolderFile = new File(resultFolder, SYSTEM_EXIT_ERROR_FILE_NAME);
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

        private void stopFramework() {
            Framework framework = (Framework) context.getBundle(0);
            LOGGER.info("Tests had been ran, stopping framework");
            try {
                framework.stop();
                framework.waitForStop(0);
            } catch (BundleException e) {
                logStackTrace(e);
            } catch (InterruptedException e) {
                logStackTrace(e);
            }
        }
    }

    /**
     * The time in ms until the testrunner will wait for non-deamon threads stopping before exiting the vm.
     */
    public static final int DEFAULT_SHUTDOWN_TIMEOUT = 5000;

    /**
     * The name of the Environment Variable that points to the folder where TEXT and XML based test results should be
     * dumped. If this variable is specified the System properties of dump folders are ignored.
     */
    public static final String ENV_TEST_RESULT_FOLDER = "EOSGI_TEST_RESULT_FOLDER";

    /**
     * Logger of class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TestRunnerActivator.class);

    /**
     * The name of the file that is written if there is an error during system exit.
     */
    public static final String SYSTEM_EXIT_ERROR_FILE_NAME = "system-exit-error.txt";

    /**
     * The name of non-daemon threads that are started by the system. These threads do not have to be interrupted before
     * a system exit.
     */
    public static final Set<String> SYSTEM_NON_DAEMON_THREAD_NAMES;

    static {
        SYSTEM_NON_DAEMON_THREAD_NAMES = new HashSet<String>();
        SYSTEM_NON_DAEMON_THREAD_NAMES.add("DestroyJavaVM");
    }

    /**
     * The blocking manager instance that is registered as a framework listener, a blueprint listener and as a service.
     */
    private BlockingManagerImpl blockingManager;


    /**
     * The timeout while the test runner will wait for blocking threads before starting to interrupt them.
     */
    private int shutdownTimeout = DEFAULT_SHUTDOWN_TIMEOUT;

    @Override
    public void start(final BundleContext context) throws Exception {

        String resultDumpFolder = System.getenv(ENV_TEST_RESULT_FOLDER);

        blockingManager = new BlockingManagerImpl(context);

        Junit4TestRunner junit4TestRunner = new Junit4TestRunner(resultDumpFolder, resultDumpFolder, context);
        blockingManager.addTestRunner(junit4TestRunner);

        String stopAfterTests = System.getenv(BlockingManager.ENV_STOP_AFTER_TESTS);
        if (Boolean.parseBoolean(stopAfterTests)) {
            new TestFinalizationWaitingShutdownThread(context, resultDumpFolder).start();
        } else {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    blockingManager.start(context);
                }
            }).start();
        }
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        if (blockingManager != null) {
            blockingManager.stop();
        }
    }
}
