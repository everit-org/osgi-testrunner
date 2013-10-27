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

import java.util.List;
import java.util.Set;

import org.everit.osgi.dev.testrunner.engine.TestClassResult;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;

/**
 * Via the TestManager OSGi service 3rd party tools can specify which tests should run after a bundle deployment. The
 * management is based on OSGi {@link Filter}s. Inclusion priority is higher than exclusion. This means, that:
 * <ul>
 * <li>In case a test service matches an exclusion filter, it will be excluded</li>
 * <li>In case a test service matches an inclusion filter, even if it matches with any exclusion filter, it will be
 * included</li>
 * <li>In case a test does not match with any exclusion and inclusion filter, it will be included</li>
 * </ul>
 * 
 */
public interface TestManager {

    /**
     * Adding a test inclusion as an OSGi filter that will take effect during the deployment of the test bundles. For
     * more information see the doc of the interface.
     * 
     * @param filter
     *            The filter.
     * 
     * @return true if the filter was added, false if the filter was already in the list of filters so nothing happened.
     */
    boolean addTestInclusionFilter(Filter filter);

    /**
     * Adding a test exclusion as an OSGi filter that will take effect during the deployment of the test bundles. For
     * more information see the doc of the interface.
     * 
     * @param filter
     *            The filter.
     * 
     * @return true if the filter was added, false if the filter was already in the list of filters so nothing happened.
     */
    boolean addTestExclusionFilter(Filter filter);

    /**
     * Removing a test inclusion as an OSGi filter that will take effect during the deployment of the test bundles. For
     * more information see the doc of the interface.
     * 
     * @param filter
     *            The filter.
     * 
     * @return true if the filter was removed, false if the filter was not in the list of filters so nothing happened.
     */
    boolean removeTestInclusionFilter(Filter filter);

    /**
     * Removing a test exclusion as an OSGi filter that will take effect during the deployment of the test bundles. For
     * more information see the doc of the interface.
     * 
     * @param filter
     *            The filter.
     * 
     * @return true if the filter was removed, false if the filter was not in the list of filters so nothing happened.
     */
    boolean removeTestExclusionFilter(Filter filter);

    /**
     * Getting the exclusions that take effect during the deployment of the test bundles. For more information see the
     * doc of the interface.
     * 
     * @return The set of filters. The set is unordered and modifying the element list of the set will not take effect
     *         to the TestManager.
     */
    Set<Filter> getTestExclusionFilters();

    /**
     * Getting the inclusions that take effect during the deployment of the test bundles. For more information see the
     * doc of the interface.
     * 
     * @return The set of filters. The set is unordered and modifying the element list of the set will not take effect
     *         to the TestManager.
     */
    Set<Filter> getTestInclusionFilters();

    /**
     * Runs all tests that are found based on the service reference.
     * 
     * @param reference
     *            The service reference that points to test objects.
     * 
     * @param evenIfExcluded
     *            if true, the tests will run even if they are excluded based on the filter settings.
     * 
     * @return The test results.
     */
    List<TestClassResult> runTest(ServiceReference<Object> reference, boolean evenIfExcluded);
}
