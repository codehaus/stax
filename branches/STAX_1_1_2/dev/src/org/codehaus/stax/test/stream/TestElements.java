package org.codehaus.stax.test.stream;

import javax.xml.namespace.*;
import javax.xml.stream.*;

/**
 * Unit test suite that tests handling of XML elements, both in namespace
 * aware and non-namespace modes.
 */
public class TestElements
    extends BaseStreamTest
{
    public TestElements(String name) {
        super(name);
    }

    /**
     * Method that checks properties of START_ELEMENT and END_ELEMENT
     * returned by the stream reader are correct according to StAX specs.
     */
    public void testNsProperties()
        throws XMLStreamException
    {
        testProperties(true);
    }

    public void testNonNsProperties()
        throws XMLStreamException
    {
        testProperties(false);
    }

    /**
     * Does test for simple element structure in namespace aware mode
     */
    public void testValidNsElems()
        throws XMLStreamException
    {
        testValid(true);
    }

    public void testValidNonNsElems()
        throws XMLStreamException
    {
        testValid(false);
    }

    public void testInvalidNsElems()
        throws XMLStreamException
    {
        testInvalid(true);
    }

    public void testInvalidNonNsElems()
        throws XMLStreamException
    {
        testInvalid(false);
    }

    public void testEmptyDocument()
        throws XMLStreamException
    {
        String EMPTY_XML = "   ";

        // Empty documents are not valid (missing root element)

        streamThroughFailing(getElemReader(EMPTY_XML, false),
                             "empty document (not valid, missing root element)");
        streamThroughFailing(getElemReader(EMPTY_XML, true),
                             "empty document (not valid, missing root element)");
    }

    public void testNoRootDocument()
        throws XMLStreamException
    {
        String EMPTY_XML = "<?xml version='1.0' ?>\n"
            +"   <!-- comment...-->   <?target !?>";

        // Documents without root are not valid

        streamThroughFailing(getElemReader(EMPTY_XML, false),
                             "document without root element");
        streamThroughFailing(getElemReader(EMPTY_XML, true),
                             "document without root element");
    }

    public void testInvalidEmptyElem()
        throws XMLStreamException
    {
        String XML = "<root>   <elem / ></root>";
        String MSG = "malformed empty element (space between '/' and '>')";

        streamThroughFailing(getElemReader(XML, false), MSG);
        streamThroughFailing(getElemReader(XML, true), MSG);
    }

    /*
    ////////////////////////////////////////
    // Private methods, shared test code
    ////////////////////////////////////////
     */

    private void testProperties(boolean nsAware)
        throws XMLStreamException
    {
        XMLStreamReader sr = getElemReader("<root />", nsAware);
        final String EXPECTED_EMPTY_URI = nsAware ?
            DEFAULT_URI_NS : DEFAULT_URI_NON_NS;

        assertEquals(START_ELEMENT, sr.next());
        testStartOrEnd(nsAware, sr, true);

        assertEquals(END_ELEMENT, sr.next());
        testStartOrEnd(nsAware, sr, false);
    }

    private void testStartOrEnd(boolean nsAware, XMLStreamReader sr,
                                boolean isStart)
        throws XMLStreamException
    {
        assertEquals(isStart ? START_ELEMENT : END_ELEMENT, sr.getEventType());

        // simple type info
        assertEquals(isStart, sr.isStartElement());
        assertEquals(!isStart, sr.isEndElement());
        assertEquals(false, sr.isCharacters());
        assertEquals(false, sr.isWhiteSpace());

        // indirect type info
        assertEquals(true, sr.hasName());
        assertEquals(false, sr.hasText());

        assertNotNull(sr.getLocation());
        QName n = sr.getName();
        assertNotNull(n);
        assertEquals("root", n.getLocalPart());
        /* Hmmh. Seems like QName won't return null no matter what...
         * so let's just check it's null or empty
         */
        //assertEquals(null, n.getPrefix());
        {
            String prefix = n.getPrefix();
            assertTrue((prefix == null) || prefix.length() == 0);
        }

        // Similarly, ns URI apparently is never null...
        //assertEquals((nsAware ? DEFAULT_URI_NS : DEFAULT_URI_NON_NS), n.getNamespaceURI());
        {
            String uri = n.getNamespaceURI();
            assertTrue((uri == null) || uri.length() == 0);
        }

        if (isStart) {
            assertEquals(0, sr.getAttributeCount());
        } else {
            try {
                int count = sr.getAttributeCount();
                fail("Expected an IllegalStateException when trying to call getAttributeCount() for END_ELEMENT");
            } catch (IllegalStateException e) {
                // good
            }
        }
        assertEquals(0, sr.getNamespaceCount());
        if (nsAware) {
            /* but how about if namespaces are not supported? Can/should
             * it return null?
             */
            assertNotNull(sr.getNamespaceContext());
        }
        /* StAX JavaDocs just say 'Proc. instr. target/data, or null', NOT
         * that there should be an exception...
         */
        assertNull(sr.getPITarget());
        assertNull(sr.getPIData());

        try {
            String str = sr.getText();
            fail("Expected an IllegalStateException when trying to call getText() for START_ELEMENT");
        } catch (IllegalStateException e) {
            // good
        }

        try {
            char[] c = sr.getTextCharacters();
            fail("Expected an IllegalStateException when trying to call getTextCharacters() for START_ELEMENT");
        } catch (IllegalStateException e) {
            // good
        }
    }

    private void testValid(boolean nsAware)
        throws XMLStreamException
    {
        final String NS_URL1 = "http://www.stax.org";
        final String NS_PREFIX1 = "prefix1";
        
        final String NS_URL2 = "urn://mydummyid";
        final String NS_PREFIX2 = "prefix2";

        final String VALID_CONTENT
            = "<root><"+NS_PREFIX1+":elem xmlns:"+NS_PREFIX1
            +"='"+NS_URL1+"' "+NS_PREFIX1+":attr='value'>Text"
            +"</"+NS_PREFIX1+":elem>"
            +"<elem2 xmlns='"+NS_URL2+"' attr='value' /></root>";

        /* First of all, let's check that it can be completely
         * parsed:
         */
        streamThrough(getElemReader(VALID_CONTENT, nsAware));

        // And then let's do it step by step
        XMLStreamReader sr = getElemReader(VALID_CONTENT, nsAware);
        final String EXPECTED_EMPTY_URI = nsAware ?
            DEFAULT_URI_NS : DEFAULT_URI_NON_NS;

        // First, need to get <root>
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        String prefix = sr.getPrefix();
        assertNull("Missing prefix should be reported as null", prefix);
        String nsURI = sr.getNamespaceURI();

        /* Hmmh. It's not defined by StAX API, whether null or "" is expected
         * in non-ns mode... so let's accept either:
         */
        if (nsAware) {
            // In NS-mode, null is not allowed however
            assertEquals("Default (non-defined) namespace should be reported as empty String", "", nsURI);
        } else {
            if (nsURI != null && nsURI.length() > 0) {
                fail("Default (non-defined) namespace should be reported either as NULL, or as empty String, in non-NS mode");
            }
        }

        // Let's also check QName seems valid:
        QName name = sr.getName();
        assertNotNull("Shouldn't get null QName for any start element", name);
        assertEquals(name, new QName("root"));

        // Hmmh. In ns-aware mode, is it ok to get null, ever?
        assertEquals(EXPECTED_EMPTY_URI, sr.getNamespaceURI());
        assertEquals(0, sr.getAttributeCount());
        assertEquals(0, sr.getNamespaceCount());

        // And then <elem ...>
        assertEquals(START_ELEMENT, sr.next());
        if (nsAware) {
            assertEquals("elem", sr.getLocalName());
            assertEquals(NS_PREFIX1, sr.getPrefix());
            assertEquals(NS_URL1, sr.getNamespaceURI());
        } else {
            assertEquals(NS_PREFIX1+":elem", sr.getLocalName());
            assertEquals(null, sr.getPrefix());
            assertEquals(DEFAULT_URI_NON_NS, sr.getNamespaceURI());
        }

        int expNs = nsAware ? 1 : 0;
        int expAttr = nsAware ? 1 : 2;

        /* Let's just check counts, not values; attribute test can
         * do thorough tests for values and access
         */
        assertEquals(expAttr, sr.getAttributeCount());
        assertEquals(expNs, sr.getNamespaceCount());

        assertEquals(CHARACTERS, sr.next());
        assertEquals("Text", getAndVerifyText(sr));

        assertEquals(END_ELEMENT, sr.next());
        if (nsAware) {
            assertEquals("elem", sr.getLocalName());
            assertEquals(NS_PREFIX1, sr.getPrefix());
            assertEquals(NS_URL1, sr.getNamespaceURI());
        } else {
            assertEquals(NS_PREFIX1+":elem", sr.getLocalName());
            assertEquals(null, sr.getPrefix());
            assertEquals(DEFAULT_URI_NON_NS, sr.getNamespaceURI());
        }
        assertEquals(expNs, sr.getNamespaceCount());

        assertEquals(START_ELEMENT, sr.next());
        assertEquals("elem2", sr.getLocalName());

        assertEquals(null, sr.getPrefix());
        if (nsAware) {
            assertEquals(NS_URL2, sr.getNamespaceURI());
        } else {
            assertEquals(DEFAULT_URI_NON_NS, sr.getNamespaceURI());
        }
        assertEquals(expAttr, sr.getAttributeCount());
        assertEquals(expNs, sr.getNamespaceCount());

        assertEquals(END_ELEMENT, sr.next());
        assertEquals("elem2", sr.getLocalName());

        assertEquals(null, sr.getPrefix());
        if (nsAware) {
            assertEquals(NS_URL2, sr.getNamespaceURI());
        } else {
            assertEquals(DEFAULT_URI_NON_NS, sr.getNamespaceURI());
        }
        assertEquals(expNs, sr.getNamespaceCount());

        assertEquals(END_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        assertEquals(null, sr.getPrefix());

        assertEquals(EXPECTED_EMPTY_URI, sr.getNamespaceURI());
        assertEquals(0, sr.getNamespaceCount());
    }

    /**
     * Simple tests to check for incorrect nesting
     */
    private void testInvalid(boolean nsAware)
        throws XMLStreamException
    {
        // Wrong end element:
        String XML = "<root>  text </notroot>";
        streamThroughFailing(getElemReader(XML, nsAware),
                             "incorrect nesting (wrong end element name)");

        // Missing end element:
        XML = "<root><branch>  text </branch>";
        streamThroughFailing(getElemReader(XML, nsAware),
                             "incorrect nesting (missing end element)");

        // More than one root:
        XML = "<root /><anotherRoot />";
        streamThroughFailing(getElemReader(XML, nsAware),
                             "more than one root element");
    }

    /*
    ////////////////////////////////////////
    // Private methods, other
    ////////////////////////////////////////
     */

    private XMLStreamReader getElemReader(String contents, boolean nsAware)
        throws XMLStreamException
    {
        XMLInputFactory f = getInputFactory();
        setCoalescing(f, true);
        setNamespaceAware(f, nsAware);
        setValidating(f, false);
        return constructStreamReader(f, contents);
    }
}