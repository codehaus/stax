package org.codehaus.stax.test.evt;

import java.io.StringWriter;
import java.util.*;

import javax.xml.stream.*;
import javax.xml.stream.events.*;

/**
 * Class that contains simple tests for making sure that event objects
 * get serialized properly when using {@link XMLEventWriter}.
 */
public class TestEventWriter
    extends BaseEventTest
{
    public TestEventWriter(String name) {
        super(name);
    }

    public void testNonRepairingNsWrite()
        throws XMLStreamException
    {
        XMLOutputFactory f = getOutputFactory();
        StringWriter strw = new StringWriter();
        XMLEventWriter w = f.createXMLEventWriter(strw);

        XMLEventFactory evtf = getEventFactory();

        ArrayList attrs = new ArrayList();
        attrs.add(evtf.createAttribute("attr", "value"));
        attrs.add(evtf.createAttribute("ns", "uri", "attr2", "value2"));
        ArrayList ns = new ArrayList();
        ns.add(evtf.createNamespace("ns", "uri"));
        StartElement elem = evtf.createStartElement("", "", "root",
                                                    attrs.iterator(),
                                                    ns.iterator());
        
        w.add(elem);
        w.add(evtf.createEndElement("", "", "root"));
        w.close();
        
        // Ok, let's read it back:
        String contents = strw.toString();
        
        XMLStreamReader sr = getReader(contents, true);
        
        assertTokenType(START_DOCUMENT, sr.getEventType());
        assertTokenType(START_ELEMENT, sr.next());
        
        assertEquals("root", sr.getLocalName());
        assertEquals(2, sr.getAttributeCount());

        // Ordering of attrs is not guaranteed...
        String ln = sr.getAttributeLocalName(0);
        if (ln.equals("attr")) {
            assertEquals("attr2", sr.getAttributeLocalName(1));
            assertEquals("ns", sr.getAttributePrefix(1));
            assertEquals("uri", sr.getAttributeNamespace(1));
        } else if (ln.equals("attr2")) {
            assertEquals("attr", sr.getAttributeLocalName(1));
            assertEquals("ns", sr.getAttributePrefix(0));
            assertEquals("uri", sr.getAttributeNamespace(0));
        } else {
            fail("Unexpected attr local name '"+ln+"' for attribute #0; expected 'attr' or 'attr2'");
        }

        assertTokenType(END_ELEMENT, sr.next());
    }

    /*
    ////////////////////////////////////////
    // Private methods, other
    ////////////////////////////////////////
     */

    private XMLStreamReader getReader(String contents, boolean nsAware)
        throws XMLStreamException
    {
        XMLInputFactory f = getInputFactory();
        if (!setNamespaceAware(f, nsAware)) { // mode not supported
            return null;
        }
        return constructStreamReader(f, contents);
    }
}
