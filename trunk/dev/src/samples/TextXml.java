// copied from http://www.dynamicobjects.com/d2r/archives/TextXML.java.txt
package samples;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

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
public class TextXml
{
    static String sampleXML = "<?xml version=\"1.0\"?>\n<code>" +
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
            parseWithSTAX();
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
    
    private static void parseWithSTAX() throws FactoryConfigurationError
    {
        
        XMLInputFactory factory_d = XMLInputFactory.newInstance();
        factory_d.setProperty("javax.xml.stream.isValidating", Boolean.FALSE);
        factory_d.setProperty("javax.xml.stream.isCoalescing", Boolean.TRUE);
        factory_d.setProperty("javax.xml.stream.isNamespaceAware", Boolean.FALSE);
        
        try {
            XMLStreamReader parser_d = factory_d.createXMLStreamReader(new StringReader(sampleXML));
            parser_d.next();
            while (parser_d.getEventType() != XMLStreamConstants.END_DOCUMENT) {
                if (parser_d.getEventType() == XMLStreamConstants.START_ELEMENT) {
                    String element = parser_d.getLocalName();
                    if (element.equals("id")) {
                        String txt = parser_d.getElementText();
                        System.err.println(element + " = "+txt);
                    }
                }
                parser_d.next();
            }
            
            parser_d.close();
        }
        catch (XMLStreamException e) {
            e.printStackTrace();
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }
}

