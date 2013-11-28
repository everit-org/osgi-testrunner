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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class ThreadUtil {

    public static List<Thread> countDeamonThreads() {
        List<Thread> result = new ArrayList<Thread>();
        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
        for (Entry<Thread, StackTraceElement[]> threadAndStackTrace : allStackTraces.entrySet()) {
            Thread thread = threadAndStackTrace.getKey();
            if (!thread.isDaemon() && !Thread.State.TERMINATED.equals(thread.getState())
                    && !thread.equals(Thread.currentThread())
                    && (threadAndStackTrace.getValue().length > 0)) {
                result.add(thread);
            }
        }
        return result;
    }

    private ThreadUtil() {
    }

}
