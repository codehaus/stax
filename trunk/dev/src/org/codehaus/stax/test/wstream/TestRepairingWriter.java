package org.codehaus.stax.test.wstream;

import java.io.*;

import javax.xml.stream.*;

/**
 * Set of unit tests for verifying operation of {@link XMLStreamWriter}
 * in "repairing" mode.
 */
public class TestRepairingWriter
    extends BaseWriterTest
{
    /**
     * Test similar to the one in {@link TestSimpleWriter}.
     */
    public void testElements()
        throws IOException, XMLStreamException
    {
        StringWriter strw = new StringWriter();
        XMLStreamWriter w = getRepairingWriter(strw);
        final String URL_P1 = "http://p1.org";
        final String URL_P2 = "http://ns.p2.net/yeehaw.html";
        final String URL_DEF = "urn:default";

        final String TEXT = "  some text\n";

        w.writeStartDocument();

        /* Calling setPrefix() should be optional; but if we call it,
         * exceptation is that it does properly cause URL to be bound.
         */
        w.setPrefix("p1", URL_P1);
        w.writeStartElement(URL_P1, "test");

        w.writeStartElement("p2", "branch", URL_P2);

        // And then a dynamically created prefix...
        w.writeStartElement(URL_DEF, "leaf");

        w.writeCharacters(TEXT);

        w.writeEndElement(); // first leaf

        w.writeEmptyElement(URL_P1, "leaf"); // second leaf

        w.writeStartElement("", "third"); // may need dynamic NS too
        w.writeEndElement();

        w.writeEndElement(); // branch
        w.writeEndElement(); // root elem
        w.writeEndDocument();
        w.close();

        /* And then let's parse and verify it all:
         */

        XMLStreamReader sr = constructNsStreamReader(strw.toString());
        assertTokenType(START_DOCUMENT, sr.getEventType());

        // root element
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("test", sr.getLocalName());
        // ??? is writer obligated to honor the prefix suggestion
        assertEquals(URL_P1, sr.getNamespaceURI());
        /* note: can not really verify number of namespace bindings, since
         * writer should be in charge... and it may output extra bindings,
         * too (and use default ns or explicit ones etc)
         */
        
        // first branch:
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("branch", sr.getLocalName());
        assertEquals(URL_P2, sr.getNamespaceURI());

        // first leaf
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("leaf", sr.getLocalName());
        assertEquals(URL_DEF, sr.getNamespaceURI());

        assertTokenType(CHARACTERS, sr.next());
        assertEquals(TEXT, getAllText(sr));
        // not: getAllText ^^^ moves cursor!

        assertTokenType(END_ELEMENT, sr.getEventType());
        assertEquals("leaf", sr.getLocalName());
        assertEquals(URL_DEF, sr.getNamespaceURI());

        // another leaf:
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("leaf", sr.getLocalName());
        assertEquals(URL_P1, sr.getNamespaceURI());

        assertTokenType(END_ELEMENT, sr.next());
        assertEquals("leaf", sr.getLocalName());
        assertEquals(URL_P1, sr.getNamespaceURI());

        // "third"
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("third", sr.getLocalName());
        assertNoNsURI(sr);
        assertTokenType(END_ELEMENT, sr.next());
        assertEquals("third", sr.getLocalName());
        assertNoNsURI(sr);

        // (close) branch
        assertTokenType(END_ELEMENT, sr.next());
        assertEquals("branch", sr.getLocalName());
        assertEquals(URL_P2, sr.getNamespaceURI());

        // closing root element
        assertTokenType(END_ELEMENT, sr.next());
        assertEquals("test", sr.getLocalName());
        assertEquals(URL_P1, sr.getNamespaceURI());

        assertTokenType(END_DOCUMENT, sr.next());
    }
}
