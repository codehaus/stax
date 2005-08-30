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
 * Requires StAX 1.0 (JSR 173) parser such as http://stax.codehaus.org/Download
 *
 */
public class TestXml2
{
    static String sampleXML =
        "<?xml version=\"1.0\"?>\n"+
        "<?xml-stylesheet href=\"xmlconformance.xsl\" type=\"text/xsl\"?>"+
        "<bar><?xml-stylesheet href=\"xmlconformance.xsl\" type=\"text/xsl\"?></bar>"
        ;
    
    public static void main(String[] s)
    {
        parseWithSTAX();
        System.exit(0);
    }
    
    
    private static void parseWithSTAX() throws FactoryConfigurationError
    {
        
        XMLInputFactory factory_d = XMLInputFactory.newInstance();
        factory_d.setProperty("javax.xml.stream.isValidating", Boolean.FALSE);
        factory_d.setProperty("javax.xml.stream.isCoalescing", Boolean.TRUE);
        factory_d.setProperty("javax.xml.stream.isNamespaceAware", Boolean.FALSE);
        
        try {
            XMLStreamReader pp = factory_d.createXMLStreamReader(new StringReader(sampleXML));
            pp.next();
            while (pp.getEventType() != XMLStreamConstants.END_DOCUMENT) {
                printEvent(pp);
                pp.next();
            }
            
            pp.close();
        }
        catch (XMLStreamException e) {
            e.printStackTrace();
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }
    
    public final static String getEventTypeString(int eventType) {
        return ElementTypeNames.getEventTypeString(eventType);
    }
    
    private static void printEvent(XMLStreamReader xmlr) {
        System.out.print("EVENT:["+xmlr.getLocation().getLineNumber()+"]["+
                             xmlr.getLocation().getColumnNumber()+"] ");
        System.out.print(getEventTypeString(xmlr.getEventType()));
        System.out.print(" [");
        switch (xmlr.getEventType()) {
            case XMLEvent.START_ELEMENT:
                System.out.print("<");
                printName(xmlr);
                printNamespaces(com.bea.xml.stream.XMLEventAllocatorBase.getNamespaces(xmlr));
                printAttributes(xmlr);
                System.out.print(">");
                break;
            case XMLEvent.END_ELEMENT:
                System.out.print("</");
                printName(xmlr);
                printNamespaces(com.bea.xml.stream.XMLEventAllocatorBase.getNamespaces(xmlr));
                System.out.print(">");
                break;
            case XMLEvent.SPACE:
            case XMLEvent.CHARACTERS:
                //System.out.print(xmlr.getText());
                int start = xmlr.getTextStart();
                int length = xmlr.getTextLength();
                System.out.print(new String(xmlr.getTextCharacters(),
                                            start,
                                            length));
                break;
            case XMLEvent.PROCESSING_INSTRUCTION:
                System.out.print("<?");
                System.out.print(xmlr.getPITarget());
                System.out.print(" ");
                System.out.print(xmlr.getPIData());
                System.out.print("?>");
                break;
            case XMLEvent.CDATA:
                System.out.print("<![CDATA[");
                if (xmlr.hasText())
                    System.out.print(xmlr.getText());
                System.out.print("]]>");
                break;
                
            case XMLEvent.COMMENT:
                System.out.print("<!--");
                if (xmlr.hasText())
                    System.out.print(xmlr.getText());
                System.out.print("-->");
                break;
            case XMLEvent.ENTITY_REFERENCE:
                System.out.print(xmlr.getLocalName()+"=");
                if (xmlr.hasText())
                    System.out.print("["+xmlr.getText()+"]");
                break;
            case XMLEvent.START_DOCUMENT:
                System.out.print("<?xml");
                System.out.print(" version='"+xmlr.getVersion()+"'");
                System.out.print(" encoding='"+xmlr.getCharacterEncodingScheme()+"'");
                if (xmlr.isStandalone())
                    System.out.print(" standalone='yes'");
                else
                    System.out.print(" standalone='no'");
                System.out.print("?>");
                break;
                
        }
        System.out.println("]");
    }
    private static void printEventType(int eventType) {
        System.out.print("EVENT TYPE("+eventType+"):");
        System.out.println(getEventTypeString(eventType));
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
    
    private static void printValue(XMLStreamReader xmlr){
        if(xmlr.hasText()){
            System.out.println("HAS VALUE: " + xmlr.getText());
        } else {
            System.out.println("HAS NO VALUE");
        }
    }
    
    private static void printAttributes(XMLStreamReader xmlr){
        if(xmlr.getAttributeCount()>0){
            Iterator ai = com.bea.xml.stream.XMLEventAllocatorBase.getAttributes(xmlr);
            while(ai.hasNext()){
                System.out.print(" ");
                Attribute a = (Attribute) ai.next();
                printAttribute(a);
            }
        }
    }
    
    private static void printAttribute(Attribute a) {
        printName(a.getName().getPrefix(),a.getName().getNamespaceURI(),
                  a.getName().getLocalPart());
        System.out.print("='"+a.getValue()+"'");
    }
    
    private static void printNamespaces(Iterator ni){
        while(ni.hasNext()){
            System.out.print(" ");
            Namespace n = (Namespace) ni.next();
            printNamespace(n);
        }
    }
    
    private static void printNamespace(Namespace n) {
        if (n.isDefaultNamespaceDeclaration())
            System.out.print("xmlns='"+n.getNamespaceURI()+"'");
        else
            System.out.print("xmlns:"+n.getPrefix()+"='"+n.getNamespaceURI()+"'");
    }
    
}

