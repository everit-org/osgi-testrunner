The test runner OSGi bundle
===========================

Introduction
------------

The test runner bundle picks up tests and runs them.


Running OSGi tests
------------------

The module picks up every OSGi service that has the service property
**osgitest=junit4**. In case such a service is found it is passed to JUnit.
Annotations should be placed into the interface the OSGi service implements.

The bundle does not start looking for services until there is any blocking
cause. Blocking causes can be:

  - Framework is not started yet (Framework bundle is not in ACTIVE state)

  - A BlueprintContainer is starting

  - In later versions it will be possible to write custom blocking causes
    like the one that checks if there is any BlueprintContainer starting up
    on a separate thread.


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