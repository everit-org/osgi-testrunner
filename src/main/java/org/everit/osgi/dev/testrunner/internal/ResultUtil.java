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
package org.everit.osgi.dev.testrunner.internal;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
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
   * The number that should be used to get the seconds from a millisec based value during a
   * diviation.
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
   *          The time in millisecs calculates since 1970.
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

  private static void createParentDirectory(final File file) {
    File parentFolder = file.getParentFile();
    boolean folderCreationSuccessful = parentFolder.exists() || parentFolder.mkdirs();
    if (!folderCreationSuccessful) {
      throw new UncheckedIOException(
          new IOException("Cannot create test result folder: " + parentFolder));
    }
  }

  /**
   * Dumping test results in text format.
   *
   * @param testId
   *          Id of the test.
   *
   * @param testClassResult
   *          The results of the test.
   * @param writer
   *          The writer the test results will be written to.
   * @throws IOException
   *           if the writer does not work well.
   */
  public static void dumpTextResult(final TestClassResult testClassResult, final String testId,
      final Writer writer)
      throws IOException {
    String testClassName = testClassResult.className;
    writer
        .write("-------------------------------------------------------------------------------\n");
    writer.write("Test set: " + testClassName + (testId != null ? " (" + testId + ")" : "") + "\n");
    writer
        .write("-------------------------------------------------------------------------------\n");
    writer.write(
        "Tests run: " + testClassResult.runCount + ", Failures: " + testClassResult.failureCount
            + ", Errors: " + testClassResult.errorCount + ", Skipped: "
            + testClassResult.ignoreCount
            + ", Time elapsed: "
            + ResultUtil.convertTimeToString(testClassResult.finishTime - testClassResult.startTime)
            + " sec");
    if (testClassResult.failureCount > 0) {
      writer.write(" <<< FAILURE!");
    }
    writer.write("\n");

    PrintWriter pw = new PrintWriter(writer);
    for (TestCaseResult testCaseResult : testClassResult.testCaseResults) {
      if (testCaseResult.failure != null) {
        Throwable failure = testCaseResult.failure;
        writer.write(testCaseResult.testMethodName + "  Time elapsed: "
            + ResultUtil.convertTimeToString(testCaseResult.finishTime - testCaseResult.startTime)
            + " sec  <<< " + ((failure instanceof AssertionError) ? "FAILURE" : "ERROR") + "!"
            + "\n");

        failure.printStackTrace(pw);
      }
    }
    pw.flush();
  }

  /**
   * Dumping test results in XML format.
   *
   * @param testId
   *          Id of the test.
   * @param testClassResult
   *          The results of the test.
   * @param writer
   *          The writer the test results will be written to.
   */
  public static void dumpXmlResult(final TestClassResult testClassResult, final String testId,
      final Writer writer) {

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

  /**
   * Generateds the name of the test result file without an extension. An extension might be txt or
   * xml later.
   *
   * @param testClassName
   *          The name of the test class.
   * @param testId
   *          The id of the test.
   * @param includeDate
   *          The date when the test was run.
   * @return The name of the file.
   */
  public static String generateFileNameWithoutExtension(final String testClassName,
      final String testId, final boolean includeDate) {
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
   *          The result of the test.
   * @return An XML node representing the testSuite.
   */
  public static Node generateTestSuiteNode(final TestClassResult testClassResult) {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    try {
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document document = db.newDocument();
      Element testSuiteElement = document.createElement("testsuite");
      document.appendChild(testSuiteElement);

      testSuiteElement.setAttribute("failures", String.valueOf(testClassResult.failureCount));
      testSuiteElement.setAttribute("time",
          ResultUtil.convertTimeToString(testClassResult.finishTime - testClassResult.startTime));
      testSuiteElement.setAttribute("errors", String.valueOf(testClassResult.errorCount));
      testSuiteElement.setAttribute("skipped", String.valueOf(testClassResult.ignoreCount));
      testSuiteElement.setAttribute("tests", String.valueOf(testClassResult.runCount));
      testSuiteElement.setAttribute("name", testClassResult.className);

      Element propertiesElement = document.createElement("properties");
      testSuiteElement.appendChild(propertiesElement);
      Set<Entry<Object, Object>> propertyEntrySet = System.getProperties().entrySet();
      for (Entry<Object, Object> propertyEntry : propertyEntrySet) {
        Element propertyElement = document.createElement("property");
        propertiesElement.appendChild(propertyElement);
        propertyElement.setAttribute("name", String.valueOf(propertyEntry.getKey()));
        propertyElement.setAttribute("value", String.valueOf(propertyEntry.getValue()));
      }

      for (TestCaseResult testCaseResult : testClassResult.testCaseResults) {
        Element testCaseElement = document.createElement("testcase");
        testSuiteElement.appendChild(testCaseElement);
        testCaseElement.setAttribute("time",
            ResultUtil.convertTimeToString(testCaseResult.finishTime - testCaseResult.startTime));
        testCaseElement.setAttribute("classname", testClassResult.className);
        testCaseElement.setAttribute("name", testCaseResult.testMethodName);
        if (testCaseResult.failure != null) {
          Throwable failure = testCaseResult.failure;
          Element errorElement = null;
          if (failure instanceof AssertionError) {
            errorElement = document.createElement("failure");
          } else {
            errorElement = document.createElement("error");
          }
          testCaseElement.appendChild(errorElement);
          errorElement.setAttribute("message", failure.getMessage());

          errorElement.setAttribute("type", failure.getClass().getName());
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          failure.printStackTrace(pw);
          errorElement.setTextContent(sw.toString());
        }
      }
      return testSuiteElement;
    } catch (ParserConfigurationException e) {
      LOGGER.log(Level.SEVERE, "Error generating test suite node", e);
    }
    return null;
  }

  /**
   * Resolves the id of the test from the {@link ServiceReference} instance by checking the
   * {@link TestRunnerConstants#SERVICE_PROPERTY_TEST_ID} service property.
   *
   * @param reference
   *          The service reference.
   * @return The id of the test.
   */
  public static String getTestIdFromReference(final ServiceReference<?> reference) {
    Object testIdProp = reference.getProperty(TestRunnerConstants.SERVICE_PROPERTY_TEST_ID);
    if ((testIdProp != null) && (testIdProp instanceof String)) {
      return (String) testIdProp;
    } else {
      return null;
    }
  }

  /**
   * Write the test results into a text file.
   *
   * @param testClassResult
   *          The test results.
   * @param testId
   *          The if of the test.
   * @param file
   *          The file to write to.
   * @param append
   *          Whether to append the file or overwrite it.
   * @throws IOException
   *           if the file cannot be written.
   */
  public static void writeTextResultToFile(final TestClassResult testClassResult,
      final String testId,
      final File file, final boolean append) throws IOException {
    boolean existed = file.exists();
    FileOutputStream fout = new FileOutputStream(file, append);

    try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fout, "UTF8"))) {
      if (existed && append) {
        bw.write("\n\n");
      }
      ResultUtil.dumpTextResult(testClassResult, testId, bw);
    }

  }

  /**
   * Writing the test result in XML format to a file.
   *
   * @param testId
   *          Id of the test.
   * @param testClassResult
   *          The result of test.
   * @param file
   *          The file where test results should be written.
   * @param append
   *          Whether to append or rewrite the test results to the file.
   */
  public static void writeXmlResultToFile(final TestClassResult testClassResult, final File file,
      final String testId, final boolean append) {
    createParentDirectory(file);
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
    } catch (ParserConfigurationException | IOException | TransformerException | SAXException e) {
      LOGGER.log(Level.SEVERE, "Error during dumping test results in XML format", e);
    }
  }

  /**
   * Private constructor for Util class.
   */
  private ResultUtil() {
  }
}
