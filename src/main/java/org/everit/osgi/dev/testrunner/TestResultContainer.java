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
/**
 * Container class that stores the result of tests that were run on this OSGI container.
 */
public final class TestResultContainer {

    /**
     * Result data that contains a merge of each result ran in this OSGI container.
     */
    private static GlobalResult globalResult = null;

    /**
     * Merging the data of partResult to the global one by simply adding each field to each other.
     * 
     * @param partResult
     *            The result data that should be added to the global.
     */
    public static synchronized void addToGlobalResult(final GlobalResult partResult) {
        if (globalResult == null) {
            globalResult = new GlobalResult();
        }
        globalResult.setErrorCount(globalResult.getErrorCount() + partResult.getErrorCount());
        globalResult.setFailureCount(globalResult.getFailureCount() + partResult.getFailureCount());
        globalResult.setIgnoreCount(globalResult.getIgnoreCount() + partResult.getIgnoreCount());
        globalResult.setRunCount(globalResult.getRunCount() + partResult.getRunCount());
        globalResult.setRunTime(globalResult.getRunTime() + partResult.getRunTime());
    }

    public static GlobalResult getGlobalResult() {
        return globalResult;
    }

    /**
     * Private constructor of Util class.
     */
    private TestResultContainer() {
        // Utility class private constructor
    }
}
