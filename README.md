osgi-testrunner
===============

Introduction
------------

Very often the developers have to write lots of emulation code just to test a function. Writing mocks makes development slow and painful. We think that the tests should run on a system that is similar to the server where the application will finally run except that there are maybe emulated smtp servers, embedded databases, etc.

With the maven-eosgi-plugin it is possible to set up such a test system. With this module it is possible to run the tests on the test system and see the changes without a restart.

Writing tests
-------------

After dropping this bundle with it's dependencies into the OSGi container the written tests need to meet the followings:

- The tests have to be provided as OSGi services
- The services have to have the service property (osgitest.junit4)
- The JUnit annotations (like @Test) should be placed into the interfaces that the services is provided with.
- The classes where that implement the interfaces should not contain any JUnit annotation.
- If you provide multiple test services with the same interfaces you should define the (service.id=XX) service property where XX is a unique id. This is important if you want to run your tests with maven-eosgi-plugin later as it needs a unique id or interface for every tests.

As you can see this solution uses JUnit4 a bit differently from the ordinary ones. JUnit does not instantiate any object here but they are provided as OSGi services already. This is good as you can fill the properties of your OSGi service with any technology you like.

Running the tests during compilation
------------------------------------

The Maven OSGi plugin supports running your tests during the integration-test phase of your project. In case the Maven plugin started the OSGi container (known from environment variables):

- The results of the tests will be written to the folder that is defined by the plugin as JUnit XML and TXT files.
- The server is stopped after all tests ran. The testrunner module thinks that all tests ran when:
 - the framework bundle is in STARTED state
 - there is no pending Blueprint event
 - all caughed OSGi services were ran by JUnit

When all tests ran the system is stopped by the testrunner module. This is done with the following steps:

- stop and waitForStop is called on the framework bundle
- All non-deamon threads with their stacktraces are dumped into a shutdown-error.txt file in the folder specified by the maven plugin. Currently the testrunner waits for max five seconds after the framework is stopped as sometimes non-deamon threads need a really little time to finish.
- System exit is called

The maven plugin fails the integration tests if any non-daemon thread is running after the OSGi framework is stopped. The simple reason is that nobody wants solution where some resources are not freed when the bundle that created the resource is uninstalled. A typical mistake can be when there is no close method called during stopping the bundle on a BasicDataSource that was created by the bundle during starting.

For more information please see the plugin usage site.

Test Driven Development with the testrunner module
--------------------------------------------------

To pick up the first steps please read our step-by-step guide.

Will be another test frameworks supported?
------------------------------------------

We were thinking of it. That is the reason why in the service property the junit4 engine has to be defined. However, this is not top priority.
