/**
 * This file is part of OSGi Test Runner Bundle.
 *
 * OSGi Test Runner Bundle is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OSGi Test Runner Bundle is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with OSGi Test Runner Bundle.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.everit.osgi.dev.testrunner.blocking;
/**
 * A {@link ShutdownBlocker} can notify the {@link BlockingManager} that is should block or let starting the test
 * runners via this listener.
 */
public interface BlockListener {

    /**
     * The {@link BlockingManager} should not start the test runners yet.
     */
    void block();

    /**
     * The {@link BlockingManager} should start the test runners if no other {@link ShutdownBlocker} blocks.
     */
    void unblock();
}
