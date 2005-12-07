package com.bea.xml.stream.test;

import com.bea.xml.stream.util.ElementTypeNames;
import com.bea.xml.stream.XMLStreamPlayer;

import java.util.HashMap;
import java.util.Iterator;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.*;

import javax.xml.stream.XMLStreamException;

public class Util {
  private boolean verbose=false;
  private int depth=0;
  public Util(){}
  public Util(boolean v) {
    verbose = v;
  }
  public EqualityResult equals(XMLStreamReader r1, XMLStreamReader r2) 
    throws XMLStreamException
  {
    depth = 0;
    EqualityResult result = new EqualityResult(false,
                                       "Readers had no content");
    while(r1.hasNext() && r2.hasNext()) {
       result = check(r1,r2);
       if (verbose)
         System.out.println(result);
       if (r1.hasNext())
         r1.next(); 
       if (r2.hasNext())
         r2.next();
       if (!result.getValue())
         return result;
    }
    if (r1.hasNext()) {
      return new EqualityResult(false,"XMLStreamReader 1 had extra events["+ElementTypeNames.getEventTypeString(r1.getEventType())+"]");
    }
    if (r2.hasNext()) {
      return new EqualityResult(false,"XMLStreamReader 2 had extra events["+ElementTypeNames.getEventTypeString(r2.getEventType())+"]");
    }
    
    return new EqualityResult(true,"The two XMLStreamReaders were equal");
  }
  
  private String collect(XMLStreamReader r) 
    throws XMLStreamException
  {
    StringBuffer b = new StringBuffer();
    while(r.hasNext() && (r.getEventType() == XMLEvent.CHARACTERS ||
                          r.getEventType() == XMLEvent.SPACE ||
                          r.getEventType() == XMLEvent.ENTITY_REFERENCE)) {
    
      b.append(r.getText());
      r.next();
    }
    return b.toString();
  }
  public EqualityResult check(XMLStreamReader r1,
                              XMLStreamReader r2)
    throws XMLStreamException
  {
    if (r1.getEventType() != r2.getEventType()) 
      return new EqualityResult(false,
                                "Event types were not equal\n\t"+
                                " r1["+ElementTypeNames.getEventTypeString(r1.getEventType())+"] != r2["+
                                ElementTypeNames.getEventTypeString(r2.getEventType())+"]");
    switch (r1.getEventType()) {
    case XMLEvent.START_ELEMENT:
      depth++;
      if (!checkName(r1,r2)) 
        return new EqualityResult(false,
                                  "START_ELEMENT names must be equal r1:"+
                                  getName(r1)+
                                  " r2:"+
                                  getName(r2));
      if (!checkAttributes(r1,r2))
        return new EqualityResult(false,
                                  "ATTRIBUTES were not equal");
      if (!checkNamespaces(r1,r2))
        return new EqualityResult(false,
                                  "NAMESPACES were not equal");
      break;
    case XMLEvent.END_ELEMENT:
      if (!checkName(r1,r2)) 
        return new EqualityResult(false,
                                  "START_ELEMENT names must be equal r1:"+
                                  getName(r1)+
                                  " r2:"+
                                  getName(r2));
      depth--;
      break;
    case XMLEvent.CHARACTERS:
    case XMLEvent.ENTITY_REFERENCE:
    case XMLEvent.SPACE:
      String s1 = collect(r1);
      String s2 = collect(r2);
      // Ignore text content outside of the body
      if (depth > 0 && !s1.equals(s2))
        return new EqualityResult(false,
                                  "Text content was not equal:["+
                                  s1+"]!=["+s2+"]");
      break;
    case XMLEvent.START_DOCUMENT:
    case XMLEvent.END_DOCUMENT:
      break;
    }
    return new EqualityResult(true,"XMLStreamReaders at current position were equal"+
                                "\n\t r1["+ElementTypeNames.getEventTypeString(r1.getEventType())+"] == r2["+
                                ElementTypeNames.getEventTypeString(r2.getEventType())+"]");

  }
    
  public String getName(XMLStreamReader r) 
    throws XMLStreamException
  {
    return "nsuri=["+r.getNamespaceURI()+
      "],prefix=["+r.getPrefix()+
      "],localname=["+r.getLocalName()+"]";
  }

  private static String checkNull(String s) {
    if (s == null) return "";
    return s;
  }
  public boolean checkName(XMLStreamReader r1, XMLStreamReader r2) 
    throws XMLStreamException
  {
    String p1 = checkNull(r1.getPrefix());
    String p2 = checkNull(r2.getPrefix());

    String ns1 = checkNull(r1.getNamespaceURI());
    String ns2 = checkNull(r2.getNamespaceURI());

    String ln1 = r1.getLocalName();
    String ln2 = r2.getLocalName();

    return (stringEquals(p1,p2) && stringEquals(ns1,ns2) && stringEquals(ln1,ln2));
  }

  public boolean checkAttributes(XMLStreamReader r1, XMLStreamReader r2)
    throws XMLStreamException
  {
    if (r1.getAttributeCount() != r2.getAttributeCount()) 
      return false;
    for (int i =0; i < r1.getAttributeCount(); i++) {
        String ns = r1.getAttributeNamespace(i);
        String name = r1.getAttributeLocalName(i);
        String value = r1.getAttributeValue(i);
        if (!value.equals(r2.getAttributeValue(ns,name)))
          return false;
    }
    return true;
  }

  public boolean checkNamespaces(XMLStreamReader r1, XMLStreamReader r2)
    throws XMLStreamException
  {
    if (r1.getNamespaceCount() != r2.getNamespaceCount())
      return false;
    
    for (int i=0; i < r1.getNamespaceCount(); i++) {
        String prefix=r1.getNamespacePrefix(i);
        String uri=r1.getNamespaceURI(i);
        if (!uri.equals(r2.getNamespaceURI(prefix)))
          return false;
      }
    return true;
  }

  public EqualityResult checkNamespaces(StartElement e1, StartElement e2)
    throws XMLStreamException
  {
    Iterator i1 = e1.getNamespaces();
    Iterator i2 = e2.getNamespaces();

    HashMap map = new HashMap();

    while (i2.hasNext()) {
      Namespace ns = (Namespace) i2.next();
      map.put(ns.getPrefix(),ns);
    }
    
    i2 = e2.getNamespaces();
              
    while(i1.hasNext() && i2.hasNext()) {
      Namespace ns1 = (Namespace) i1.next();
      i2.next();
      Namespace ns2 = (Namespace) map.get(ns1.getPrefix());
      if (ns2 == null) return new EqualityResult(false,
                                                 ns1.getPrefix()+" was not"+
                                                 " found in "+e2);
      if (!ns1.getPrefix().equals(ns2.getPrefix()))
        return new EqualityResult(false,
                                  ns1.getPrefix()+" != "+ns2.getPrefix());
      if (!ns1.getNamespaceURI().equals(ns2.getNamespaceURI()))
        return new EqualityResult(false,
                                  ns1.getNamespaceURI()+ " != "+
                                  ns2.getNamespaceURI());
    }
    if (i1.hasNext())
      return new EqualityResult(false,e1+"has extra namespaces");
    if (i2.hasNext())
      return new EqualityResult(false,e2+"has extra namespaces");
    return new EqualityResult(true,"NAMESPACES were equal");
  }

  public boolean checkAttributes(StartElement e1, StartElement e2)
    throws XMLStreamException
  {
    Iterator i1 = e1.getAttributes();
    Iterator i2 = e2.getAttributes();

    HashMap map = new HashMap();

    while (i2.hasNext()) {
      Attribute att = (Attribute) i2.next();
      map.put(att.getName(),att);
    }
    
    i2 = e2.getAttributes();
              
    while(i1.hasNext() && i2.hasNext()) {
      Attribute att1 = (Attribute) i1.next();
      i2.next();
      Attribute att2 = (Attribute) map.get(att1.getName());
      if (att2 == null) return false;
      if (!att1.getName().equals(att2.getName()))
        return false;
      if (!att1.getValue().equals(att2.getValue()))
        return false;
    }
    if (i1.hasNext() || i2.hasNext())
      return false;
    return true;

  }



  public boolean stringEquals(String s1, String s2) {
    if (s1 == null && s2== null) return true;
    if (s1 == null && s2 != null) return false;
    if (s2 == null && s1 != null) return false;
    return s1.equals(s2);
  }

  public EqualityResult check(XMLEvent e1, XMLEvent e2)
    throws XMLStreamException
  {
    if (e1.getEventType() != e2.getEventType())
      return new EqualityResult(false,
                                "Event types were not equal\n\t"+
                                " e1["+e1+"] != r2["+
                                e2+"]");
    switch(e1.getEventType()) {
    case XMLEvent.START_ELEMENT: 
      depth++;
      if (!((StartElement)e1).getName().equals(((StartElement)e2).getName())) 
        return new  EqualityResult(false,
                                  "START_ELEMENT names must be equal e1:"+
                                   ((StartElement)e1).getName()+
                                  " e2:"+
                                   ((StartElement)e2).getName());

      if (!checkAttributes((StartElement) e1,(StartElement)e2))
        return new EqualityResult(false,
                                  "ATTRIBUTES were not equal");
      EqualityResult nr = checkNamespaces((StartElement) e1,(StartElement)e2);
      if (!nr.getValue())
        return nr;
      break;
    case XMLEvent.END_ELEMENT:
      if (!((EndElement)e1).getName().equals(((EndElement)e2).getName())) 
        return new  EqualityResult(false,
                                   "START_ELEMENT names must be equal e1:"+
                                   ((EndElement)e1).getName()+
                                   " e2:"+
                                   ((EndElement)e2).getName());
      depth--;
      break;
    case XMLEvent.CHARACTERS:
      String d1 = ((Characters) e1).getData();
      String d2 = ((Characters) e2).getData();
      if (depth > 0 && !d1.equals(d2))
        return new EqualityResult(false,
                                  "Text content was not equal:["+
                                  d1+"]!=["+d2+"]");

      break;
    case XMLEvent.START_DOCUMENT:
    case XMLEvent.END_DOCUMENT:
    }
    return new EqualityResult(true,"XMLStreamReaders at current position were equal"+
                                "\n\t r1["+ElementTypeNames.getEventTypeString(e1.getEventType())+"] == r2["+
                                ElementTypeNames.getEventTypeString(e2.getEventType())+"]");

  }

  private String collect(XMLEventReader r1) 
    throws XMLStreamException
  {
    StringBuffer b = new StringBuffer();
    while (r1.hasNext() && ((r1.peek()).isCharacters())) {
      Characters c = (Characters) r1.next();
      b.append(c.getData());
    }
    return b.toString();
  }

  private EqualityResult checkEventCharacters(XMLEventReader r1,
                                              XMLEventReader r2)
    throws XMLStreamException 
  {
    
    if (r1.peek().getEventType() != r2.peek().getEventType())
      return new EqualityResult(false,
                                "Event types were not equal\n\t"+
                                " e1["+r1.peek()+"] != r2["+
                                r2.peek()+"]");
    String d1 = collect(r1);
    String d2 = collect(r2);
    if (depth > 0 && !d1.equals(d2))
      return new EqualityResult(false,
                                "Text content was not equal:["+
                                d1+"]!=["+d2+"]");
    else 
      return new EqualityResult(true,
                                "Text content was equal:["+
                                d1+"]==["+d2+"]");
  }

  public EqualityResult equals(XMLEventReader r1, XMLEventReader r2 )
    throws XMLStreamException
  {
    depth = 0;
    EqualityResult result = new EqualityResult(false,
                                               "EventReaders had no content");
    while(r1.hasNext() && r2.hasNext()) {
      //
      // Special case to check characters
      //
      if (r1.peek().isCharacters())
        result = checkEventCharacters(r1,r2);
      else
        result = check(r1.nextEvent(),r2.nextEvent());
      if (verbose)
        System.out.println(result);
      if (!result.getValue())
        return result;
    }
    if (r1.hasNext()) return new EqualityResult(false,"XMLEventReader 1 had extra events["+r1.next()+"]");
    if (r2.hasNext()) return new EqualityResult(false,"XMLEventReader 2 had extra events["+r2.next()+"]");

    return new EqualityResult(true,"The two XMLEventReaders were equal");
 
  }

  public static void main(String args[]) throws Exception {
    System.setProperty("javax.xml.stream.XMLInputFactory", 
                       args[0]);
    if (args.length == 2) {
      String input = args[1]+".xml";
      String master = args[1]+".stream";
      XMLInputFactory factory = XMLInputFactory.newInstance();
      XMLStreamReader r1 = 
        factory.createXMLStreamReader(new java.io.FileReader(input));
      XMLStreamReader r2 = 
        new XMLStreamPlayer(new java.io.FileReader(master));
      
      Util util = new Util(true);
      System.out.println(util.equals(r1,r2));
     
      XMLEventReader e1 = 
        factory.createXMLEventReader(new java.io.FileReader(input));
      
      XMLEventReader e2 = 
        factory.createXMLEventReader(new XMLStreamPlayer(new java.io.FileReader(master)));

      System.out.println(util.equals(e1,e2));
    } else {
      String input = args[1];
      String master = args[2];
      XMLInputFactory factory = XMLInputFactory.newInstance();
      XMLStreamReader r1 = 
        factory.createXMLStreamReader(new java.io.FileReader(input));
      XMLStreamReader r2 = 
        factory.createXMLStreamReader(new java.io.FileReader(master));
      Util util = new Util();
      System.out.println(util.equals(r1,r2));

      XMLEventReader e1 = 
        factory.createXMLEventReader(new java.io.FileReader(input));
      XMLEventReader e2 = 
        factory.createXMLEventReader(new java.io.FileReader(master));
      System.out.println(util.equals(e1,e2));
    }    
  }
}
