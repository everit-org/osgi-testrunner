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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Logger;

import org.everit.osgi.dev.testrunner.TestRunnerConstants;
import org.everit.osgi.dev.testrunner.blocking.ShutdownBlocker;
import org.everit.osgi.dev.testrunner.internal.blocking.BlockingManagerImpl;
import org.everit.osgi.dev.testrunner.internal.blocking.FrameworkStartingShutdownBlockerImpl;
import org.everit.osgi.dev.testrunner.internal.blocking.TestClassShutdownBlockerImpl;
import org.everit.osgi.dev.testrunner.internal.util.ThreadUtil;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.launch.Framework;

/**
 * Activator of the bundle that activates the listeners for the different testing technologies.
 */
public class TestRunnerActivator implements BundleActivator {

  /**
   * In case the tests are running during a build the test runner stops the system after every
   * possible test were ran. This is the thread class that monitors the state of the test runs and
   * logs out notifications if the build has to wait too much.
   */
  private class TestFinalizationWaitingShutdownThread extends Thread {

    /**
     * The context of the testrunner bundle.
     */
    private final BundleContext context;

    /**
     * The folder where the {@link TestRunnerActivator#SYSTEM_EXIT_ERROR_FILE_NAME} file will be
     * written.
     */
    private final String resultFolder;

    TestFinalizationWaitingShutdownThread(final BundleContext context,
        final String resultFolder) {
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
      File resultFolderFile =
          new File(resultFolder, TestRunnerConstants.SYSTEM_EXIT_ERROR_FILE_NAME);
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
        e.printStackTrace(System.err);
      }
    }

    @Override
    public void run() {
      blockingManager.waitForNoBlockCause(0);
      ThreadUtil threadUtil = new ThreadUtil();

      stopFramework();

      List<Thread> blockingThreads = threadUtil.countDeamonThreads();
      boolean canBeStopped = blockingThreads.size() == 0;
      long threadBlockCheckStartTime = new Date().getTime();
      while (!canBeStopped) {
        try {
          final int stoppingWaitingPeriodInMs = 100;
          Thread.sleep(stoppingWaitingPeriodInMs);
          blockingThreads = threadUtil.countDeamonThreads();
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
        Runtime.getRuntime().halt(1);
      }
    }

    private void stopFramework() {
      Framework framework = (Framework) context.getBundle(0);
      LOGGER.info("Tests had been ran, stopping framework");
      try {
        framework.stop();
        PrintStream stdout = System.out;
        stdout.println("Starting to wait for stop framework");
        framework.waitForStop(0);
        stdout.println("framework stopped");
        stdout.flush();
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

  private static void executeIfNotNull(final Object obj, final Runnable runnable) {
    if (obj != null) {
      runnable.run();
    }
  }

  /**
   * The blocking manager instance that is registered as a framework listener, a blueprint listener
   * and as a service.
   */
  private BlockingManagerImpl blockingManager = null;

  private FrameworkStartingShutdownBlockerImpl frameworkStartBlocker;

  private ServiceRegistration<ShutdownBlocker> frameworkStartBlockerSR;

  private ServiceRegistration<ShutdownBlocker> runnableThreadBlockerSR;

  /**
   * The timeout while the test runner will wait for blocking threads before starting to interrupt
   * them.
   */
  private final int shutdownTimeout = TestRunnerConstants.DEFAULT_SHUTDOWN_TIMEOUT;

  private FrameworkListener startTestManagerOnFrameworkActive;

  private TestClassShutdownBlockerImpl testClassBlocker;

  private ServiceRegistration<ShutdownBlocker> testClassBlockerSR;

  private TestExtender testExtender;

  @Override
  public void start(final BundleContext context) throws Exception {
    String resultDumpFolder = context.getProperty(TestRunnerConstants.PROP_TEST_RESULT_FOLDER);

    final boolean shutdownAfterTests =
        Boolean.parseBoolean(context.getProperty(TestRunnerConstants.PROP_STOP_AFTER_TESTS));

    final boolean developmentMode =
        Boolean.parseBoolean(context.getProperty(TestRunnerConstants.PROP_DEVELOPMENT_MODE));

    if (shutdownAfterTests) {
      frameworkStartBlocker = new FrameworkStartingShutdownBlockerImpl(context);
      frameworkStartBlocker.start();
      frameworkStartBlockerSR =
          context.registerService(ShutdownBlocker.class, frameworkStartBlocker,
              new Hashtable<String, Object>());

      testClassBlocker = new TestClassShutdownBlockerImpl(context);
      testClassBlockerSR = context.registerService(ShutdownBlocker.class, testClassBlocker,
          new Hashtable<String, Object>());
      testClassBlocker.open();

      blockingManager = new BlockingManagerImpl(context);
      blockingManager.start();

      TestFinalizationWaitingShutdownThread shutdownThread =
          new TestFinalizationWaitingShutdownThread(context, resultDumpFolder);
      shutdownThread.setDaemon(false);
      shutdownThread.start();
    }

    testExtender = new TestExtender(context, blockingManager, developmentMode);
    testExtender.open();
  }

  @Override
  public void stop(final BundleContext context) throws Exception {

    executeIfNotNull(testExtender, () -> testExtender.close());

    executeIfNotNull(startTestManagerOnFrameworkActive,
        () -> context.removeFrameworkListener(startTestManagerOnFrameworkActive));

    executeIfNotNull(blockingManager, () -> blockingManager.stop());
    executeIfNotNull(runnableThreadBlockerSR, () -> runnableThreadBlockerSR.unregister());
    executeIfNotNull(testClassBlocker, () -> testClassBlocker.close());
    executeIfNotNull(testClassBlockerSR, () -> testClassBlockerSR.unregister());
    executeIfNotNull(frameworkStartBlocker, () -> frameworkStartBlocker.stop());
    executeIfNotNull(frameworkStartBlockerSR, () -> frameworkStartBlockerSR.unregister());
  }
}
