package org.everit.osgi.dev.testrunner.internal.util;

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

import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.everit.osgi.dev.testrunner.Constants;

public final class ThreadUtil {

    private static final AtomicBoolean threadGroupSecurityWarningLogged = new AtomicBoolean(false);

    public static List<Thread> countDeamonThreads() {
        List<Thread> result = new ArrayList<Thread>();
        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
        for (Entry<Thread, StackTraceElement[]> threadAndStackTrace : allStackTraces.entrySet()) {
            Thread thread = threadAndStackTrace.getKey();
            if (!thread.isDaemon() && !Thread.State.TERMINATED.equals(thread.getState())
                    && !thread.equals(Thread.currentThread())
                    && !Constants.SYSTEM_NON_DAEMON_THREAD_NAMES.contains(thread.getName())) {
                result.add(thread);
            }
        }
        return result;
    }

    public static ThreadGroup getRootThreadGroup(final Thread thread) {
        ThreadGroup currentThreadGroup = thread.getThreadGroup();
        ThreadGroup parent = currentThreadGroup.getParent();
        while (parent != null) {
            currentThreadGroup = parent;
            try {
                parent = parent.getParent();
            } catch (SecurityException e) {
                if (!threadGroupSecurityWarningLogged.compareAndSet(false, true)) {
                    System.err.println("WARN: Cannot query the parent of the thread group test runner is in."
                            + " Framework may stop before all tests finishes");
                    e.printStackTrace(System.err);
                }
                parent = null;
            }
        }
        return currentThreadGroup;
    }
    
    public static Thread[] getRunnableThreads(ThreadGroup threadGroup) {
        List<Thread> result = new ArrayList<Thread>();
        int activeCount = threadGroup.activeCount();
        Thread[] threads = new Thread[activeCount];
        threadGroup.enumerate(threads);
        for (Thread thread : threads) {
            if (thread != null && State.RUNNABLE.equals(thread.getState())) {
                result.add(thread);
            }
        }
        return result.toArray(new Thread[0]);
    }

    private ThreadUtil() {
    }
}
