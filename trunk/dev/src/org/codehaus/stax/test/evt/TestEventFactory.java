package org.codehaus.stax.test.evt;

import java.io.StringWriter;

import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.*;

/**
 * Class that contains simple tests for making sure that event objets
 * created by the {@link XMLEventFactory} have expected properties.
 */
public class TestEventFactory
    extends BaseEventTest
{
    public void testAttribute()
        throws XMLStreamException
    {
        XMLEventFactory f = getEventFactory();
        final String URI = "http://foo.com";

        QName attrName = new QName(URI, "attr", "ns");
        Attribute attr = f.createAttribute(attrName, "value");

        checkIsMethods(ATTRIBUTE, attr);
        testWritability(attr);

        Attribute attr2 = f.createAttribute("ns", URI, "attr", "value'2'");
        Attribute attr3 = f.createAttribute("attr", "this&more");

        /* No way to associate with a DTD, should have fairly basic
         * settings:
         */
        assertEquals("CDATA", attr.getDTDType());
        assertEquals("CDATA", attr2.getDTDType());
        assertEquals("CDATA", attr3.getDTDType());
        assertTrue("Attribute 'ns:attr' should be created as 'defined'",
                   attr.isSpecified());
        assertTrue("Attribute 'ns:attr' should be created as 'defined'", 
                   attr2.isSpecified());
        assertTrue("Attribute 'attr' should be created as 'defined'", 
                   attr3.isSpecified());
        assertEquals("value", attr.getValue());
        assertEquals("value'2'", attr2.getValue());
        assertEquals("this&more", attr3.getValue());

        // Ok, then names...
        assertEquals(attrName, attr.getName());
        assertEquals(attrName, attr2.getName());

        QName name3 = attr3.getName();
        /* Alas, QName doesn't seem to retain nulls... so let's
         * be bit more lenient here:
         */
        assertEquals("attr", name3.getLocalPart());
        String str = name3.getPrefix();
        assertTrue(str == null || str.length() == 0);
        str = name3.getNamespaceURI();
        assertTrue(str == null || str.length() == 0);
    }

    public void testCData()
        throws XMLStreamException
    {
        final String contents = "test <some> text & more! [[]] --";
        XMLEventFactory f = getEventFactory();
        Characters c = f.createCData(contents);
        checkIsMethods(CHARACTERS, c);
        testWritability(c);

        assertEquals(contents, c.getData());
        assertTrue(c.isCData());
        assertFalse(c.isIgnorableWhiteSpace());
        assertFalse(c.isWhiteSpace());
    }

    public void testCharacters()
        throws XMLStreamException
    {
        final String contents = "test <some> text & more! [[]] --";
        XMLEventFactory f = getEventFactory();
        Characters c = f.createCharacters(contents);

        checkIsMethods(CHARACTERS, c);
        testWritability(c);

        assertEquals(contents, c.getData());
        assertFalse(c.isCData());
        assertFalse(c.isIgnorableWhiteSpace());
        assertFalse(c.isWhiteSpace());
    }

    public void testComment()
        throws XMLStreamException
    {
        final String content = "Comment - how interesting!";

        XMLEventFactory f = getEventFactory();
        Comment c = f.createComment(content);

        checkIsMethods(COMMENT, c);
        testWritability(c);

        assertEquals(content, c.getText());
    }

    public void testDTD()
        throws XMLStreamException
    {
        XMLEventFactory f = getEventFactory();
        DTD d = f.createDTD("<!DOCTYPE root SYSTEM 'http://foo' [ ]>");

        checkIsMethods(DTD, d);
        testWritability(d);

        // !!! TBI
    }

    public void testEndDocument()
        throws XMLStreamException
    {
        XMLEventFactory f = getEventFactory();
        EndDocument ed = f.createEndDocument();

        // No properties -- as long as we got instance of right type, it's ok
        checkIsMethods(END_DOCUMENT, ed);
        testWritability(ed);
    }

    public void testEndElement()
        throws XMLStreamException
    {
        XMLEventFactory f = getEventFactory();
        final String LOCALNAME = "elem";

        // prefix, uri, localName
        EndElement ee = f.createEndElement("", "", LOCALNAME);
        checkIsMethods(END_ELEMENT, ee);
        testWritability(ee);

        QName n = ee.getName();
        assertNotNull(n);
        assertEquals(LOCALNAME, n.getLocalPart());
    }

    public void testEntityReference()
        throws XMLStreamException
    {
        XMLEventFactory f = getEventFactory();

        /* 22-Dec-2005, TSa: ... but how can we create the entity declaration
         *   that is needed? Should null be ok? For now, can't really test...
         */
        //EntityReference ref = f.createEntityReference("ref", decl);
        //checkIsMethods(ENTITY_REFERENCE, ref);
        //testWritability(ref);
    }

    public void testIgnorableSpace()
        throws XMLStreamException
    {
        final String contents = "  \t  \n  ";
        XMLEventFactory f = getEventFactory();
        Characters c = f.createIgnorableSpace(contents);

        checkIsMethods(CHARACTERS, c);
        testWritability(c);

        assertEquals(contents, c.getData());
        assertFalse(c.isCData());
        assertTrue(c.isIgnorableWhiteSpace());
        assertTrue(c.isWhiteSpace());
    }

    public void testNamespace()
        throws XMLStreamException
    {
        XMLEventFactory f = getEventFactory();
        final String PREFIX = "prefix";
        final String URI = "http://foo";

        // First default:
        Namespace ns = f.createNamespace(URI);

        checkIsMethods(NAMESPACE, ns);
        testWritability(ns);

        String prefix = ns.getPrefix();
        // Both null and empty are ok?
        if (prefix != null && prefix.length() > 0) {
            fail("Expected prefix to be null or empty for default namespace event object");
        }
        assertEquals(URI, ns.getNamespaceURI());
        assertTrue(ns.isDefaultNamespaceDeclaration());

        // Then non-default:
        ns = f.createNamespace(PREFIX, URI);
        checkIsMethods(NAMESPACE, ns);
        assertEquals(PREFIX, ns.getPrefix());
        assertEquals(URI, ns.getNamespaceURI());
        assertFalse(ns.isDefaultNamespaceDeclaration());
    }

    public void testProcInstr()
        throws XMLStreamException
    {
        XMLEventFactory f = getEventFactory();
        ProcessingInstruction pi = f.createProcessingInstruction("target", "data");
        checkIsMethods(PROCESSING_INSTRUCTION, pi);
        testWritability(pi);

        assertEquals("target", pi.getTarget());
        assertEquals("data", pi.getData());

    }

    public void testSpace()
        throws XMLStreamException
    {
        final String contents = "  \t  \n  ";
        XMLEventFactory f = getEventFactory();
        Characters c = f.createSpace(contents);
        assertEquals(contents, c.getData());

        checkIsMethods(CHARACTERS, c);
        testWritability(c);

        assertFalse(c.isCData());
        assertFalse(c.isIgnorableWhiteSpace());
        assertTrue(c.isWhiteSpace());
    }

    public void testStartDocument()
        throws XMLStreamException
    {
        XMLEventFactory f = getEventFactory();
        StartDocument sd = f.createStartDocument();
        checkIsMethods(START_DOCUMENT, sd);
        testWritability(sd);

        assertFalse(sd.encodingSet());
        assertFalse(sd.standaloneSet());

        final String ENCODING = "ISO-8859-1";
        final String VERSION = "1.0";
        sd = f.createStartDocument(ENCODING, VERSION, true);
        checkIsMethods(START_DOCUMENT, sd);
        assertTrue(sd.encodingSet());
        assertTrue(sd.standaloneSet());
        assertEquals(ENCODING, sd.getCharacterEncodingScheme());
        assertEquals(VERSION, sd.getVersion());
    }

    public void testStartElement()
        throws XMLStreamException
    {
        final String LOCALNAME = "root";
        final String PREFIX = "ns";
        final String URI = "urn:whatever";

        XMLEventFactory f = getEventFactory();
        // prefix, uri, localname
        StartElement se = f.createStartElement("", "", LOCALNAME);
        testWritability(se);

        checkIsMethods(START_ELEMENT, se);
        QName n = se.getName();
        assertNotNull(n);
        assertEquals(LOCALNAME, n.getLocalPart());

        se = f.createStartElement(PREFIX, URI, LOCALNAME);
        checkIsMethods(START_ELEMENT, se);
        n = se.getName();
        assertNotNull(n);
        assertEquals(LOCALNAME, n.getLocalPart());
        assertEquals(PREFIX, n.getPrefix());
        assertEquals(URI, n.getNamespaceURI());
    }

    /*
    ////////////////////////////////////////
    // Private methods, tests
    ////////////////////////////////////////
     */

    private void checkIsMethods(int type, XMLEvent evt)
    {
        int actualType = evt.getEventType();
        if (actualType != type) {
            /* Minor deviation; should Characters objects that are constructed
             * for CDATA and SPACE return true type or CHARACTERS?
             */
            if (type == CHARACTERS &&
                (actualType == SPACE || actualType == CDATA)) {
                // for now let's let this pass...
            } else {
                assertTokenType(type, actualType); // this'll fail and output descs for types
            }
        }

        /* Hmmh. Whether Namespace object should return true or false
         * is an open question. So let's accept both
         */
        if (type == NAMESPACE) {
            /* for now let's just ask for it (to make sure it won't throw
             * exceptions), but not verify the value
             */
            boolean isAttr = evt.isAttribute();
        } else {
            assertEquals((type == ATTRIBUTE), evt.isAttribute());
        }

        assertEquals((type == CHARACTERS), evt.isCharacters());
        assertEquals((type == START_DOCUMENT), evt.isStartDocument());
        assertEquals((type == END_DOCUMENT), evt.isEndDocument());
        assertEquals((type == START_ELEMENT), evt.isStartElement());
        assertEquals((type == END_ELEMENT), evt.isEndElement());
        assertEquals((type == ENTITY_REFERENCE), evt.isEntityReference());
        assertEquals((type == NAMESPACE), evt.isNamespace());
        assertEquals((type == PROCESSING_INSTRUCTION), evt.isProcessingInstruction());
    }

    /**
     * Simple test utility method that just calls output method, to verify
     * it does not throw anything nasty, and does output something.
     * Not enough to verify actual working, but should exercise code path
     * to check for fatal problems.
     */
    private void testWritability(XMLEvent evt)
        throws XMLStreamException
    {
        StringWriter sw = new StringWriter();
        evt.writeAsEncodedUnicode(sw);

        // Some events do not (have to) output anything:
        switch (evt.getEventType()) {
        case END_DOCUMENT: // nothing to output, usually
            return;
        }

        assertTrue(sw.toString().length() > 0);
    }
}

