package org.everit.osgi.dev.testrunner.internal.blocking;

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

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.everit.osgi.dev.testrunner.blocking.AbstractShutdownBlocker;
import org.everit.osgi.dev.testrunner.internal.util.ThreadUtil;

public class RunnableThreadShutdownBlockerImpl extends AbstractShutdownBlocker {

    private class ActiveThreadChecker extends Thread {

        public static final long MIN_WAIT_FOR_CHECK_NANOS = 1000;

        public static final long MAX_WAIT_FOR_CHECK_NANOS = 2000000;

        private Random r = new Random();

        @Override
        public void run() {
            long range = MAX_WAIT_FOR_CHECK_NANOS - MIN_WAIT_FOR_CHECK_NANOS;

            ThreadGroup rootThreadGroup = ThreadUtil.getRootThreadGroup(this);

            try {

                while (!stopped.get()) {
                    if (ThreadUtil.getRunnableThreads(rootThreadGroup).length > 1) {
                        if (!blocking.get()) {
                            notifyListenersAboutBlock();
                        }
                    } else {
                        if (blocking.get()) {
                            notifyListenersAboutUnblock();
                        }
                    }

                    long randomLong = r.nextLong();
                    randomLong = Math.abs(randomLong);
                    long sleepingRange = randomLong % range;
                    long sleepingTime = sleepingRange + MIN_WAIT_FOR_CHECK_NANOS;
                    long sleepingMillis = sleepingTime / 1000;
                    int sleepingNanos = (int) sleepingTime % 1000;
                    Thread.sleep(sleepingMillis, sleepingNanos);
                }
            } catch (InterruptedException e) {
                notifyListenersAboutUnblock();
                System.err.println("WARN: Active thread checker of testrunner interrupted");
                e.printStackTrace(System.err);
            }
        }
    }

    private AtomicBoolean blocking = new AtomicBoolean(false);

    private AtomicBoolean stopped = new AtomicBoolean(true);

    public boolean isBlocking() {
        return blocking.get();
    }

    @Override
    public void logBlockCauses(final StringBuilder sb) {
        Thread[] runnableThreads = ThreadUtil.getRunnableThreads(ThreadUtil.getRootThreadGroup(Thread.currentThread()));

        sb.append("  The following threads are in runnable state:\n");
        System.err.println("Blockers:");
        for (Thread thread : runnableThreads) {
            sb.append("    ").append(thread.getName()).append(" / ").append("Classloader: ").append(thread.getContextClassLoader()).append(" / ").append("Daemon: ").append(thread.isDaemon())
                    .append(" / ").append(thread.getThreadGroup().getName()).append(", daemon: ").append(thread.getThreadGroup().isDaemon()).append("\n");
            System.err.println("\n\n\n\n");
            System.err.println(thread.getName());
            StackTraceElement[] stackTraceElements = thread.getStackTrace();
            for (StackTraceElement stackTraceElement : stackTraceElements) {
                System.err.println("    " + stackTraceElement.toString());
            }
        }

        sb.append("  There should be none (but only the checker and the block cause logger\n")
                .append("  thread of the testrunner) to unblock.\n")
                .append("  Generate a thread dump if you are interested in more details.\n");
    }

    public void start() {
        if (stopped.compareAndSet(true, false)) {
            new ActiveThreadChecker().start();
        }
    }

    public void stop() {
        stopped.set(true);
    }
}
