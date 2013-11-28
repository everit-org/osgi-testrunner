package org.everit.osgi.dev.testrunner.blocking;

import java.lang.Thread.State;

import org.everit.osgi.dev.testrunner.Constants;

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
 * When the JVM is started in the way that the {@link Constants#ENV_STOP_AFTER_TESTS} environment variable is set, the
 * testrunner stops the JVM as soon as there is no thread that is in {@link State#RUNNABLE}. By implementing a
 * {@link ShutdownBlocker}, it is possible to make the testrunner waiting a bit more. This can be useful when the JVM
 * has to wait for pheripherials during startup.
 */
public interface ShutdownBlocker {

    /**
     * Adding a listener to the Blocker so it can notify the listener about blocking and unblocking. Normally at least
     * there is one listener that is the test runner itself.
     */
    void addBlockListener(BlockListener blockListener);

    /**
     * The {@link BlockingManager} calls this function periodically to be able to log out the causes of the blocked
     * tests.
     * 
     * @param sb
     *            The causes should be written into the {@link StringBuilder}. It is recommended to start every line
     *            with two spaces and write a line break onto the end of the message to have pretty output.
     */
    void logBlockCauses(StringBuilder sb);

    /**
     * Removing a blocking listener so it does not have to be notified anymore.
     * 
     * @param blockListener
     *            The listener instance.
     */
    void removeBlockListener(BlockListener blockListener);
}
