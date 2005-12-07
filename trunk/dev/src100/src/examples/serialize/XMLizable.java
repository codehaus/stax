package examples.serialize;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;

public interface XMLizable {
  public void read(XMLStreamReader reader)
    throws XMLStreamException;

  public void write(XMLStreamWriter writer)
    throws XMLStreamException;
}
