package examples.event;

import com.bea.xml.stream.StaticAllocator;
import java.io.FileReader;
import javax.xml.stream.*;
import javax.xml.stream.events.*;
import javax.xml.stream.util.*;
import javax.xml.namespace.QName;

/**
 * A simple example to iterate over events
 *
 * @author Copyright (c) 2002 by BEA Systems. All Rights Reserved.
 */

public class Parse {
  private static String filename = null;
  
  private static void printUsage() {
    System.out.println("usage: java examples.event.Parse <xmlfile>");
  }

  public static void main(String[] args) throws Exception {
    try { 
      filename = args[0];
    } catch (ArrayIndexOutOfBoundsException aioobe){
      printUsage();
      System.exit(0);
    }
    System.setProperty("javax.xml.stream.XMLInputFactory", 
                       "com.bea.xml.stream.MXParserFactory");
    System.setProperty("javax.xml.stream.XMLOutputFactory", 
                       "com.bea.xml.stream.XMLOutputFactoryBase");
    System.setProperty("javax.xml.stream.XMLEventFactory",
                       "com.bea.xml.stream.EventFactory");
    

    XMLInputFactory factory = XMLInputFactory.newInstance();
    XMLEventReader r = 
      factory.createXMLEventReader(new FileReader(filename));
    while(r.hasNext()) {
      XMLEvent e = r.nextEvent();
      System.out.println("ID:"+e.hashCode()+"["+e+"]");
    }
  }
}
