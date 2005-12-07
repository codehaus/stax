package examples.order;

import javax.xml.stream.*;

public class OrderProcessor {
  private final String ns = "ns://standardOrder"; 
  public OrderProcessor(){}
  public void toStartTag(XMLStreamReader r) 
    throws XMLStreamException
  {
    while(!r.isStartElement()&& r.hasNext())
      r.next();
  }

  public OrderDocument parseDocument(XMLStreamReader r) 
    throws XMLStreamException 
  {
    OrderDocument doc = new OrderDocument();
    toStartTag(r);
    if ("document".equals(r.getLocalName()) &&
        ns.equals(r.getNamespaceURI())) {
      r.next();
      toStartTag(r);
      while ("customer".equals(r.getLocalName()) &&
             ns.equals(r.getNamespaceURI())) {
        doc.addCustomer(parseCustomer(r));
        r.next();
        toStartTag(r);
      } 

      while("order".equals(r.getLocalName()) &&
            ns.equals(r.getNamespaceURI())) {
        doc.addOrder(parseOrder(r));
        r.next();
        toStartTag(r);
      }
    }
    return doc;
  }

  public Customer parseCustomer(XMLStreamReader r) 
    throws XMLStreamException 
  {
    int id=-1;
    String name="defaultName";
    String company="defaultCompany";
    id  = Integer.parseInt(r.getAttributeValue(ns,"id"));
    r.next(); toStartTag(r);
    if ("name".equals(r.getLocalName())) {
      r.next();
      name = r.getText();
      r.next(); toStartTag(r);
    }
    if ("company".equals(r.getLocalName())) {
      r.next();
      company = r.getText();
    }
    return new Customer(id,name,company);
  }

  public Order parseOrder(XMLStreamReader r)
    throws XMLStreamException
  {
    int idRef=-1;
    String part="defaultPart";
    String shipDate="1/1/03";
    r.next(); toStartTag(r);
    if ("customerIdRef".equals(r.getLocalName())) {
      r.next();
      idRef  = Integer.parseInt(r.getText());
      r.next(); toStartTag(r);
    }
    if ("part".equals(r.getLocalName())) {
      r.next();
      part = r.getText();
      r.next(); toStartTag(r);
    }
    if ("ship-date".equals(r.getLocalName())) {
      r.next();
      shipDate = r.getText();
    }
    return new Order(idRef,part,shipDate);
  }

  public static void main(String args[]) 
    throws Exception
  {
    OrderProcessor processor = new OrderProcessor();

    XMLStreamReader r = XMLInputFactory.newInstance().createXMLStreamReader(
                          new java.io.FileInputStream(args[0])
                          );

    OrderDocument doc = processor.parseDocument(r);
    System.out.println(doc);
  }
}






