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
public class WriterTest_wsdl extends BaseTestCase {
  protected String input;
  protected String output;
  File streamOutput;
  File eventOutput;
  protected XMLInputFactory inputFactory;
  protected XMLOutputFactory outputFactory;

  public static void main (String[] args) {
    junit.textui.TestRunner.run (suite());
  }

  protected void setUp() {
    try {
      logger.info("Writer for wsdl Test Setup");
      input = "./files/wsdl_babelfish.xml";
      inputFactory = XMLInputFactory.newInstance();
      outputFactory = XMLOutputFactory.newInstance();
      streamOutput = File.createTempFile("wsdl_out1",".tmp",new java.io.File("files"));
      eventOutput = File.createTempFile("wsdl_out2",".tmp",new java.io.File("files"));
    }catch(IOException ioe) {
      ioe.printStackTrace();
    }
  }

  public static Test suite() {
    return new TestSuite(WriterTest_wsdl.class);
  }

  public void testStreamEquals() throws XMLStreamException, FileNotFoundException, IOException {
    logger.info("Can the XMLStreamWriter properly parse the WSDL document?");
    XMLStreamReader r1 = 
      inputFactory.createXMLStreamReader(new java.io.FileReader(input));
    XMLStreamWriter sw = outputFactory.createXMLStreamWriter(new java.io.FileWriter(streamOutput));
    ReaderToWriter rtow = new ReaderToWriter(sw);
    sw = rtow.writeAll(r1);
    XMLStreamReader r2 = 
      inputFactory.createXMLStreamReader(new java.io.FileReader(streamOutput));
    Util util = new Util();
    if((r1 == null)||(r2 == null))
      fail("Writer is null");
    assertTrue("Completed StreamEquals() for Writer", util.equals(r1, r2).getValue());
    r1.close();
    r2.close();
    sw.close();
    logger.info("XMLStreamWriter successfully parsed WSDL document");
  }

  public void testEventEquals() throws XMLStreamException, FileNotFoundException, IOException {
    logger.info("Can the XMLEventWriter properly parse the WSDL document?");
    XMLEventReader e1 = 
      inputFactory.createXMLEventReader(new java.io.FileReader(input));
    XMLEventWriter ew = outputFactory.createXMLEventWriter(new java.io.FileWriter(eventOutput));
    ew.add(e1);
    ew.flush();
    XMLEventReader e2 = 
      inputFactory.createXMLEventReader(new java.io.FileReader(eventOutput));
    
    Util util = new Util();
    if((e1 == null)||(e2 == null))
      fail("Writer is null");
    assertTrue("Completed EventEquals() for Writer", util.equals(e1, e2).getValue());
    ew.close();
    logger.info("XMLEventWriter successfully parsed WSDL document");
  }

  protected void tearDown() {
    streamOutput.delete();
    eventOutput.delete();
  }


}
