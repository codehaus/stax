package org.codehaus.stax.test.evt;

import java.util.*;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.*;
import javax.xml.stream.events.*;

/**
 * Unit tests for testing that START_ELEMENT event object behaves
 * as expected
 */
public class TestStartElem
    extends BaseEventTest
{
    public TestStartElem(String name) {
        super(name);
    }

    public void testStartElemNs()
        throws XMLStreamException
    {
        String XML = "<root xmlns='http://my' xmlns:a='ns:attrs' "
            +"attr1='value1' a:attr2='value2'"
            +"/>";

        XMLEventReader er = getReader(XML, true, false);
        assertTokenType(START_DOCUMENT, er.nextEvent().getEventType());
        XMLEvent evt = er.nextEvent();
        assertTokenType(START_ELEMENT, evt.getEventType());
        // Ok, got the start element... is it ok?
        assertTrue(evt.isStartElement());
        StartElement se = evt.asStartElement();

        NamespaceContext nsCtxt = se.getNamespaceContext();

        assertNotNull("StartElement.getNamespaceContext() should never return null", nsCtxt);
        // First, ones we shouldn't get:
        assertNull(nsCtxt.getPrefix("a"));
        assertNull(nsCtxt.getPrefix("http://foobar"));
        assertNull(nsCtxt.getNamespaceURI("b"));
        assertNull(nsCtxt.getNamespaceURI("http://my"));

        {
            Iterator it = nsCtxt.getPrefixes("http://foobar");
            // Specs don't specify if we should get null, or empty iterator
            assertTrue((it == null) || !it.hasNext());
            it = nsCtxt.getPrefixes("a");
            assertTrue((it == null) || !it.hasNext());
        }
        // Then ones we should:

        assertEquals("a", nsCtxt.getPrefix("ns:attrs"));
        assertEquals("", nsCtxt.getPrefix("http://my"));
        assertEquals("http://my", nsCtxt.getNamespaceURI(""));
        assertEquals("ns:attrs", nsCtxt.getNamespaceURI("a"));

        assertTokenType(END_ELEMENT, er.nextEvent().getEventType());
        assertTokenType(END_DOCUMENT, er.nextEvent().getEventType());
        assertFalse(er.hasNext());
    }

    /*
    /////////////////////////////////////////////////
    // Internal methods:
    /////////////////////////////////////////////////
     */

    private XMLEventReader getReader(String contents, boolean nsAware,
                                     boolean coalesce)
        throws XMLStreamException
    {
        XMLInputFactory f = getInputFactory();
        setNamespaceAware(f, nsAware);
        setCoalescing(f, coalesce);
        setSupportDTD(f, true);
        setValidating(f, false);
        return constructEventReader(f, contents);
    }
}
