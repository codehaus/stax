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
        
        XMLStreamReader sr = getReader(contents, true, true);
        
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

    /**
     * The idea of this test is to basically verify that given a simplish
     * input document, we can parse, output and re-parse it; and second
     * time around still get the same events (at least by type, and maybe
     * doing some simple sanity checks).
     */
    public void testPassThrough()
        throws XMLStreamException
    {
        final String INPUT =
            "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
            +"<!DOCTYPE root ["
            +"]>\n<!-- the doc...-->"
            +"<root xmlns:ns='urn:foo'>\n"
            +"  <branch attr='val' ns:foo='bar'>\n"
            +"    <ns:leaf xmlns='another-uri' />\n"
            +"    <?proc instr?>\n"
            +"<leaf><![CDATA[ Haa: <nottag /> ]]></leaf>\n"
            +"  </branch>\n"
            +"  <!-- another comment -->"
            +"</root>";
            ;
        XMLEventReader er = getEventReader(INPUT, true, true);
        List list1 = collectEvents(er);

        StringWriter strw = new StringWriter();
        XMLOutputFactory f = getOutputFactory();
        XMLEventWriter ew = f.createXMLEventWriter(strw);
        Iterator it = list1.iterator();
        while (it.hasNext()) {
            ew.add((XMLEvent) it.next());
        }

        // And re-parse...
        er = getEventReader(INPUT, true, true);
        List list2 = collectEvents(er);

        assertEquals("Should have gotten same number of events",
                     list1.size(), list2.size());

        // And finally, let's at least compare types we have:
        it = list1.iterator();
        Iterator it2 = list2.iterator();

        for (int ix = 0; it.hasNext(); ++ix) {
            XMLEvent evt1 = (XMLEvent) it.next();
            XMLEvent evt2 = (XMLEvent) it2.next();

            if (evt1.getEventType() != evt2.getEventType()) {
                fail("Event #"+ix+"; first time got event "+evt1.getEventType()
                     +", second time "+evt2.getEventType());
            }

            if (evt1.isStartElement()) {
                /* ok, should have same attrs and ns decls. For now, let's
                 * just verify raw counts; can/should test contents too
                 * in future.
                 */
                StartElement se1 = evt1.asStartElement();
                StartElement se2 = evt2.asStartElement();
                List attrs1 = fetchElems(se1.getAttributes());
                List attrs2 = fetchElems(se2.getAttributes());
                assertEquals(attrs1.size(), attrs2.size());

                List ns1 = fetchElems(se1.getNamespaces());
                List ns2 = fetchElems(se2.getNamespaces());
                assertEquals(ns1.size(), ns2.size());
            }
        }
    }

    private List fetchElems(Iterator it)
    {
        ArrayList l = new ArrayList();
        while (it.hasNext()) {
            l.add(it.next());
        }
        return l;
    }

    /*
    ////////////////////////////////////////
    // Private methods, other
    ////////////////////////////////////////
     */

    private XMLStreamReader getReader(String contents, boolean nsAware,
                                      boolean coalesce)
        throws XMLStreamException
    {
        XMLInputFactory f = getInputFactory();
        setNamespaceAware(f, nsAware);
        setCoalescing(f, coalesce);
        setSupportDTD(f, true);
        setValidating(f, false);
        return constructStreamReader(f, contents);
    }

    private XMLEventReader getEventReader(String contents, boolean nsAware,
                                           boolean coalesce)
        throws XMLStreamException
    {
        XMLInputFactory f = getInputFactory();
        setNamespaceAware(f, nsAware);
        setCoalescing(f, coalesce);
        setSupportDTD(f, true);
        setValidating(f, false);
        XMLStreamReader sr = constructStreamReader(f, contents);
        return f.createXMLEventReader(sr);
    }

    private List collectEvents(XMLEventReader er)
        throws XMLStreamException
    {
        ArrayList events = new ArrayList();
        while (er.hasNext()) {
            events.add(er.nextEvent());
        }
        return events;
    }
}
