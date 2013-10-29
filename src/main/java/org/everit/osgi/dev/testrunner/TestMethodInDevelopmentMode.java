package org.everit.osgi.dev.testrunner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * With this annotation the test runner will pick up the test method even if the environment is started in development
 * mode. The method still has to have the @Test annotation as well.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface TestMethodInDevelopmentMode {

}
