package org.everit.osgi.dev.testrunner.util;

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

import org.osgi.framework.Bundle;

public class BundleUtil {

    private BundleUtil() {
    }
    
    public static final String getBundleStateName(int bundleState) {
        String result = null;
        switch (bundleState) {
        case Bundle.INSTALLED:
            result = "INSTALLED";
            break;
        case Bundle.ACTIVE:
            result = "ACTIVE";
            break;
        case Bundle.RESOLVED:
            result = "RESOLVED";
            break;
        case Bundle.STARTING:
            result = "STARTING";
            break;
        case Bundle.STOPPING:
            result = "STOPPING";
            break;
        case Bundle.UNINSTALLED:
            result = "UNINSTALLED";
            break;
        default:
            result = "UNKNOWN(" + bundleState + ")";
            break;
        }
        return result;
    }

}
