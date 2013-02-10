package org.everit.osgi.dev.testrunner.junit4;

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

import java.util.List;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

/**
 * Class that extends the {@link BlockJUnit4ClassRunner} functionality to make it possible to run tests on created
 * objects and getting the annotations from the specified interface the object implements. In integration tests it may
 * be necessary that we want to set up an object and add value to the properties of the object before we run tests on
 * it.
 * 
 */
public class BlockJUnit4ObjectRunner extends BlockJUnit4ClassRunner {

    /**
     * The object which has the test functions.
     */
    private Object testObject;

    /**
     * Constructor of the class.
     * 
     * @param klass
     *            The interface or parent class of the testObject that contains the annotated functions.
     * @param testObject
     *            The object that has the test functions to run.
     * @throws InitializationError
     *             if there was any error during the initialization of this class.
     */
    public BlockJUnit4ObjectRunner(final Class<?> klass, final Object testObject) throws InitializationError {
        super(klass);
        this.testObject = testObject;
    }

    /**
     * Simply giving back the testObject we got via the constructor.
     * 
     * @return The value of the testObject property.
     */
    @Override
    protected Object createTest() {
        return testObject;
    }

    public Object getTestObject() {
        return testObject;
    }

    /**
     * Avoiding the validation of the constructor the test object is already instantiated and interfaces do not have
     * constructor at all.
     * 
     * @param errors
     *            Not used.
     */
    @Override
    protected void validateConstructor(final List<Throwable> errors) {
        // In case of objects we do not want to validate constructors so nothing has to be done
    }
}
