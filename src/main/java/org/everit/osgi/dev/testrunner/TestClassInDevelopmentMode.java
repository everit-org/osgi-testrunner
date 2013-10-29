package org.everit.osgi.dev.testrunner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * With this annotation the test runner will pick up all method of the class even if the environment is started in
 * development mode.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface TestClassInDevelopmentMode {

}
