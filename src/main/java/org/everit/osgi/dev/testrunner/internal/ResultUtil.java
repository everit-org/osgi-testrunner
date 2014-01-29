/**
 * This file is part of Everit Test Runner Bundle.
 *
 * Everit Test Runner Bundle is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Everit Test Runner Bundle is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Everit Test Runner Bundle.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.everit.osgi.dev.testrunner.internal;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

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

import org.everit.osgi.dev.testrunner.TestRunnerConstants;
import org.everit.osgi.dev.testrunner.engine.TestCaseResult;
import org.everit.osgi.dev.testrunner.engine.TestClassResult;
import org.osgi.framework.ServiceReference;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Util class to help dumping test results into files or streams.
 */
public final class ResultUtil {

    /**
     * The logger of the class.
     */
    private static final Logger LOGGER = Logger.getLogger(ResultUtil.class.getName());

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
     * @param testId
     *            Id of the test.
     * 
     * @param testClassResult
     *            The results of the test.
     * @param writer
     *            The writer the test results will be written to.
     * @throws IOException
     *             if the writer does not work well.
     */
    public static void dumpTextResult(final TestClassResult testClassResult, final String testId, final Writer writer)
            throws IOException {
        String testClassName = testClassResult.getClassName();
        writer.write("-------------------------------------------------------------------------------\n");
        writer.write("Test set: " + testClassName + (testId != null ? " (" + testId + ")" : "") + "\n");
        writer.write("-------------------------------------------------------------------------------\n");
        writer.write("Tests run: " + testClassResult.getRunCount() + ", Failures: " + testClassResult.getFailureCount()
                + ", Errors: " + testClassResult.getErrorCount() + ", Skipped: " + testClassResult.getIgnoreCount()
                + ", Time elapsed: " + ResultUtil.convertTimeToString(testClassResult.getRunTime()) + " sec");
        if (testClassResult.getFailureCount() > 0) {
            writer.write(" <<< FAILURE!");
        }
        writer.write("\n");

        PrintWriter pw = new PrintWriter(writer);
        for (TestCaseResult testCaseResult : testClassResult.getTestCaseResults()) {
            if (testCaseResult.getFailure() != null) {
                Throwable failure = testCaseResult.getFailure();
                writer.write(testCaseResult.getTestMethodName() + "  Time elapsed: "
                        + ResultUtil.convertTimeToString(testCaseResult.getRunningTime()) + " sec  <<< "
                        + ((failure instanceof AssertionError) ? "FAILURE" : "ERROR") + "!" + "\n");

                failure.printStackTrace(pw);
            }
        }
        pw.flush();
    }

    /**
     * Dumping test results in XML format.
     * 
     * @param testId
     *            Id of the test.
     * @param testClassResult
     *            The results of the test.
     * @param writer
     *            The writer the test results will be written to.
     */
    public static void dumpXmlResult(final TestClassResult testClassResult, final String testId, final Writer writer) {

        try {
            Node testSuiteElement = ResultUtil.generateTestSuiteNode(testClassResult);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            Source source = new DOMSource(testSuiteElement);
            javax.xml.transform.Result xmlResult = new StreamResult(writer);
            transformer.transform(source, xmlResult);
            writer.flush();
        } catch (TransformerConfigurationException e) {
            LOGGER.log(Level.SEVERE, "Error during dumping test results in XML format", e);
        } catch (TransformerException e) {
            LOGGER.log(Level.SEVERE, "Error during dumping test results in XML format", e);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error during dumping test results in XML format", e);
        }
    }

    public static String generateFileNameWithoutExtension(final String testClassName, final String testId,
            final boolean includeDate) {
        StringBuilder sb = new StringBuilder(testClassName);
        if (testId != null) {
            sb.append("_").append(testId);
        }
        if (includeDate) {
            sb.append("_");
            SimpleDateFormat dateFormat = new SimpleDateFormat("YYYYMMddHHmmss");
            String formattedDate = dateFormat.format(new Date());
            sb.append(formattedDate);
        }
        return sb.toString();
    }

    /**
     * Generating a testSuite XML node from a test result.
     * 
     * @param testClassResult
     *            The result of the test.
     * @return An XML node representing the testSuite.
     */
    public static Node generateTestSuiteNode(final TestClassResult testClassResult) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.newDocument();
            Element testSuiteElement = document.createElement("testsuite");
            document.appendChild(testSuiteElement);

            testSuiteElement.setAttribute("failures", String.valueOf(testClassResult.getFailureCount()));
            testSuiteElement.setAttribute("time", ResultUtil.convertTimeToString(testClassResult.getRunTime()));
            testSuiteElement.setAttribute("errors", String.valueOf(testClassResult.getErrorCount()));
            testSuiteElement.setAttribute("skipped", String.valueOf(testClassResult.getIgnoreCount()));
            testSuiteElement.setAttribute("tests", String.valueOf(testClassResult.getRunCount()));
            testSuiteElement.setAttribute("name", testClassResult.getClassName());

            Element propertiesElement = document.createElement("properties");
            testSuiteElement.appendChild(propertiesElement);
            Set<Entry<Object, Object>> propertyEntrySet = System.getProperties().entrySet();
            for (Entry<Object, Object> propertyEntry : propertyEntrySet) {
                Element propertyElement = document.createElement("property");
                propertiesElement.appendChild(propertyElement);
                propertyElement.setAttribute("name", String.valueOf(propertyEntry.getKey()));
                propertyElement.setAttribute("value", String.valueOf(propertyEntry.getValue()));
            }
            List<TestCaseResult> testCaseResults = testClassResult.getTestCaseResults();
            for (TestCaseResult testCaseResult : testCaseResults) {
                if (testCaseResult.getFinishTime() != null) {
                    Element testCaseElement = document.createElement("testcase");
                    testSuiteElement.appendChild(testCaseElement);
                    testCaseElement.setAttribute("time",
                            ResultUtil.convertTimeToString(testCaseResult.getRunningTime()));
                    testCaseElement.setAttribute("classname", testClassResult.getClassName());
                    testCaseElement.setAttribute("name", testCaseResult.getTestMethodName());
                    if (testCaseResult.getFailure() != null) {
                        Throwable failure = testCaseResult.getFailure();
                        Element errorElement = null;
                        if (failure instanceof AssertionError) {
                            errorElement = document.createElement("failure");
                        } else {
                            errorElement = document.createElement("error");
                        }
                        testCaseElement.appendChild(errorElement);
                        if (failure != null) {
                            errorElement.setAttribute("message", failure.getMessage());
                        }

                        if (failure != null) {
                            errorElement.setAttribute("type", failure.getClass().getName());
                            StringWriter sw = new StringWriter();
                            PrintWriter pw = new PrintWriter(sw);
                            failure.printStackTrace(pw);
                            errorElement.setTextContent(sw.toString());
                        }
                    }
                }
            }
            return testSuiteElement;
        } catch (ParserConfigurationException e) {
            LOGGER.log(Level.SEVERE, "Error generating test suite node", e);
        }
        return null;
    }

    public static String getTestIdFromReference(final ServiceReference<?> reference) {
        Object testIdProp = reference.getProperty(TestRunnerConstants.SERVICE_PROPERTY_TEST_ID);
        if ((testIdProp != null) && (testIdProp instanceof String)) {
            return (String) testIdProp;
        } else {
            return null;
        }
    }

    public static void writeTextResultToFile(final TestClassResult testClassResult, final String testId,
            final File file, final boolean append) throws IOException {
        boolean existed = file.exists();
        FileOutputStream fout = new FileOutputStream(file, append);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fout, "UTF8"));
        try {
            if (existed && append) {
                bw.write("\n\n");
            }
            ResultUtil.dumpTextResult(testClassResult, testId, bw);
        } finally {
            if (bw != null) {
                bw.close();
            }
        }

    }

    /**
     * Writing the test result in XML format to a file.
     * 
     * @param testId
     *            Id of the test.
     * @param testClassResult
     *            The result of test.
     * @param file
     *            The file where test results should be written.
     * @param append
     *            Whether to append or rewrite the test results to the file.
     */
    public static void writeXmlResultToFile(final TestClassResult testClassResult, final File file,
            final String testId, final boolean append) {
        file.getParentFile().mkdirs();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = null;
            if (file.exists() && append) {
                document = db.parse(file);
            } else {
                document = db.newDocument();
            }
            Node node = document.adoptNode(ResultUtil.generateTestSuiteNode(testClassResult));
            document.appendChild(node);

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            Source source = new DOMSource(node);
            javax.xml.transform.Result xmlResult = new StreamResult(file);
            transformer.transform(source, xmlResult);
        } catch (ParserConfigurationException e) {
            LOGGER.log(Level.SEVERE, "Error during dumping test results in XML format", e);
        } catch (SAXException e) {
            LOGGER.log(Level.SEVERE, "Error during dumping test results in XML format", e);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error during dumping test results in XML format", e);
        } catch (TransformerConfigurationException e) {
            LOGGER.log(Level.SEVERE, "Error during dumping test results in XML format", e);
        } catch (TransformerException e) {
            LOGGER.log(Level.SEVERE, "Error during dumping test results in XML format", e);
        }
    }

    /**
     * Private constructor for Util class.
     */
    private ResultUtil() {
    }
}
