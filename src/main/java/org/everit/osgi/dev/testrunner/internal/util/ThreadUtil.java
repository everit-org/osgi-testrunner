package org.everit.osgi.dev.testrunner.internal.util;

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
