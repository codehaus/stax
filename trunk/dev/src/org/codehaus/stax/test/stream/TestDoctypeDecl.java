package org.codehaus.stax.test.stream;

import javax.xml.stream.*;

/**
 * Unit test suite that tests handling of the DOCTYPE declaration.
 */
public class TestDoctypeDecl
    extends BaseStreamTest
{
    public TestDoctypeDecl(String name) {
        super(name);
    }

    /**
     * Method that verifies properties that should be active when
     * DTD is the current event.
     */
    public void testProperties()
        throws XMLStreamException
    {
        doTestProperties(false);
        doTestProperties(true);
    }

    public void testMinimalValidDecl() 
        throws XMLStreamException
    {
        doTestMinimalValid(false);
        doTestMinimalValid(true);
    }

    public void testSimpleValidDecl() 
        throws XMLStreamException
    {
        doTestSimpleValid(false);
        doTestSimpleValid(true);
    }

    public void testTypicalValid()
        throws XMLStreamException
    {
        doTestTypicalValid(false);
        doTestTypicalValid(true);
    }

    public void testSimpleInvalidDecl() 
        throws XMLStreamException
    {
        doTestSimpleInvalid(false);
        doTestSimpleInvalid(true);
    }

    /*
    ////////////////////////////////////////
    // Private methods, shared test code
    ////////////////////////////////////////
     */

    private void doTestProperties(boolean nsAware)
        throws XMLStreamException
    {
        final String PROP_TEST = 
            "<!DOCTYPE root [ <!-- comment --> ]><root />";

        XMLStreamReader sr = getReader(PROP_TEST, nsAware);
        assertEquals(DTD, sr.next());
        // Type info
        assertEquals(false, sr.isStartElement());
        assertEquals(false, sr.isEndElement());
        assertEquals(false, sr.isCharacters());
        assertEquals(false, sr.isWhiteSpace());

        // indirect type info
        assertEquals(false, sr.hasName());
        assertEquals(true, sr.hasText());

        /* Interesting; according to Javadocs, these 2 methods behave
         * nicely, ie. no exceptions even if they are not applicable...
         */
        assertNull(sr.getPrefix());
        assertNull(sr.getNamespaceURI());

        // And then let's check methods that should throw specific exception
        for (int i = 0; i < 4; ++i) {
            String method = "";

            try {
                Object result = null;
                switch (i) {
                case 0:
                    method = "getName";
                    result = sr.getName();
                    break;
                case 1:
                    method = "getLocalName";
                    result = sr.getLocalName();
                    break;
                case 2:
                    method = "getNamespaceCount";
                    result = new Integer(sr.getNamespaceCount());
                    break;
                case 3:
                    method = "getAttributeCount";
                    result = new Integer(sr.getAttributeCount());
                    break;
                }
                fail("Expected IllegalArgumentException, when calling "
                     +method+"() for COMMENT");
            } catch (IllegalStateException iae) {
                ; // good
            }
        }

        // Here let's only check it's not null or empty, not exact contents
        String str = sr.getText();
        assertNotNull(str);
        if (str.trim().length() == 0) {
            fail("Internal subset not available; StreamReader.getText() returned an empty String (after trim())");
        }

        /* Javadoc doesn't say anything about exceptions... so assumption
         * is, we should get null here:
         */
        assertNull(sr.getPITarget());
        assertNull(sr.getPIData());
    }

    private void doTestMinimalValid(boolean nsAware)
        throws XMLStreamException
    {
        final String VALID_TEST = "<!DOCTYPE root><root />";

        XMLStreamReader sr = getReader(VALID_TEST, nsAware);

        assertEquals(DTD, sr.next());
        assertEquals(START_ELEMENT, sr.next());
    }

    private void doTestSimpleValid(boolean nsAware)
        throws XMLStreamException
    {
        final String INT_SUBSET = "<!-- comment -->";
        final String VALID_TEST = 
            "<!DOCTYPE root [ "+INT_SUBSET+" ]><root />";

        XMLStreamReader sr = getReader(VALID_TEST, nsAware);

        assertEquals(DTD, sr.next());

        /* Now... exactly what should be returned is not quite clear.
         * Let's assume that leading/trailing white space may be truncated,
         * but that otherwise we'll get stuff back, with linefeeds
         * normalized?
         */
        String str = getAndVerifyText(sr).trim();

        /* 16-Aug-2004, TSa: Hmmh. Specs are bit confusing; in some places
         *   claiming it should be only the internal subset, in others that
         *   it should be the full DOCTYPE declaration production...
         */
        assertEquals(INT_SUBSET, str);
    }

    private void doTestTypicalValid(boolean nsAware)
        throws XMLStreamException
    {
        final String VALID_TEST = "<!DOCTYPE root [\n"
	    +"<!ELEMENT root ANY>\n"
	    +"<!ATTLIST root attr CDATA #IMPLIED>\n"
	    +"]>\n"
	    +"<root />";

        XMLStreamReader sr = getReader(VALID_TEST, nsAware);

        assertEquals(DTD, sr.next());
        assertEquals(START_ELEMENT, sr.next());
    }

    private void doTestSimpleInvalid(boolean nsAware)
        throws XMLStreamException
    {
        final String INVALID1 = "<!DOCTYPE>  <root />";
        streamThroughFailing(getReader(INVALID1, nsAware), 
                             "invalid DOCTYPE declaration (missing root element)");

        final String INVALID2 = "<!DOCTYPE root>  <fubar />";
        streamThroughFailing(getReader(INVALID2, nsAware),
                             "invalid DOCTYPE declaration (root element does not match)");

        final String INVALID3 = "<!DOCTYPE root SYSTEM  ><root />";
        streamThroughFailing(getReader(INVALID3, nsAware),
                             "invalid DOCTYPE declaration (missing SYSTEM identifier for DTD)");
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
        setCoalescing(f, false); // shouldn't matter
        setNamespaceAware(f, nsAware);
        setValidating(f, false);
        setSupportDTD(f, true);
        return constructStreamReader(f, contents);
    }
}
