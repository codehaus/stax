package com.bea.xml.stream.test;

import java.io.FileNotFoundException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import junit.framework.*;
import com.bea.xml.stream.XMLStreamPlayer;


/**
 * A sample test case, testing <code>java.util.Vector</code>.
 *
 */
public class SimpleReaderTest extends BaseTestCase {
  protected String input;
  protected String master;
  protected XMLInputFactory factory;
  public static String fileName;

  public static void main (String[] args) throws Exception{
    fileName = args[0];
    junit.textui.TestRunner.run (suite());
  }

  public void setParams(String in) {
    this.input = in + ".xml";
    this.master = in + ".stream";
  }

  protected void setUp() {
    if(input == null)
      input = "./files/play.xml";
    if(master == null)
      master  = "./files/play.stream";
    factory = XMLInputFactory.newInstance();
  }

  public static Test suite() {
    TestSuite testSuite = new TestSuite();
    SimpleReaderTest tc1 = new SimpleReaderTest();
    tc1.setParams(SimpleReaderTest.fileName);
    testSuite.addTest(tc1);
    return testSuite;
  }

  public void testStreamEquals() throws XMLStreamException, FileNotFoundException {
    globalCounter.increment();
    logger.info("Can the XMLStreamReader properly parse the XML document?");
    XMLStreamReader r1 = 
      factory.createXMLStreamReader(new java.io.FileReader(input));
    XMLStreamReader r2 = 
      new XMLStreamPlayer(new java.io.FileReader(master));  
    Util util = new Util();
    if(r1 == null)
      fail("Reader is null");
    assertTrue(util.equals(r1, r2).getValue());
    r1.close();
    r2.close();
    logger.info("XMLStreamReader successfully parsed " + input);
  }

  public void testEventEquals() throws XMLStreamException, FileNotFoundException {
    globalCounter.increment();
    logger.info("Can the XMLEventReader properly parse the XML document?");
    XMLEventReader e1 = 
      factory.createXMLEventReader(new java.io.FileReader(input));
    XMLEventReader e2 = 
      factory.createXMLEventReader(new XMLStreamPlayer(new java.io.FileReader(master)));

    Util util = new Util();
    if(e1 == null)
      fail("Reader is null");
    assertTrue(util.equals(e1, e2).getValue());
    logger.info("XMLEventReader successfully parsed " + input);
  }

  protected void tearDown() {
    logger.info("Number of tests run so far " + globalCounter.getCount());
  }
}
