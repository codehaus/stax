package samples;

import java.io.StringWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
/**
 * Requires StAX RI 1.0 (JSR 173) available at http://stax.codehaus.org/
 */
public class TestSerializer2
{
    private final static String SOAP12 =  "http://www.w3.org/2003/05/soap-envelope";
    private final static String TESTNS =  "http://someTestUri";
    private final static String CHEESENS =  "http://cheese";
    
    public static void doXmlOutput(boolean useRepairing) throws XMLStreamException {
        StringWriter buffer = new StringWriter();
        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        if (useRepairing) {
            outputFactory.setProperty("javax.xml.stream.isRepairingNamespaces", Boolean.TRUE);
        }
        XMLStreamWriter out = outputFactory.createXMLStreamWriter(buffer);
        out.writeStartDocument();
        if (useRepairing) {
            out.setPrefix("env", SOAP12);
            out.setPrefix("test", TESTNS);
        }
        out.writeStartElement("env", "Envelope", SOAP12);
        if (!useRepairing) {
            out.writeNamespace("env", SOAP12);
            out.writeNamespace("test", TESTNS);
        }
        out.writeStartElement("test", "dummyElement", TESTNS);
        if (useRepairing) {
            out.setPrefix("", CHEESENS);
        }
        out.writeStartElement("", "cheddar", CHEESENS);
        if (!useRepairing) {
            out.writeDefaultNamespace(CHEESENS);
        }
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

