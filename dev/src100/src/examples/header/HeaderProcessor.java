package examples.header;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamConstants;

public class HeaderProcessor {
  private static final String SOAP_ENV=
    "http://www.w3.org/2003/05/soap-envelope";
  public static boolean advance(String uri,
                                String name,
                                XMLStreamReader r) 
    throws XMLStreamException
  {
    while(r.hasNext()) {
      if (r.isStartElement() &&
          uri.equals(r.getNamespaceURI()) &&
          name.equals(r.getLocalName())) return true;
      r.next();
    }
    return false;
  }

  public static void main(String args[]) throws Exception {
    XMLInputFactory factory = XMLInputFactory.newInstance();
    XMLStreamReader reader = factory.createXMLStreamReader(new java.io.FileReader(args[0]));


    if(advance(SOAP_ENV,
               "Envelope",
               reader)) {
      System.out.println("processing ENVELOPE");
      if (advance(SOAP_ENV,
                  "Header",
                  reader)) {
        System.out.println("processing HEADER");
      }
      if (advance(SOAP_ENV,
                  "Body",
                  reader)) {
        System.out.println("processing BODY");
      }
    }
  }
}


