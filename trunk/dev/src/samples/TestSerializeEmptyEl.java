package samples;

import java.io.StringWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
/**
 * Requires StAX RI 1.0 (JSR 173) available at http://stax.codehaus.org/
 * @author Alek
 */
public class TestSerializeEmptyEl
{
    private final static String SOAP12 =  "http://www.w3.org/2003/05/soap-envelope";
    
    public static void doXmlOutput(boolean useRepairing) throws XMLStreamException {
        StringWriter buffer = new StringWriter();
        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        if (useRepairing) {
            outputFactory.setProperty("javax.xml.stream.isRepairingNamespaces", Boolean.TRUE);
        }
        XMLStreamWriter out = outputFactory.createXMLStreamWriter(buffer);
        out.writeStartDocument();
        out.writeStartElement("env", "Envelope", SOAP12);
        out.writeNamespace("env", SOAP12);
        out.writeNamespace("test", "http://someTestUri");
        out.writeStartElement("env", "Body", SOAP12);
        
        out.writeStartElement("test");
        out.writeAttribute("foo", "bar");
        out.writeEndElement();
        
        out.writeStartElement("test");
        out.writeAttribute("foo", "bar");
        out.writeCharacters("");
        out.writeEndElement();

        out.writeStartElement("test");
        out.writeAttribute("foo", "bar");
        out.writeCharacters(" ");
        out.writeEndElement();
        
        out.writeEndElement();
        out.writeEndElement();
        
        out.writeEndDocument();
        out.close();
        System.out.println("Created "+(useRepairing ? "" : "not")+" using repairing :-");
        System.out.println(buffer);
    }
    
    public static void main(String[] s) throws Exception {
        doXmlOutput(false);
        doXmlOutput(true);
    }
}

