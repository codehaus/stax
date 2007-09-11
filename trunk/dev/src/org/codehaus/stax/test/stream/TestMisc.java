package org.codehaus.stax.test.stream;

import javax.xml.stream.*;

/**
 * Unit test suite that tests miscallenous stream reader methods, such
 * as require(), getElementText() and nextTag()
 */
public class TestMisc
    extends BaseStreamTest
{
    public TestMisc(String name) {
        super(name);
    }

    public void testRequire()
        throws XMLStreamException
    {
        String XML =
            "<root>"
            +"<!-- comment --><?proc instr?>"
            +"<tag>Text</tag>"
            +"<tag><![CDATA[xx]]></tag>"
            +"<a:empty xmlns:a='http://foo' />"
            +"</root>"
            ;

        XMLStreamReader sr = getReader(XML, true, true);

        sr.require(START_DOCUMENT, null, null);

        assertTokenType(START_ELEMENT, sr.next());

        assertTokenType(COMMENT, sr.next());
        sr.require(COMMENT, null, null);
        try { // should get an exception due to comments not having names
            sr.require(COMMENT, null, "tag");
            fail("Should have thrown an exception when checking local name of a COMMENT");
        } catch (XMLStreamException e) {
            ; // good
        } catch (IllegalStateException ise) {
            ; // likewise this is ok, as getName() can throw it
        }

        assertTokenType(PROCESSING_INSTRUCTION, sr.next());
        sr.require(PROCESSING_INSTRUCTION, null, null);

        assertTokenType(START_ELEMENT, sr.next());
        sr.require(START_ELEMENT, null, "tag");

        try {
            sr.require(START_ELEMENT, "", "tag");
        } catch (XMLStreamException e) {
            fail("Did not expect problems with <tag> match, got: "+e.getMessage());
        }

        try { // should get an exception due to incorrect ns URI
            sr.require(START_ELEMENT, "http://foo", "tag");
            fail("Should have thrown an exception for incorrect NS URI");
        } catch (XMLStreamException e) {
            ; // good
        }

        assertTokenType(CHARACTERS, sr.next());
        sr.require(CHARACTERS, null, null);

        assertTokenType(END_ELEMENT, sr.next());
        sr.require(END_ELEMENT, null, "tag");
        sr.require(END_ELEMENT, "", "tag");

        assertTokenType(START_ELEMENT, sr.next());

        /* Will get CHARACTERS instead of CDATA, because it's a
         * coalescing reader...
         */
        assertTokenType(CHARACTERS, sr.next());
        sr.require(CHARACTERS, null, null);

        assertTokenType(END_ELEMENT, sr.next());

        assertTokenType(START_ELEMENT, sr.next()); // empty
        sr.require(START_ELEMENT, "http://foo", "empty");
        assertTokenType(END_ELEMENT, sr.next());
        sr.require(END_ELEMENT, "http://foo", "empty");
        sr.require(END_ELEMENT, "http://foo", null);
        sr.require(END_ELEMENT, null, "empty");

        assertTokenType(END_ELEMENT, sr.next());
        sr.require(END_ELEMENT, "", "root");

        assertTokenType(END_DOCUMENT, sr.next());
        sr.require(END_DOCUMENT, null, null);
    }

    public void testGetElementText()
        throws XMLStreamException
    {
        String XML =
            "<root>"
            +"<tag>Got some <!-- comment --> text &apos;n stuff!</tag>"
            +"<tag><?proc instr?>more <![CDATA[stuff]]> </tag>"
            +"<tag><empty /></tag>"
            +"</root>"
            ;
        XMLStreamReader sr = getReader(XML, true, false);

        // First 2 valid cases:
        assertTokenType(START_ELEMENT, sr.next());
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("Got some  text 'n stuff!", sr.getElementText());
        assertTokenType(END_ELEMENT, sr.getEventType());
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("more stuff ", sr.getElementText());
        assertTokenType(END_ELEMENT, sr.getEventType());

        // and then invalid
        assertTokenType(START_ELEMENT, sr.next());
        try {
            String str = sr.getElementText();
            fail("Expected an exception for nested start element");
        } catch (XMLStreamException ex) {
            ; // ok!
        }
    }

    public void testNextTag()
        throws XMLStreamException
    {
        String XML =
            "<root>   "
            +"<tag>   </tag>"
            +"<tag><leaf />   </tag>"
            +"<tag>text</tag>"
            ;
        XMLStreamReader sr = getReader(XML, true, false);
        // First valid cases:
        assertTokenType(START_ELEMENT, sr.nextTag());
        assertEquals("root", sr.getLocalName());
        assertTokenType(START_ELEMENT, sr.nextTag());
        assertTokenType(END_ELEMENT, sr.nextTag());
        assertEquals("tag", sr.getLocalName());

        assertTokenType(START_ELEMENT, sr.nextTag());
        assertEquals("tag", sr.getLocalName());

        assertTokenType(START_ELEMENT, sr.nextTag());
        assertEquals("leaf", sr.getLocalName());

        assertTokenType(END_ELEMENT, sr.nextTag());
        assertEquals("leaf", sr.getLocalName());

        assertTokenType(END_ELEMENT, sr.nextTag());
        assertEquals("tag", sr.getLocalName());

        // And then invalid:
        assertTokenType(START_ELEMENT, sr.nextTag());
        assertEquals("tag", sr.getLocalName());
        try {
            int type = sr.nextTag();
            fail("Expected an exception for non-whitespace text");
        } catch (XMLStreamException ex) {
            ; // ok!
        }
    }

    public void testNextTagWithCommentsAndPIs()
        throws XMLStreamException
    {
        String XML =
            "<root>   <!-- comment -->   <leaf />\n"
            +"<leaf><?target data.. ?></leaf>\n"
            +"<leaf><!-- comment --><!-- comment --> <!--comment--></leaf>\n"
            +"<leaf><?target data.. ?><!--comment  --><?target?></leaf>\n"
            +"</root>"
            ;
        XMLStreamReader sr = getReader(XML, true, false);

        assertTokenType(START_ELEMENT, sr.nextTag());
        assertEquals("root", sr.getLocalName());

        // First, an empty 'leaf' element
        assertTokenType(START_ELEMENT, sr.nextTag());
        assertEquals("leaf", sr.getLocalName());
        assertTokenType(END_ELEMENT, sr.nextTag());
        assertEquals("leaf", sr.getLocalName());

        // Then one with a single PI in it
        assertTokenType(START_ELEMENT, sr.nextTag());
        assertEquals("leaf", sr.getLocalName());
        assertTokenType(END_ELEMENT, sr.nextTag());
        assertEquals("leaf", sr.getLocalName());

        // Then one with multiple comments
        assertTokenType(START_ELEMENT, sr.nextTag());
        assertEquals("leaf", sr.getLocalName());
        assertTokenType(END_ELEMENT, sr.nextTag());
        assertEquals("leaf", sr.getLocalName());

        // Then one with a mix:
        assertTokenType(START_ELEMENT, sr.nextTag());
        assertEquals("leaf", sr.getLocalName());
        assertTokenType(END_ELEMENT, sr.nextTag());
        assertEquals("leaf", sr.getLocalName());

        // And then the closing root tag
        assertTokenType(END_ELEMENT, sr.nextTag());
        assertEquals("root", sr.getLocalName());
    }

    /**
     * Test that checks that stream reader's behavior at the end of
     * input is compliant. Specifically, an exception should be thrown
     * if one tries to access events beyond END_DOCUMENT.
     */
    public void testEndOfStream()
        throws XMLStreamException
    {
        String XML = "<root>x</root>";
        XMLStreamReader sr = getReader(XML, true, true);

        assertTokenType(START_DOCUMENT, sr.getEventType());
        assertTokenType(START_ELEMENT, sr.next());
        assertTokenType(CHARACTERS, sr.next());
        assertTokenType(END_ELEMENT, sr.next());
        assertTokenType(END_DOCUMENT, sr.next());
        assertFalse(sr.hasNext());

        try {
            int type = sr.next();
            fail("Expected NoSuchElementException when trying to access events after END_DOCUMENT returned (but received event "+tokenTypeDesc(type)+")");
        } catch (java.util.NoSuchElementException ne) {
            // good
        } catch (XMLStreamException e) { // wrong exception
            fail("Expected NoSuchElementException; received (type "+e.getClass()+"): "+e);
        }
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
}
