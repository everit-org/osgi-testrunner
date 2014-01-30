The test runner OSGi bundle
===========================

Introduction
------------

The test runner bundle picks up tests and runs them.


Running OSGi tests
------------------

The module picks up every OSGi service having the following service
properties defined:

 - **eosgi.testId**: This property should have a value capable of being a
   part of a file name. 

 - **eosgi.testEngine**: This property defines the test engine to which the
   test should be passed. Currently, there is a **junit4** implementation
   in the osgi-testrunner-junit4 project.
   
Annotations should be placed into the interface implemented by the OSGi 
service.

The bundle does not start looking for services until there is a blocking
cause. Blocking causes can be:

  - Framework is not started yet (Framework bundle is not in ACTIVE state)

  - Custom blocking causes: It is possible to write custom Blockers and
    register them as OSGi services. If they are registered, they are picked 
    up, and the related tests will not be started after a system startup only
    if all blockers allow it. Currently, a blocker for Blueprint technology
    is implemented in the osgi-testrunner-blueprint project that forces
    the tests to wait until all Blueprintcontainers get into ACTIVE or
    FAILED state.


Ordinary JUnit tests vs. testrunner bundle
------------------------------------------

The tests have to be provided as OSGi services and the JUnit annotations
are required to be placed into the interface the OSGi services implement.

In ordinary JUnit based solutions, the programmer has to provide the class
name so that the JUnit technology can instantiate the class. By using this
bundle, the programmer instantiates the JUnit test class with any kind
of technology.


Environment variables
---------------------

The following environment variables make it easier to integrate the
testrunner with build tools.


  - **EOSGI_STOP_AFTER_TESTS**: In case this system property has the value
    "true", the testrunner bundle stops the OSGi container and the JVM
    after the tests run.

  - **ENV_TEST_RESULT_FOLDER**: By default, only test results in TEXT format
    are written out to the standard output. In case this environment is
    specified, the test results will be dumped in XML and TEXT format to
    the specified folder. 


Maven support
-------------

The [eosgi-maven-plugin][1] uses the testrunner bundle to run the written
tests as a part of the build lifecycle.


[1]: http://github.com/everit-org/eosgi-maven-plugin 