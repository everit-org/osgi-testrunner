The test runner OSGi bundle
===========================

Introduction
------------

The test runner bundle picks up tests and runs them.


Running OSGi tests
------------------

The module picks up every OSGi service that has the following service
properties defined:

 - **eosgi.testId**: This property should have a value that can be a
   part of a file name. 

 - **eosgi.testEngine**: This property defines which test engine the
   test should be passed to. Currently there is a **junit4** implemented
   in the osgi-testrunner-junit4 project.
   
Annotations should be placed into the interface the OSGi service implements.

The bundle does not start looking for services until there is any blocking
cause. Blocking causes can be:

  - Framework is not started yet (Framework bundle is not in ACTIVE state)

  - Custom blocking causes: It is possible to write custom Blockers and
    register them as OSGi service. In case they are registered, they will
    be picked up and the tests will not run after a system startup until
    all blockers allow it. Currently a blocker for Blueprint technology
    is implemented in the osgi-testrunner-blueprint project that forces
    the tests to wait until all Blueprintcontainers get to the ACTIVE or
    FAILED state.


Ordinary JUnit tests vs. testrunner bundle
------------------------------------------

The tests have to be provided as OSGi services and the JUnit annotations
must be placed into the interface the OSGi services implement.

In ordinary JUnit based solutions the programmer has to provide the class
name somehow and the JUnit technology instantiates the class. By using this
bundle the programmer instantiates the JUnit test class with any kind
of technology.


Environment variables
---------------------

The following environment variables make it easier to integrate the
testrunner with build tools.


  - **EOSGI_STOP_AFTER_TESTS**: In case this system property has the value
    "true" the testrunner bundle will stop the OSGi container and the JVM
    after the tests ran.

  - **ENV_TEST_RESULT_FOLDER**: By default only TEXT format test results
    are written out to the standard output. In case this environment is
    specified the test results will be dumped in XML and TEXT format to
    the specified folder. 


Maven support
-------------

The [eosgi-maven-plugin][1] uses the testrunner bundle to run the written
tests as the part of the build lifecycle.


[1]: http://github.com/everit-org/eosgi-maven-plugin 