package org.codehaus.stax.test.evt;

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
    public TestEventFactory(String name) {
        super(name);
    }

    public void testAttribute()
        throws XMLStreamException
    {
        XMLEventFactory f = getEventFactory();
        final String URI = "http://foo.com";

        QName attrName = new QName(URI, "attr", "ns");
        Attribute attr = f.createAttribute(attrName, "value");
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
        assertEquals(content, c.getText());
    }

    public void testDTD()
        throws XMLStreamException
    {
        XMLEventFactory f = getEventFactory();

        // !!! TBI
    }

    public void testEndDocument()
        throws XMLStreamException
    {
        XMLEventFactory f = getEventFactory();
        EndDocument ed = f.createEndDocument();

        // No properties -- as long as we got instance of right type, it's ok
    }

    public void testEndElement()
        throws XMLStreamException
    {
        XMLEventFactory f = getEventFactory();
        // !!! TBI
    }

    public void testEntityReference()
        throws XMLStreamException
    {
        XMLEventFactory f = getEventFactory();
        // !!! TBI
    }

    public void testIgnorableSpace()
        throws XMLStreamException
    {
        final String contents = "  \t  \n  ";
        XMLEventFactory f = getEventFactory();
        Characters c = f.createIgnorableSpace(contents);
        assertEquals(contents, c.getData());
        assertFalse(c.isCData());
        assertTrue(c.isIgnorableWhiteSpace());
        assertTrue(c.isWhiteSpace());
    }

    public void testNamespace()
        throws XMLStreamException
    {
        XMLEventFactory f = getEventFactory();
        // !!! TBI
    }

    public void testProcInstr()
        throws XMLStreamException
    {
        XMLEventFactory f = getEventFactory();
        // !!! TBI
    }

    public void testSpace()
        throws XMLStreamException
    {
        final String contents = "  \t  \n  ";
        XMLEventFactory f = getEventFactory();
        Characters c = f.createSpace(contents);
        assertEquals(contents, c.getData());
        assertFalse(c.isCData());
        assertFalse(c.isIgnorableWhiteSpace());
        assertTrue(c.isWhiteSpace());
    }

    public void testStartDocument()
        throws XMLStreamException
    {
        XMLEventFactory f = getEventFactory();
        // !!! TBI
    }

    public void testStartElement()
        throws XMLStreamException
    {
        XMLEventFactory f = getEventFactory();
        // !!! TBI
    }

    /*
    ////////////////////////////////////////
    // Private methods, tests
    ////////////////////////////////////////
     */
}

