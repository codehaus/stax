package examples.serialize;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.util.ReaderDelegate;



public class Main {

  public static void writeList(String fname) 
    throws Exception
  {
    XMLizable[] customerArray = new XMLizable[5];
    customerArray[0] = new Customer("chris",1);
    customerArray[1] = new Customer("scott",2);
    customerArray[2] = new Customer("patrick",3);
    customerArray[3] = new Customer("pete",4);
    customerArray[4] = new Customer("manoj",5);
    
    XMLOutputFactory outputFactory = 
      XMLOutputFactory.newInstance();
    
    XMLStreamWriter writer = 
      outputFactory.createXMLStreamWriter(new java.io.FileOutputStream(fname));

    writer.writeStartElement("customerList");
    writer.writeAttribute("count",
                          Integer.toString(customerArray.length));
    writer.writeCharacters("\n");
    for (int i=0; i < customerArray.length; i++) {
      XMLizable xo = customerArray[i];
      writer.writeCharacters("\t");
      xo.write(writer);
      writer.writeCharacters("\n");
    }
    writer.writeEndElement();
    writer.writeCharacters("\n");
    writer.flush();
    writer.close();

  }
  
  public static void readList(String fname) 
    throws Exception
  {

    XMLInputFactory inputFactory =
      XMLInputFactory.newInstance();

    XMLStreamReader reader = 
      inputFactory.createXMLStreamReader(new java.io.FileReader(fname));
    
    reader.nextTag();
    int numCustomer = Integer.parseInt(reader.getAttributeValue("","count"));
    for (int i=0; i < numCustomer; i++) {
      reader.nextTag();
      XMLizable c = new Customer();
      c.read(reader);
      System.out.println(c);
    }
  }

  public static void main(String args[]) throws Exception {
    writeList("temp.xml");
    readList("temp.xml");
  }
}



