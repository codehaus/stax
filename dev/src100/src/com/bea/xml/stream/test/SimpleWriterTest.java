package com.bea.xml.stream.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import junit.framework.*;
import com.bea.xml.stream.XMLStreamPlayer;
import com.bea.xml.stream.ReaderToWriter;


/**
 * XMLStreamWriter and XMLEventWriter tests.
 *
 */
public class SimpleWriterTest extends BaseTestCase {
  protected String input;
  protected String output;
  File streamOutput;
  File eventOutput;
  protected XMLInputFactory inputFactory;
  protected XMLOutputFactory outputFactory;
  public static String fileName;

  public static void main (String[] args) {
    fileName = args[0];
    junit.textui.TestRunner.run (suite());
  }

  public void setParams(String in) {
    logger.info("Running SimpleWriter Test");
    this.input = in + ".xml";
  }

  protected void setUp() {
    try {
      if(input == null)
        input = "./files/play.xml";
      inputFactory = XMLInputFactory.newInstance();
      outputFactory = XMLOutputFactory.newInstance();
      streamOutput = File.createTempFile("sw_out1",".tmp",new java.io.File("files"));
      eventOutput = File.createTempFile("sw_out2",".tmp",new java.io.File("files"));
    }catch(IOException ioe) {
      ioe.printStackTrace();
    }
  }

  public static Test suite() {
    TestSuite testSuite = new TestSuite();
    SimpleWriterTest tc1 = new SimpleWriterTest();
    tc1.setParams(SimpleWriterTest.fileName);
    testSuite.addTest(tc1);
    return testSuite;
  }

  public void testStreamEquals() throws XMLStreamException, FileNotFoundException, IOException {
   logger.info("Can the XMLStreamWriter properly parse the XML document?");
   XMLStreamReader r1 = 
      inputFactory.createXMLStreamReader(new java.io.FileReader(input));
    XMLStreamWriter sw = outputFactory.createXMLStreamWriter(new java.io.FileWriter(streamOutput));
    ReaderToWriter rtow = new ReaderToWriter(sw);
    sw = rtow.writeAll(r1);
    r1 = inputFactory.createXMLStreamReader(new java.io.FileReader(input));
    XMLStreamReader r2 = 
      inputFactory.createXMLStreamReader(new java.io.FileReader(streamOutput));
    Util util = new Util();
    if((r1 == null)||(r2 == null))
      fail("Writer is null");
    EqualityResult r = util.equals(r1,r2);
    logger.info(r.toString());
    assertTrue(r.getValue());
    r1.close();
    r2.close();
    sw.close();
    logger.info("XMLStreamWriter successfully parsed XML document " + input);
  }

  public void testEventEquals() throws XMLStreamException, FileNotFoundException, IOException {
    logger.info("Can the XMLEventWriter properly parse the XML document?");
    XMLEventReader e1 = 
      inputFactory.createXMLEventReader(new java.io.FileReader(input));
    XMLEventWriter ew = outputFactory.createXMLEventWriter(new java.io.FileWriter(eventOutput));
    ew.add(e1);
    ew.flush();
    e1 = 
      inputFactory.createXMLEventReader(new java.io.FileReader(input));
    XMLEventReader e2 = 
      inputFactory.createXMLEventReader(new java.io.FileReader(eventOutput));
    
    Util util = new Util();
    if((e1 == null)||(e2 == null))
      fail("Writer is null");
    EqualityResult r = util.equals(e1,e2);
    logger.info(r.toString());
    assertTrue(r.getValue());
    ew.close();
    logger.info("XMLEventWriter successfully parsed XML document " + input);
  }

  protected void tearDown() {
    streamOutput.delete();
    eventOutput.delete();
  }


}
