package org.codehaus.stax.test.stream;

import javax.xml.stream.*;

/**
 * Unit test suite that tests handling of XML comments (except for the
 * linefeed normalization which is tested elsewhere); mostly just what
 * properties is the stream reader returning when pointing to a comment.
 */
public class TestCommentRead
    extends BaseStreamTest
{
    public TestCommentRead(String name) {
        super(name);
    }

    public void testValidComments()
        throws XMLStreamException
    {
        String XML = "<!-- <comment> --><root />  ";
        streamThrough(getReader(XML, true, true));
        streamThrough(getReader(XML, false, true));
        XML = "  <root>  </root>  <!-- hee&haw - - -->";
        streamThrough(getReader(XML, true, true));
        streamThrough(getReader(XML, false, true));
    }

    /**
     * Method that checks properties of COMMENT
     * returned by the stream reader are correct according to StAX specs.
     */
    public void testCommentProperties()
        throws XMLStreamException
    {
        /* Neither ns-awareness nor dtd-support should make any differnece,
         * but let's double check them...
         */
        doTestProperties(true, true);
        doTestProperties(true, false);
        doTestProperties(false, true);
        doTestProperties(false, false);
    }

    public void testInvalidComment()
        throws XMLStreamException
    {
        String XML = "<!-- Can not have '--' in here! -->  <root />";

        for (int i = 0; i < 3; ++i) {
            boolean ns = (i & 1) != 0;
            boolean dtd = (i & 2) != 0;

            streamThroughFailing(getReader(XML, ns, dtd),
                                 "invalid comment content (embedded \"--\")");
        }
    }

    public void testUnfinishedComment()
        throws XMLStreamException
    {
        String XML = "<root><!-- Comment that won't end </root>";

        for (int i = 0; i < 3; ++i) {
            boolean ns = (i & 1) != 0;
            boolean dtd = (i & 2) != 0;

            streamThroughFailing(getReader(XML, ns, dtd),
                                 "invalid comment (unfinished)");
        }
    }

    /**
     * This unit test checks that the parser does not allow split comments;
     * comments that start from within an entity expansion, but have
     * no matching close marker until in the context that had the entity
     * reference.
     */
    public void testRunawayComment()
        throws XMLStreamException
    {
        String XML = "<!DOCTYPE root [\n"
            + "<!ENTITY comm '<!-- start'>\n"
            +"]>\n"
            + "<root>&comm;   --></root>";

        XMLStreamReader sr = getReader(XML, true, true);
        try {
            streamThrough(sr);
              // Uncomment for debugging (and comment out previous line)
            /*
            while (sr.hasNext()) {
                int type = sr.next();
                System.err.println("PI, type -> "+tokenTypeDesc(type));
                if (sr.hasText()) {
                    System.err.println("  ["+sr.getTextLength()+" c]");
                }
            }
            */
            fail("Expected an exception for split/runaway comment");
        } catch (Exception e) { }
    }

    /*
    ////////////////////////////////////////
    // Private methods, shared test code
    ////////////////////////////////////////
     */

    private void doTestProperties(boolean nsAware, boolean dtd)
        throws XMLStreamException
    {
        XMLStreamReader sr = getReader("<!--comment & <content>--><root/>",
                                       nsAware, dtd);
        assertEquals(COMMENT, sr.next());

        // Type info
        assertEquals(false, sr.isStartElement());
        assertEquals(false, sr.isEndElement());
        assertEquals(false, sr.isCharacters());
        assertEquals(false, sr.isWhiteSpace());

        // indirect type info
        assertEquals(false, sr.hasName());
        assertEquals(true, sr.hasText());

        assertNotNull(sr.getLocation());
        if (nsAware) {
            assertNotNull(sr.getNamespaceContext());
        }

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
                fail("Expected IllegalStateException, when calling "
                     +method+"() for COMMENT");
            } catch (IllegalStateException iae) {
                ; // good
            }
        }

        /* StAX JavaDocs just say 'Proc. instr. target/data, or null', NOT
         * that there should be an exception...
         */
        assertNull(sr.getPITarget());
        assertNull(sr.getPIData());

        String content = getAndVerifyText(sr);
        assertEquals("comment & <content>", content);
    }

    /*
    ////////////////////////////////////////
    // Private methods, other
    ////////////////////////////////////////
     */

    private XMLStreamReader getReader(String contents, boolean nsAware,
                                      boolean dtd)
        throws XMLStreamException
    {
        XMLInputFactory f = getInputFactory();
        setCoalescing(f, false); // shouldn't really matter
        setNamespaceAware(f, nsAware);
        setSupportDTD(f, dtd);
        setValidating(f, false);
        return constructStreamReader(f, contents);
    }
}
