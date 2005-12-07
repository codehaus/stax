package com.bea.xml.stream.test;

import java.io.FileNotFoundException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import junit.framework.*;
import com.bea.xml.stream.XMLStreamPlayer;


/**
 * Test for testing Equality between two streams, one from XML file and another from a pre-recorded stream file.
 * There are two test cases, one for XMLStreamReader and another for XMLEventReader
 *
 */
public class ReaderTest_wsdl extends BaseTestCase {
  protected String input;
  protected String master;
  protected XMLInputFactory factory;

  public static void main (String[] args) {
    junit.textui.TestRunner.run (suite());
  }

  protected void setUp() {
    input = "./files/wsdl_babelfish.xml";
    master  = "./files/wsdl_babelfish.stream";
    factory = XMLInputFactory.newInstance();
  }

  public static Test suite() {
    return new TestSuite(ReaderTest_wsdl.class);
  }

  public void testStreamEquals() throws XMLStreamException, FileNotFoundException {
    logger.info("Can the XMLStreamReader properly parse the WSDL document?");
    XMLStreamReader r1 = 
      factory.createXMLStreamReader(new java.io.FileReader(input));
    XMLStreamReader r2 = 
      new XMLStreamPlayer(new java.io.FileReader(master));  
    Util util = new Util();
    if(r1 == null)
      fail("Reader is null");
    assertTrue(util.equals(r1, r2).getValue());
    logger.info("XMLStreamReader successfully parsed WSDL document");
    r1.close();
    r2.close();
  }

  public void testEventEquals() throws XMLStreamException, FileNotFoundException {
    logger.info("Can the XMLEventReader properly parse the WSDL document?");
    XMLEventReader e1 = 
      factory.createXMLEventReader(new java.io.FileReader(input));
    XMLEventReader e2 = 
      factory.createXMLEventReader(new XMLStreamPlayer(new java.io.FileReader(master)));

    Util util = new Util();
    if(e1 == null)
      fail("Reader is null");
    assertTrue(util.equals(e1, e2).getValue());
    logger.info("XMLEventReader successfully parsed WSDL document");
  }
    
}
