package org.codehaus.stax.test.stream;

import javax.xml.stream.*;

/**
 * Unit test suite that tests handling of XML processing instructions (except
 * for the linefeed normalization which is tested elsewhere); mostly just what
 * properties is the stream reader returning when pointing to a comment.
 */
public class TestProcInstrRead
    extends BaseStreamTest
{
    public TestProcInstrRead(String name) {
        super(name);
    }

    /**
     * Method that checks properties of PROCESSING_INSTRUCTION
     * returned by the stream reader are correct according to StAX specs.
     */
    public void testProcInstrProperties()
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

    public void testSpaceHandling()
        throws XMLStreamException
    {
	String CONTENT_TEXT = "some   data ";
	String CONTENT = "   "+CONTENT_TEXT;
	String XML = "<?target   "+CONTENT+"?><root />";

        for (int i = 0; i < 3; ++i) {
            boolean ns = (i & 1) != 0;
            boolean dtd = (i & 2) != 0;
	    XMLStreamReader sr = getReader(XML, ns, dtd);
	    assertTokenType(PROCESSING_INSTRUCTION, sr.next());
	    assertEquals("target", sr.getPITarget());

	    String content = sr.getPIData();
	    // Is content exactly as expected?
	    if (!content.equals(CONTENT_TEXT)) {
		// Nope... but would it be without white space?
		if (CONTENT_TEXT.trim().equals(content.trim())) {
		    fail("Proc. instr. white space handling not correct: expected data '"
			 +CONTENT_TEXT+"', got '"+content+"'");
		}
		// Nah, totally wrong:
		fail("Processing instruction data incorrect: expected '"
		     +CONTENT_TEXT+"', got '"+content+"'");
	    }

	}
    }

    public void testInvalidProcInstr()
        throws XMLStreamException
    {
        String XML = "<?xMl Can not use that target!  ?><root />";
        String XML2 = "<?   ?>   <root />";
        String XML3 = "<root><?target data   ></root>";

        for (int i = 0; i < 3; ++i) {
            boolean ns = (i & 1) != 0;
            boolean dtd = (i & 2) != 0;

            streamThroughFailing(getReader(XML, ns, dtd),
                                 "invalid processing instruction target ('xml' [case-insensitive] not legal) [ns: "+ns+", dtd: "+dtd+"]");

            streamThroughFailing(getReader(XML2, ns, dtd),
                                 "invalid processing instruction; empty proc. instr (missing target)");

            streamThroughFailing(getReader(XML3, ns, dtd),
                                 "invalid processing instruction; ends with '?', not \"?>\"");
        }
    }

    public void testUnfinishedPI()
        throws XMLStreamException
    {
        String XML = "<root><!? target data     </root>";

        for (int i = 0; i < 3; ++i) {
            boolean ns = (i & 1) != 0;
            streamThroughFailing(getReader(XML, ns, true),
                                 "invalid proc. instr. (unfinished)");
        }
    }

    /**
     * This unit test checks that the parser does not allow split processing
     * instructions; ones that start from within an entity expansion, but do
     * not completely finish within entity expansion, but in the original
     * input source that referenced the entity.
     * Such markup is illegal according to XML specs.
     */
    public void testRunawayProcInstr()
        throws XMLStreamException
    {
        String XML = "<!DOCTYPE root [\n"
            + "<!ENTITY pi '<?target d'>\n"
            +"]>\n"
            + "<root>&pi;?></root>";

        streamThroughFailing(getReader(XML, true, true),
                             "split/runaway proc. instr.");
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
    }

    /*
    ////////////////////////////////////////
    // Private methods, shared test code
    ////////////////////////////////////////
     */

    private void doTestProperties(boolean ns, boolean dtd)
        throws XMLStreamException
    {
        final String DATA = "data & more data (???) <>";
        XMLStreamReader sr = getReader("<?target "+DATA+" ?><root />", ns, dtd);

        assertEquals(PROCESSING_INSTRUCTION, sr.next());

        // Type info
        assertEquals(false, sr.isStartElement());
        assertEquals(false, sr.isEndElement());
        assertEquals(false, sr.isCharacters());
        assertEquals(false, sr.isWhiteSpace());

        // indirect type info
        assertFalse("Processing instructions have no names; XMLStreamReader.hasName() should return false", sr.hasName());
        assertEquals(false, sr.hasText());

        assertNotNull(sr.getLocation());
        if (ns) {
            assertNotNull(sr.getNamespaceContext());
        }

        /* Interesting; according to Javadocs, these 2 methods behave
         * nicely, ie. no exceptions even if they are not applicable...
         */
        assertNull(sr.getPrefix());
        assertNull(sr.getNamespaceURI());

        // And then let's check methods that should throw specific exception
        for (int i = 0; i < 8; ++i) {
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
                case 4:
                    method = "getText";
                    result = sr.getText();
                    break;
                case 5:
                    method = "getTextCharacters";
                    result = sr.getTextCharacters();
                    break;
                case 6:
                    method = "getTextStart";
                    result = new Integer(sr.getTextStart());
                    break;
                case 7:
                    method = "getTextLength";
                    result = new Integer(sr.getTextLength());
                    break;
                }
                fail("Expected IllegalStateException, when calling "
                     +method+"() for PROCESSING_INSTRUCTION");
            } catch (IllegalStateException iae) {
                ; // good
            }
        }

        assertEquals("target", sr.getPITarget());

        /* Now; specs are bit vague WRT white space handling between target
         * and data; thus, let's just trim trailing/leading white space
         */
	/* 13-Nov-2004, TSa: Actually, handling is to get rid
	 *  of leading but not trailing white space, as per XML specs.
	 *  StAX API is not clear, but another test will verify proper
	 *  behaviour.
	 */
        assertEquals(DATA.trim(), sr.getPIData().trim());
    }

    /*
    ////////////////////////////////////////
    // Private methods, other
    ////////////////////////////////////////
     */

    private XMLStreamReader getReader(String contents, boolean nsAware,
                                      boolean supportDTD)
        throws XMLStreamException
    {
        XMLInputFactory f = getInputFactory();
        setCoalescing(f, false); // shouldn't really matter
        setNamespaceAware(f, nsAware);
        setSupportDTD(f, supportDTD);
        setValidating(f, false);
        return constructStreamReader(f, contents);
    }
}
