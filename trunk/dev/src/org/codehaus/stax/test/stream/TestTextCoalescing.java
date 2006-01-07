package org.codehaus.stax.test.stream;

import javax.xml.stream.*;

/**
 * Unit test suite that tests that the stream reader does in fact
 * coalesce adjacent text/CDATA segments when told to do so.
 */
public class TestTextCoalescing
    extends BaseStreamTest
{
    final static String VALID_XML = "<root>Text <![CDATA[cdata\n"
        +"in two lines]]><![CDATA[!]]>/that's all!</root>";

    public TestTextCoalescing(String name) {
        super(name);
    }

    public void testCoalescing()
        throws XMLStreamException
    {
        XMLStreamReader sr = getReader(VALID_XML, true, true);
        assertTokenType(START_ELEMENT, sr.next());
        assertTokenType(CHARACTERS, sr.next());

        String act = getAndVerifyText(sr);
        String exp = "Text cdata\nin two lines!/that's all!";

        if (!exp.equals(act)) {
            failStrings("Coalescing failed", exp, act);
        }

        assertTokenType(END_ELEMENT, sr.next());
        assertTokenType(END_DOCUMENT, sr.next());
    }

    /**
     * Test that ensures that even when just skipping (ie not accessing
     * any data), we'll still see just one event for the whole text
     */
    public void testCoalescingSkipping()
        throws XMLStreamException
    {
        XMLStreamReader sr = getReader(VALID_XML, true, true);
        assertTokenType(START_ELEMENT, sr.next());
        assertTokenType(CHARACTERS, sr.next());
        assertTokenType(END_ELEMENT, sr.next());
        assertTokenType(END_DOCUMENT, sr.next());
    }

    public void testNonCoalescing()
        throws XMLStreamException
    {
        XMLStreamReader sr = getReader(VALID_XML, true, false);
        assertTokenType(START_ELEMENT, sr.next());

        /* Can not assume that all events are returned in one call...
         * so let's play it safe:
         */
        sr.next();
        // still, it shouldn't coalesce them:
        assertEquals("Text ", getAndVerifyText(sr));
        sr.next();
        // Hmmmh. Worse; stream readers can return CHARACTERS or CDATA...
        int count = checkText(sr, "cdata\nin two lines!/that's all!");
        if (count < 3) {
            // Can't easily check boundaries... well, could but...
            fail("Expected at least four CDATA/CHARACTERS events; parser coalesced them (only got "+(count-1)+")");
        }
        assertTokenType(END_ELEMENT, sr.getEventType());
        assertTokenType(END_DOCUMENT, sr.next());
    }

    public void testNonCoalescingSkipping()
        throws XMLStreamException
    {
        XMLStreamReader sr = getReader(VALID_XML, true, false);
        assertTokenType(START_ELEMENT, sr.next());

        assertTokenType(CHARACTERS, sr.next());

        // Now, we may get more than one CHARACTERS
        int type = sr.next();
        /* 06-Jan-2006, TSa: Ugh. Due to CDATA -> CHARACTERS remapping
         *   some readers too, can't check too many things...
         */
        int count = 1;
        while (type == CHARACTERS || type == CDATA) {
            type = sr.next();
            ++count;
        }
        if (count < 4) {
            fail("Expected 4 or more separate CHARACTERS/CDATA events in non-coalescing mode, got "+count);
        }

        assertTokenType(END_ELEMENT, type);
        assertTokenType(END_DOCUMENT, sr.next());
    }

    public void testInvalidTextWithCDataEndMarker()
        throws XMLStreamException
    {
        String XML = "<root>   ]]>   </root>";

        streamThroughFailing(getReader(XML, true, true),
                             "text content that has ']]>' in it.");
    }

    /*
    ////////////////////////////////////////
    // Private methods, other
    ////////////////////////////////////////
     */

    private XMLStreamReader getReader(String contents, boolean nsAware,
                                      boolean coalescing)
        throws XMLStreamException
    {
        XMLInputFactory f = getInputFactory();
        setNamespaceAware(f, nsAware);
        setSupportDTD(f, true);
        setCoalescing(f, coalescing);
        setReplaceEntities(f, true);
        setValidating(f, false);
        return constructStreamReader(f, contents);
    }

    private int checkText(XMLStreamReader sr, String exp)
        throws XMLStreamException
    {
        int currType = sr.getEventType();
        if (currType == CDATA || currType == CHARACTERS) {
            ;
        } else {
            assertTokenType(CDATA, currType);
        }
        StringBuffer sb = new StringBuffer(getAndVerifyText(sr));
        int count = 1;
        while (true) {
            currType = sr.next();
            if (currType != CDATA && currType != CHARACTERS) {
                break;
            }
            ++count;
            sb.append(getAndVerifyText(sr));
        }
        String act = sb.toString();
        if (!exp.equals(act)) {
            failStrings("Incorrect text contents", act, exp);
        }
        return count;
    }
}
