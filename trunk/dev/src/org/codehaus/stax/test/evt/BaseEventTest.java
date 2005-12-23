package org.codehaus.stax.test.evt;

import java.io.*;
import java.util.Iterator;

import javax.xml.stream.*;
import javax.xml.stream.events.*;

import org.codehaus.stax.test.BaseStaxTest;

/**
 * Base class for all StaxTest unit tests that test Event API
 * functionality.
 *
 * @author Tatu Saloranta
 */
public class BaseEventTest
    extends BaseStaxTest
{
    /*
    ///////////////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////////////
     */

    protected BaseEventTest() { }

    protected BaseEventTest(String name) {
        super(name);
    }

    /*
    ///////////////////////////////////////////////////
    // Utility methods
    ///////////////////////////////////////////////////
     */

    protected XMLEventFactory getEventFactory()
        throws FactoryConfigurationError
    {
            return XMLEventFactory.newInstance();
    }

    protected static XMLEventReader constructEventReader(XMLInputFactory f, String content)
        throws XMLStreamException
    {
        return f.createXMLEventReader(new StringReader(content));
    }

    protected static XMLEventReader constructEventReaderForFile(XMLInputFactory f, String filename)
        throws IOException, XMLStreamException
    {
        File inf = new File(filename);
        XMLEventReader er = f.createXMLEventReader(inf.toURL().toString(),
                                                   new FileReader(inf));
        return er;
    }

    /**
     * Method that will iterate through contents of an XML document
     * using specified event reader; will also access some of data
     * to make sure reader reads most of lazy-loadable data.
     * Method is usually called to try to get an exception for invalid
     * content.
     *
     * @return Dummy value calculated on contents; used to make sure
     *   no dead code is eliminated
     */
    protected int streamThrough(XMLEventReader er)
        throws XMLStreamException
    {
        int result = 0;

        while (er.hasNext()) {
            XMLEvent evt = er.nextEvent();
            int type = evt.getEventType();
            result += type;
            if (evt.isCharacters()) {
                result += evt.asCharacters().getData().hashCode();
            }
        }

        return result;
    }

    protected int calcAttrCount(StartElement elem)
        throws XMLStreamException
    {
        int count = 0;
        Iterator it = elem.getAttributes();
        if (it != null) {
            while (it.hasNext()) {
                Attribute attr = (Attribute) it.next();
                ++count;
            }
        }
        return count;
    }
}
