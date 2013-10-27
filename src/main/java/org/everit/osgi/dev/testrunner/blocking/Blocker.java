package org.everit.osgi.dev.testrunner.blocking;

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
 * A Blocker can hold the tests from running. Many technologies do a set up on a new thread (e.g. Blueprint). In this
 * case a blocker has to be written for the {@link BlockingManager} so that it will not start running the tests until
 * all the technologies finished processing.
 */
public interface Blocker {

    /**
     * Adding a listener to the Blocker so it can notify the listener about blocking and unblocking.
     */
    void addBlockListener(BlockListener blockListener);

    /**
     * The {@link BlockingManager} calls this function periodically to be able to log out the causes of the blocked
     * tests. It is the decision of the implementor of the Blocker what information can be useful.
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
     */
    void removeBlockListener(BlockListener blockListener);
}
