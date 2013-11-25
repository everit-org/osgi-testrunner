package org.everit.osgi.dev.testrunner.engine;

import java.util.List;

import org.everit.osgi.dev.testrunner.TestDuringDevelopment;
import org.osgi.framework.ServiceReference;

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
 * The class that implements this interface can run tests after the framework is started.
 */
public interface TestEngine {

    /**
     * Runs a test based on the service reference. The function is not called multiple times parallel.
     * 
     * @param reference
     *            The service reference that contains the test.
     * @param developmentMode
     *            In development mode tests should not run unless the class or method is annotated with
     *            {@link TestDuringDevelopment} annotation.
     * 
     * @return The test result of all classes that belong to this reference.
     */
    List<TestClassResult> runTest(ServiceReference<Object> reference, boolean developmentMode);

}
