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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.runner.notification.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Util class to help dumping test results into files or streams.
 */
public final class JunitResultUtil {

    /**
     * The logger of the class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(JunitResultUtil.class);

    /**
     * The number that should be used to get the seconds from a millisec based value during a diviation.
     */
    private static final int MILLISEC_DECIMAL_DIVIDER = 1000;

    /**
     * The smallest number that needs three digits.
     */
    private static final int SMALLEST_THREE_DIGIT_DECIMAL = 100;

    /**
     * The smallest number that needs two digit.
     */
    private static final int SMALLEST_TWO_DIGIT_DECIMAL = 10;

    /**
     * Converting the time into String format.
     * 
     * @param time
     *            The time in millisecs calculates since 1970.
     * @return The String representation of the time: seconds.millisecs.
     */
    public static String convertTimeToString(final long time) {
        StringBuilder sb = new StringBuilder();
        sb.append(time / MILLISEC_DECIMAL_DIVIDER);
        long millisecs = time % MILLISEC_DECIMAL_DIVIDER;
        if (millisecs > 0) {
            sb.append(".");
            if (millisecs < SMALLEST_TWO_DIGIT_DECIMAL) {
                sb.append("00");
            } else if (millisecs < SMALLEST_THREE_DIGIT_DECIMAL) {
                sb.append("0");
            }
            sb.append(millisecs);
        }

        return sb.toString();
    }

    /**
     * Dumping test results in text format.
     * 
     * @param testId Id of the test.
     * @param testClazz
     *            The class or interface that these test results belong to.
     * @param extendedResult
     *            The results of the test.
     * @param writer
     *            The writer the test results will be written to.
     * @throws IOException
     *             if the writer does not work well.
     */
    public static void dumpTextResult(final Class<?> testClazz, final ExtendedResult extendedResult,
            final String testId,
            final Writer writer) throws IOException {
        writer.write("-------------------------------------------------------------------------------\n");
        writer.write("Test set: " + testClazz.getName() + (testId != null ? " (" + testId + ")" : "") + "\n");
        writer.write("-------------------------------------------------------------------------------\n");
        writer.write("Tests run: " + extendedResult.getRunCount() + ", Failures: "
                + extendedResult.getFailureCount() + ", Errors: "
                + extendedResult.getErrorCount() + ", Skipped: "
                + extendedResult.getIgnoreCount() + ", Time elapsed: "
                + JunitResultUtil.convertTimeToString(extendedResult.getRunTime()) + " sec");
        if (extendedResult.getFailureCount() > 0) {
            writer.write(" <<< FAILURE!");
        }
        writer.write("\n");

        PrintWriter pw = new PrintWriter(writer);
        for (TestCaseResult testCaseResult : extendedResult.getTestCaseResults()) {
            if (testCaseResult.getFailure() != null) {
                Failure failure = testCaseResult.getFailure();
                writer.write(failure.getDescription() + "  Time elapsed: "
                        + JunitResultUtil.convertTimeToString(testCaseResult.getRunningTime()) + " sec  <<< "
                        + ((failure.getException() instanceof AssertionError) ? "FAILURE" : "ERROR") + "!" + "\n");

                failure.getException().printStackTrace(pw);
            }
        }
    }

    /**
     * Dumping test results in XML format.
     * 
     * @param testId Id of the test.
     * @param testClazz
     *            The class or interface that these test results belong to.
     * @param extendedResult
     *            The results of the test.
     * @param writer
     *            The writer the test results will be written to.
     */
    public static void dumpXmlResult(final Class<?> testClazz, final ExtendedResult extendedResult,
            final String testId,
            final Writer writer) {

        try {
            Node testSuiteElement = JunitResultUtil.generateTestSuiteNode(testClazz, extendedResult);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            Source source = new DOMSource(testSuiteElement);
            javax.xml.transform.Result xmlResult = new StreamResult(writer);
            transformer.transform(source, xmlResult);
            writer.flush();
        } catch (TransformerConfigurationException e) {
            LOGGER.error("Error during dumping test results in XML format", e);
        } catch (TransformerException e) {
            LOGGER.error("Error during dumping test results in XML format", e);
        } catch (IOException e) {
            LOGGER.error("Error during dumping test results in XML format", e);
        }
    }

    /**
     * Generating a testSuite XML node from a test result.
     * 
     * @param testClazz
     *            The class or interface the test belongs to.
     * @param extendedResult
     *            The result of the test.
     * @return An XML node representing the testSuite.
     */
    public static Node generateTestSuiteNode(final Class<?> testClazz, final ExtendedResult extendedResult) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.newDocument();
            Element testSuiteElement = document.createElement("testsuite");
            document.appendChild(testSuiteElement);

            testSuiteElement.setAttribute("failures", String.valueOf(extendedResult.getFailureCount()));
            testSuiteElement.setAttribute("time", JunitResultUtil.convertTimeToString(extendedResult.getRunTime()));
            testSuiteElement.setAttribute("errors", String.valueOf(extendedResult.getErrorCount()));
            testSuiteElement.setAttribute("skipped", String.valueOf(extendedResult.getIgnoreCount()));
            testSuiteElement.setAttribute("tests", String.valueOf(extendedResult.getRunCount()));
            testSuiteElement.setAttribute("name", testClazz.getName());

            Element propertiesElement = document.createElement("properties");
            testSuiteElement.appendChild(propertiesElement);
            Set<Entry<Object, Object>> propertyEntrySet = System.getProperties().entrySet();
            for (Entry<Object, Object> propertyEntry : propertyEntrySet) {
                Element propertyElement = document.createElement("property");
                propertiesElement.appendChild(propertyElement);
                propertyElement.setAttribute("name", String.valueOf(propertyEntry.getKey()));
                propertyElement.setAttribute("value", String.valueOf(propertyEntry.getValue()));
            }
            List<TestCaseResult> testCaseResults = extendedResult.getTestCaseResults();
            for (TestCaseResult testCaseResult : testCaseResults) {
                if (testCaseResult.getFinishTime() != null) {
                    Element testCaseElement = document.createElement("testcase");
                    testSuiteElement.appendChild(testCaseElement);
                    testCaseElement.setAttribute("time",
                            JunitResultUtil.convertTimeToString(testCaseResult.getRunningTime()));
                    testCaseElement.setAttribute("classname", testCaseResult.getDescription().getClassName());
                    testCaseElement.setAttribute("name", testCaseResult.getDescription().getMethodName());
                    if (testCaseResult.getFailure() != null) {
                        Failure failure = testCaseResult.getFailure();
                        Element errorElement = null;
                        if (failure.getException() instanceof AssertionError) {
                            errorElement = document.createElement("failure");
                        } else {
                            errorElement = document.createElement("error");
                        }
                        testCaseElement.appendChild(errorElement);
                        if (failure.getMessage() != null) {
                            errorElement.setAttribute("message", failure.getMessage());
                        } else {
                            errorElement.setAttribute("message", failure.getException().getClass() + ":");
                        }

                        if (failure.getException() != null) {
                            Throwable throwable = failure.getException();
                            errorElement.setAttribute("type", throwable.getClass().getName());
                            StringWriter sw = new StringWriter();
                            PrintWriter pw = new PrintWriter(sw);
                            throwable.printStackTrace(pw);
                            errorElement.setTextContent(sw.toString());
                        }
                    }
                }
            }
            return testSuiteElement;
        } catch (ParserConfigurationException e) {
            LOGGER.error("Error generating test suite node", e);
        }
        return null;
    }

    /**
     * Writing the test result in XML format to a file.
     * 
     * @param testId Id of the test.
     * @param testClazz
     *            The class or interface the test belongs to.
     * @param extendedResult
     *            The result of test.
     * @param file
     *            The file where test results should be written.
     * @param append
     *            Whether to append or rewrite the test results to the file.
     */
    public static void writeXmlResultToFile(final Class<?> testClazz, final ExtendedResult extendedResult,
            final File file, final String testId, final boolean append) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = null;
            if (file.exists() && append) {
                document = db.parse(file);
            } else {
                document = db.newDocument();
            }
            Node node = document.adoptNode(JunitResultUtil.generateTestSuiteNode(testClazz, extendedResult));
            document.appendChild(node);

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            Source source = new DOMSource(node);
            javax.xml.transform.Result xmlResult = new StreamResult(file);
            transformer.transform(source, xmlResult);
        } catch (ParserConfigurationException e) {
            LOGGER.error("Error during dumping test results in XML format", e);
        } catch (SAXException e) {
            LOGGER.error("Error during dumping test results in XML format", e);
        } catch (IOException e) {
            LOGGER.error("Error during dumping test results in XML format", e);
        } catch (TransformerConfigurationException e) {
            LOGGER.error("Error during dumping test results in XML format", e);
        } catch (TransformerException e) {
            LOGGER.error("Error during dumping test results in XML format", e);
        }
    }

    /**
     * Private constructor for Util class.
     */
    private JunitResultUtil() {
    }
}
