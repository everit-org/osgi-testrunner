/*
 * Copyright (C) 2011 Everit Kft. (http://www.everit.biz)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.everit.osgi.dev.testrunner.internal.util;

import org.osgi.framework.Bundle;

/**
 * Util functions for Bundle related questions.
 */
public final class BundleUtil {

  /**
   * Getting the name of a bundle state by it's integer based index value.
   *
   * @param bundleState
   *          The bundle state integer representation.
   * @return The String name of the bundle state.
   */
  public static String getBundleStateName(final int bundleState) {
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

  private BundleUtil() {
  }

}
