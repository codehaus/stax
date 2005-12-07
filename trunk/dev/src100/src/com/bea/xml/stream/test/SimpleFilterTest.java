package com.bea.xml.stream.test;

import java.io.FileNotFoundException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.StreamFilter;
import javax.xml.stream.EventFilter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import junit.framework.*;
import com.bea.xml.stream.XMLStreamPlayer;
import com.bea.xml.stream.XMLEventPlayer;
import com.bea.xml.stream.filters.TypeFilter;


/**
 * A simple filter test, with Stream Filter and Event Filter. 
 * Events for filtering are START_ELEMENT and END_ELEMENT.
 */
public class SimpleFilterTest extends BaseTestCase {
  protected String input;
  protected String master;
  protected XMLInputFactory factory;
  TypeFilter f;

  public static void main (String[] args) {
    SimpleFilterTest fTest = new SimpleFilterTest();
    fTest.setParams(args[0]);
    junit.textui.TestRunner.run (suite());
  }

  public void setParams(String in) {
    logger.info("Running SimpleFilter Test");
    this.input = in + ".xml";
    //this.master = in + "_filtered.stream";
  }

  protected void setUp() {
    if(input == null)
      input = "./files/play.xml";
    if(master == null)
      master  = "./files/play_filtered_START_ELEMENT_END_ELEMENT.stream";
    factory = XMLInputFactory.newInstance();
    f = new com.bea.xml.stream.filters.TypeFilter();
    f.addType(XMLEvent.START_ELEMENT);
    f.addType(XMLEvent.END_ELEMENT);

  }

  public static Test suite() {
    return new TestSuite(SimpleFilterTest.class);
  }

  public void testStreamEquals() throws XMLStreamException, FileNotFoundException {
    logger.info("Can the XMLStreamReader properly parse the XML document with filter?");
    XMLStreamReader r1 = 
      factory.createFilteredReader( factory.createXMLStreamReader(new java.io.FileReader(input)), (StreamFilter)f);
    XMLStreamReader r2 = 
      new XMLStreamPlayer(new java.io.FileReader(master));  
    Util util = new Util();
    if(r1 == null)
      fail("Reader is null");
    EqualityResult r = util.equals(r1,r2);
    logger.info(r.toString());
    assertTrue(r.getValue());
    r1.close();
    r2.close();
    logger.info("XMLStreamReader successfully parsed " + input + " with filter");
  }

  public void testEventEquals() throws XMLStreamException, FileNotFoundException {
    logger.info("Can the XMLEventReader properly parse the XML document with filter?");
    XMLEventReader e1 = 
      factory.createFilteredReader( factory.createXMLEventReader(new java.io.FileReader(input)), (EventFilter)f);
    XMLEventReader e2 = 
      new XMLEventPlayer(new XMLStreamPlayer(new java.io.FileReader(master)));

    Util util = new Util();
    if(e1 == null)
      fail("Reader is null");
    EqualityResult r = util.equals(e1,e2);
    logger.info(r.toString());
    System.out.println("----------------------------------"+r.toString());
    assertTrue(r.getValue());
    logger.info("XMLEventReader successfully parsed " + input + " with filter");
  }
    
}
