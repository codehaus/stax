// copied from http://www.dynamicobjects.com/d2r/archives/TextXML.java.txt
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
import sun.security.krb5.internal.ai;

//import org.xmlpull.v1.XmlPullParser;
//import org.xmlpull.v1.XmlPullParserException;
//import org.xmlpull.v1.XmlPullParserFactory;


/**
 * TextXML.java
 * demonstrates bug in StAX
 * Requires StAX 1.0 (JSR 173) available at http://jcp.org/en/jsr/detail?id=173
 * and kXML 2 available at http://www.kxml.org/ (based on the Common XML Pull parsing API at http://www.xmlpull.org/)
 *
 * @author diego http://www.dynamicobjects.com/d2r/
 */
public class TestXml
{
    //static String sampleXML = "<?xml version=\"1.0\"?>\n<code>" +
    static String XML = "<?xml version=\"1.0\" standalone=\"yes\" ?>\n<code>" +
        "<id>&lt;code01&gt;</id>" +
        "<id>&lt;code02&gt;</id>" +
        "<id>&lt;code03&gt;</id>" +
        "<id>&lt;code04&gt;</id>" +
        "<id>&lt;code05&gt;</id>" +
        "</code>";
    
    public static void main(String[] s)
    {
        if (s != null && s.length == 1 && s[0].equals("xmlpull")) {
            //parseWithXMLPull();
            /*
             * Result will be: (Correct)
             id = <code01>
             id = <code02>
             id = <code03>
             id = <code04>
             id = <code05>
             */
        }
        else {
            parseWithSTAX(XML);
            /*
             * Result will be: (Incorrect)
             id = <code01
             id = <code02
             id = <code03
             id = <code04
             id = <code05
             */
        }
        System.exit(0);
    }
    
    public static void parseWithSTAX(String sampleXML) throws FactoryConfigurationError {
        XMLInputFactory factory_d = configureStaxFactory();
        parseWithSTAX(sampleXML, configureStaxFactory());
    }
    public static void parseWithSTAX(String sampleXML, XMLInputFactory factory_d) throws FactoryConfigurationError
    {
        
        
        try {
            XMLStreamReader pp = factory_d.createXMLStreamReader(new StringReader(sampleXML));
            pp.next();
            while (pp.getEventType() != XMLStreamConstants.END_DOCUMENT) {
                //                if (parser_d.getEventType() == XMLStreamConstants.START_ELEMENT) {
                //                    String element = parser_d.getLocalName();
                //                    System.err.println(element);
                //                }
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
    
    public static XMLInputFactory configureStaxFactory() throws IllegalArgumentException, FactoryConfigurationError {
        XMLInputFactory factory_d = XMLInputFactory.newInstance();
        factory_d.setProperty("javax.xml.stream.isValidating", Boolean.FALSE);
        factory_d.setProperty("javax.xml.stream.isCoalescing", Boolean.TRUE);
        factory_d.setProperty("javax.xml.stream.isNamespaceAware", Boolean.FALSE);
        return factory_d;
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
    
    //        private static void parseWithXMLPull()
    //        {
    //                try {
    //                        XmlPullParserFactory factory = XmlPullParserFactory.newInstance(System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
    //                        XmlPullParser parser = factory.newPullParser();
    //                        parser.setInput(new StringReader(sampleXML));
    //                        parser.next();
    //                        while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
    //                                if (parser.getEventType() == XmlPullParser.START_TAG) {
    //                                        String element = parser.getName();
    //                                        if (element.equals("id")) {
    //                                                while (parser.getEventType() != XmlPullParser.TEXT) {
    //                                                        parser.next();
    //                                                }
    //                                                String txt = parser.getText();
    //                                                System.err.println(element + " = "+txt);
    //                                        }
    //                                }
    //                                parser.next();
    //                        }
    //                }
    //                catch (XmlPullParserException e) {
    //                        e.printStackTrace();
    //                }
    //                catch (IOException e) {
    //                        e.printStackTrace();
    //                }
    //                catch (FactoryConfigurationError e) {
    //                        e.printStackTrace();
    //                }
    //                        //parser.close();
    //
    //        }
    
    //    private static void parseWithSTAX() throws FactoryConfigurationError
    //    {
    //
    //        XMLInputFactory factory_d = XMLInputFactory.newInstance();
    //        factory_d.setProperty("javax.xml.stream.isValidating", Boolean.FALSE);
    //        factory_d.setProperty("javax.xml.stream.isCoalescing", Boolean.TRUE);
    //        factory_d.setProperty("javax.xml.stream.isNamespaceAware", Boolean.FALSE);
    //
    //        try {
    //            XMLStreamReader parser_d = factory_d.createXMLStreamReader(new StringReader(sampleXML));
    //            parser_d.next();
    //            while (parser_d.getEventType() != XMLStreamConstants.END_DOCUMENT) {
    //                if (parser_d.getEventType() == XMLStreamConstants.START_ELEMENT) {
    //                    String element = parser_d.getLocalName();
    //                    if (element.equals("id")) {
    //                        String txt = parser_d.getElementText();
    //                        System.err.println(element + " = "+txt);
    //                    }
    //                }
    //                parser_d.next();
    //            }
    //
    //            parser_d.close();
    //        }
    //        catch (XMLStreamException e) {
    //            e.printStackTrace();
    //        }
    //        catch (NumberFormatException e) {
    //            e.printStackTrace();
    //        }
    //    }
    
}

