package samples;

import com.bea.xml.stream.util.ElementTypeNames;
import java.io.StringReader;
import java.util.Iterator;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.XMLEvent;

/**
 * Requires StAX 1.0 (JSR 173) available at http://jcp.org/en/jsr/detail?id=173
 *
 */
public class TestCdataReport extends TestXml
{
    static String sampleXML =
        //"<?xml version=\"1.0\"?><tag>Some <![CDATA[te]]>xt</tag> \n";
        //"<?xml-stylesheet href=\"xmlconformance.xsl\" type=\"text/xsl\"?>"+
        //"<bar><?xml-stylesheet href=\"xmlconformance.xsl\" type=\"text/xsl\"?></bar>"
        "<foo>\n"
        +"    <bar>baz\n"
        +"        <cheese id=\"3\"/>\n"
        +"        baz\n"
        +"        <cheese/>\n"
        +"        baz\n"
        +"    </bar>\n"
        +"    <doc><![CDATA[<foo>]]></doc>\n"
        +"</foo>\n"
        ;
    
    public static void main(String[] s)
    {
        parseWithSTAX(sampleXML, configureStaxFactory());
        System.exit(0);
    }
    
    
    public static XMLInputFactory configureStaxFactory() throws IllegalArgumentException, FactoryConfigurationError {
        XMLInputFactory factory_d = TestXml.configureStaxFactory();
        final String REPORT_CDATA = "http://java.sun.com/xml/stream/properties/report-cdata-event";
        //Boolean enableCdataReport = Boolean.FALSE;
        Boolean enableCdataReport = Boolean.TRUE;
        try {
            factory_d.setProperty(REPORT_CDATA, enableCdataReport);
        } catch(IllegalArgumentException e) {
            System.out.println("WARNING: property "+REPORT_CDATA+" not supported");
            e.printStackTrace();
        }
        return factory_d;
    }
}

