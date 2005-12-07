package examples.basic;

import java.io.FileReader;
import java.util.Iterator;
import javax.xml.stream.*;
import javax.xml.namespace.QName;
import com.bea.xml.stream.util.ElementTypeNames;

/**
 * This is a simple parsing example that illustrates
 * the XMLStreamReader class.
 *
 * @author Copyright (c) 2003 by BEA Systems. All Rights Reserved.
 */


public class Parse {
  private static String filename = null;
  
  private static void printUsage() {
    System.out.println("usage: java examples.basic.Parse <xmlfile>");
  }

  public static void main(String[] args) throws Exception {
    try { 
      filename = args[0];
    } catch (ArrayIndexOutOfBoundsException aioobe){
      printUsage();
      System.exit(0);
    }

    //
    // Set the factory to the RI
    //
    System.setProperty("javax.xml.stream.XMLInputFactory", 
                       "com.bea.xml.stream.MXParserFactory");
    
    //
    // Get an input factory
    //
    XMLInputFactory xmlif = XMLInputFactory.newInstance();
    System.out.println("FACTORY: " + xmlif);

    //
    // Instantiate a reader
    //
    XMLStreamReader xmlr = xmlif.createXMLStreamReader(new FileReader(filename));
    System.out.println("READER:  " + xmlr + "\n");
    
    //
    // Parse the XML
    //
    while(xmlr.hasNext()){
      printEvent(xmlr);
      xmlr.next();
    }

  }

  private static void printEvent(XMLStreamReader xmlr) {
    System.out.print("EVENT:["+xmlr.getLocation().getLineNumber()+"]["+
                     xmlr.getLocation().getColumnNumber()+"] ");
    System.out.print(ElementTypeNames.getEventTypeString(xmlr.getEventType()));
    System.out.print(" [");
    switch (xmlr.getEventType()) {
    case XMLStreamConstants.START_ELEMENT:
      System.out.print("<");
      printName(xmlr);
      printNamespaces(xmlr);
      printAttributes(xmlr);
      System.out.print(">");

      System.out.println("count------------------>"+xmlr.getNamespaceCount());
      break;
    case XMLStreamConstants.END_ELEMENT:
      System.out.print("</");
      printName(xmlr);
      printNamespaces(xmlr);
      System.out.print(">");
      break;
    case XMLStreamConstants.SPACE:
    case XMLStreamConstants.CHARACTERS:
      int start = xmlr.getTextStart();
      int length = xmlr.getTextLength();
      System.out.print(new String(xmlr.getTextCharacters(),
                                  start,
                                  length));
      break;
    case XMLStreamConstants.PROCESSING_INSTRUCTION:
      System.out.print("<?");
      if (xmlr.hasText())
        System.out.print(xmlr.getText());
      System.out.print("?>");
      break;
    case XMLStreamConstants.CDATA:
      System.out.print("<![CDATA[");
      start = xmlr.getTextStart();
      length = xmlr.getTextLength();
      System.out.print(new String(xmlr.getTextCharacters(),
                                  start,
                                  length));
      System.out.print("]]>");
      break;

    case XMLStreamConstants.COMMENT:
      System.out.print("<!--");
      if (xmlr.hasText())
        System.out.print(xmlr.getText());
      System.out.print("-->");
      break;
    case XMLStreamConstants.ENTITY_REFERENCE:
      System.out.print(xmlr.getLocalName()+"=");
      if (xmlr.hasText())
        System.out.print("["+xmlr.getText()+"]");
      break;
    case XMLStreamConstants.START_DOCUMENT:
      System.out.print("<?xml");
      System.out.print(" version='"+xmlr.getVersion()+"'");
      System.out.print(" encoding='"+xmlr.getCharacterEncodingScheme()+"'");
      if (xmlr.isStandalone())
        System.out.print(" standalone='yes'");
      else
        System.out.print(" standalone='no'");
      System.out.print("?>");
      break;

    }
    System.out.println("]");
  }

  private static void printEventType(int eventType) {
    System.out.print("EVENT TYPE("+eventType+"):");
    System.out.println(ElementTypeNames.getEventTypeString(eventType));
  }

  private static void printName(XMLStreamReader xmlr){
    if(xmlr.hasName()){
      String prefix = xmlr.getPrefix();
      String uri = xmlr.getNamespaceURI();
      String localName = xmlr.getLocalName();
      printName(prefix,uri,localName);
    } 
  }

  private static void printName(String prefix,
                                String uri,
                                String localName) {
    if (uri != null && !("".equals(uri)) ) System.out.print("['"+uri+"']:");
    if (prefix != null) System.out.print(prefix+":");
    if (localName != null) System.out.print(localName);
  }
  
  private static void printAttributes(XMLStreamReader xmlr){

    for (int i=0; i < xmlr.getAttributeCount(); i++) {
      printAttribute(xmlr,i);
    } 
  }
  
  private static void printAttribute(XMLStreamReader xmlr, int index) {
    String prefix = xmlr.getAttributePrefix(index);
    String namespace = xmlr.getAttributeNamespace(index);
    String localName = xmlr.getAttributeLocalName(index);
    String value = xmlr.getAttributeValue(index);
    System.out.print(" ");
    printName(prefix,namespace,localName);
    System.out.print("='"+value+"'");

  }
  
  private static void printNamespaces(XMLStreamReader xmlr){
    for (int i=0; i < xmlr.getNamespaceCount(); i++) {
      printNamespace(xmlr,i);
    }
  }
  
  private static void printNamespace(XMLStreamReader xmlr, int index) {
    String prefix = xmlr.getNamespacePrefix(index);
    String uri = xmlr.getNamespaceURI(index);
    System.out.print(" ");
    if (prefix == null)
      System.out.print("xmlns='"+uri+"'");
    else
      System.out.print("xmlns:"+prefix+"='"+uri+"'");
  }
}
