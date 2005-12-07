package examples.serialize;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;

public class Customer implements XMLizable {
  private String name;
  private int id;
  
  public Customer(){}
  public Customer(String n, int id) {
    this.name = n;
    this.id = id;
  }

  public void read(XMLStreamReader r)  
    throws XMLStreamException
  {
    if (!"customer".equals(r.getLocalName()))
      throw new XMLStreamException("Unexpected element.");
    name = r.getAttributeValue("","name");
    id = Integer.parseInt(r.getAttributeValue("","id"));
    
    // consume the start tag
    r.next();

    // consume the end tag
    r.next();
  }
  
  public void write(XMLStreamWriter w) 
    throws XMLStreamException
  {
    w.writeEmptyElement("customer");
    w.writeAttribute("","name",name);
    w.writeAttribute("","id",Integer.toString(id));
  }

  public String toString() {
    return "CUSTOMER["+id+":"+name+"]";
  }
}
