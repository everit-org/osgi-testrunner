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

import org.osgi.framework.BundleContext;

/**
 * A Blocker can hold the tests from running. Many technologies do a set up on a new thread (e.g. Blueprint). In this
 * case a blocker has to be written for the {@link BlockingManager} so that it will not start running the tests until
 * all the technologies finished processing.
 */
public interface Blocker {

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
     * Starting the Blocker. ServiceTrackers, ServiceRegistrations or other resources can be started here that are
     * necessary to monitor the asynchron technology.
     * 
     * @param listener
     *            When a Blocker thinks it should block or unblock the test running it should call the functions of the
     *            listener to let the {@link BlockingManager} know about it.
     * @param context
     *            The {@link BundleContext} of the testrunner bundle.
     */
    void start(BlockListener listener, BundleContext context);

    /**
     * Stopping the Blocker. Every resources should be released here if necessary.
     */
    void stop();
}
