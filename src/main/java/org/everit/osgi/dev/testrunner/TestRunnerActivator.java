package org.everit.osgi.dev.testrunner;

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
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.everit.osgi.dev.testrunner.blocking.BlockingManager;
import org.everit.osgi.dev.testrunner.blocking.BlockingManagerImpl;
import org.everit.osgi.dev.testrunner.junit4.Junit4TestRunner;
import org.everit.osgi.dev.testrunner.util.DependencyUtil;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.launch.Framework;
import org.osgi.service.blueprint.container.BlueprintListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Activator of the bundle that activates the listeners for the different testing technologies.
 */
public class TestRunnerActivator implements BundleActivator {

    public static final Set<String> SYSTEM_NON_DAEMON_THREAD_NAMES;

    public static final String SYSTEM_EXIT_ERROR_FILE_NAME = "system-exit-error.txt";

    static {
        SYSTEM_NON_DAEMON_THREAD_NAMES = new HashSet<String>();
        SYSTEM_NON_DAEMON_THREAD_NAMES.add("DestroyJavaVM");
    }

    private class TestFinalizationWaitingShutdownThread extends Thread {

        private BundleContext context;

        private String resultFolder;

        public TestFinalizationWaitingShutdownThread(BundleContext context, String resultFolder) {
            super();
            this.context = context;
            this.resultFolder = resultFolder;
        }

        @Override
        public void run() {
            blockingManager.start();
            blockingManager.waitForTestResults();

            Framework framework = (Framework) context.getBundle(0);
            LOGGER.info("Tests had been ran, stopping framework");
            try {
                framework.stop();
                framework.waitForStop(0);
            } catch (BundleException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            List<Thread> blockingThreads = countBlockingThreads();
            boolean canBeStopped = blockingThreads.size() == 0;
            long threadBlockCheckStartTime = new Date().getTime();
            while (!canBeStopped) {
                try {
                    Thread.sleep(100);
                    blockingThreads = countBlockingThreads();
                    canBeStopped = blockingThreads.size() == 0;
                    if (!canBeStopped) {
                        long currentTime = new Date().getTime();
                        if (currentTime - threadBlockCheckStartTime > shutdownTimeout) {
                            canBeStopped = true;
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    canBeStopped = true;
                }
            }
            

            if (blockingThreads.size() > 0) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                pw.println("THERE ARE NON-DEAMON THREADS THAT BLOCK STOPPING THE OSGi CONTAINER\n");
                pw.println("Calling interrupt on blocking threads. " +
                        "If the JVM does not stop after this well there is a serious problem in the code.");

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
                        e.printStackTrace(pw);
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
                        e.printStackTrace();
                    }
                } catch (FileNotFoundException e1) {
                    e1.printStackTrace();
                } finally {
                    try {
                        fout.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            System.exit(0);
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
    }

    /**
     * The name of the System Property that points to the folder where XML test results should be dumped. This property
     * is ignored if the {@link TestRunnerActivator#ENV_TEXT_RESULT_FOLDER} is defined.
     */
    public static final String SYSPROP_XML_RESULT_FOLDER = "org.everit.osgi.testing.runner.xmlResultDumpFolder";

    /**
     * The name of the System Property that points to the folder where TEXT based test results should be dumped. This
     * property is ignored if the {@link TestRunnerActivator#ENV_TEXT_RESULT_FOLDER} is defined.
     */
    public static final String SYSPROP_TEXT_RESULT_FOLDER = "org.everit.osgi.testing.runner.textResultDumpFolder";

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
     * A service is created by this activator that listens for blueprint events. This is necessary in order to block the
     * test runner code until each bundle arrives to the state of CREATED or FAILED in case of Blueprint bundles.
     */
    private ServiceRegistration blueprintServiceRegistration;

    /**
     * The blocking manager instance that is registered as a framework listener, a blueprint listener and as a service.
     */
    private BlockingManagerImpl blockingManager;

    /**
     * Service registration of Blocking Manager that starts tests after the framework is started.
     */
    private ServiceRegistration blockingManagerServiceRegistration;

    /**
     * The timeout while the test runner will wait for blocking threads before starting to interrupt them.
     */
    private int shutdownTimeout = 5000;

    @Override
    public void start(final BundleContext context) throws Exception {

        String resultDumpFolder = System.getenv(ENV_TEST_RESULT_FOLDER);

        blockingManager = new BlockingManagerImpl(context);

        context.addFrameworkListener(blockingManager);

        blockingManagerServiceRegistration = context.registerService(BlockingManager.class.getName(), blockingManager,
                new Hashtable<String, String>());

        if (DependencyUtil.isBlueprintAvailable()) {
        blueprintServiceRegistration = context.registerService(BlueprintListener.class.getName(),
                blockingManager,
                new Hashtable<String, Object>());
        }

        Junit4TestRunner junit4TestRunner = new Junit4TestRunner(resultDumpFolder, resultDumpFolder, context);
        blockingManager.addTestRunner(junit4TestRunner);

        String stopAfterTests = System.getenv(BlockingManager.ENV_STOP_AFTER_TESTS);
        if (Boolean.parseBoolean(stopAfterTests)) {
            new TestFinalizationWaitingShutdownThread(context, resultDumpFolder).start();
        } else {
            new Thread(new Runnable() {
                
                @Override
                public void run() {
                    blockingManager.start();
                }
            }).start();
        }
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        if (blockingManager != null) {
            blockingManager.stop();
        }
        if (blueprintServiceRegistration != null) {
            blueprintServiceRegistration.unregister();
            blueprintServiceRegistration = null;
        }
        if (blockingManagerServiceRegistration != null) {
            blockingManagerServiceRegistration.unregister();
            blockingManagerServiceRegistration = null;
        }
    }
}
