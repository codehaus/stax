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

//http://jira.codehaus.org/browse/STAX-15        StringIndexOutOfBoundsException when parsing PI's
/**
 * Requires StAX 1.0 (JSR 173) parser such as http://stax.codehaus.org/Download
 *
 */
public class TestPi extends TestXml
{
    static String sampleXML =
        "<?xml version=\"1.0\"?>\n"+
        "<?xml-stylesheet href=\"xmlconformance.xsl\" type=\"text/xsl\"?>"+
        "<bar><?xml-stylesheet href=\"xmlconformance.xsl\" type=\"text/xsl\"?></bar>"
        ;
    
    public static void main(String[] s)
    {
        parseWithSTAX(sampleXML);
        System.exit(0);
    }
}

