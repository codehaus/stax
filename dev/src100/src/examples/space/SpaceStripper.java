package examples.space;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.util.StreamReaderDelegate;

public class SpaceStripper extends StreamReaderDelegate {
  private SpaceStripper(){}
  public SpaceStripper(XMLStreamReader reader) {
    super(reader);
  }

  private boolean accept() {
    return !isWhiteSpace();
  }

  public int next()
    throws XMLStreamException
  {
    if (hasNext())
      return super.next();
    throw new java.util.NoSuchElementException("next() may not be called "+
                                               " when there are no more "+
                                               " items to return");
  }
  
  public boolean hasNext()
    throws XMLStreamException
  {
    while (super.hasNext()) {
      if (accept()) return true;
      super.next();
    }
    return false;
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

  public static void main(String args[]) throws Exception {
    XMLInputFactory factory = XMLInputFactory.newInstance();
    XMLStreamReader base_reader = factory.createXMLStreamReader(new java.io.FileReader(args[0]));
    XMLStreamReader reader = new SpaceStripper(base_reader);
    while(reader.hasNext()) {
      switch (reader.getEventType()) {
      case XMLStreamConstants.START_ELEMENT:
        System.out.print("<");
        printName(reader);
        printNamespaces(reader);
        printAttributes(reader);
        System.out.print(">");
        break;
      case XMLStreamConstants.END_ELEMENT:
        System.out.print("</");
        printName(reader);
        System.out.print(">");
        break;
      case XMLStreamConstants.SPACE:
      case XMLStreamConstants.CHARACTERS:
        int start = reader.getTextStart();
        int length = reader.getTextLength();
        System.out.print(new String(reader.getTextCharacters(),
                                     start,
                                     length));
        break;
      }
      reader.next();
    }
  }
}


