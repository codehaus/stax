package org.codehaus.stax.test.vstream;

import javax.xml.stream.*;

/**
 * Unit test suite that tests basic handling of attributes; aspects
 * that do not depend on actual concrete type.
 */
public class TestAttrRead
    extends BaseVStreamTest
{
    public TestAttrRead(String name) {
        super(name);
    }

    /*
    ///////////////////////////////////////
    // Attribute declaration tests:
    ///////////////////////////////////////
     */

    /**
     * Simple tests for generic valid attribute declarations; using
     * some constructs that can be warned about, but that are not
     * erroneous.
     */
    public void testValidAttrDecl()
        throws XMLStreamException
    {
        /* First; declaring attributes for non-declared elements is
         * not an error
         */
        String XML = "<!DOCTYPE root [\n"
            +"<!ELEMENT root EMPTY>\n"
            +"<!ATTLIST element attr CDATA #IMPLIED>\n"
            +"]>\n<root />";
        streamThrough(getValidatingReader(XML, true));

        /* Then, declaring same attribute more than once is not an
         * error; first one is binding (note: should test that this
         * indeed happens, via attribute property inspection?)
         */
        XML = "<!DOCTYPE root [\n"
            +"<!ELEMENT root EMPTY>\n"
            +"<!ATTLIST root attr CDATA #IMPLIED>\n"
            +"<!ATTLIST root attr CDATA #IMPLIED>\n"
            +"<!ATTLIST root attr CDATA #IMPLIED>\n"
            +"]>\n<root />";
        streamThrough(getValidatingReader(XML, true));
    }

    /**
     * Unit test that verifies that the attribute type declaration information
     * is properly parsed and accessible via stream reader.
     */
    public void testAttributeTypes()
        throws XMLStreamException
    {
        String XML = "<!DOCTYPE root [\n"
            +"<!ELEMENT root EMPTY>\n"
            +"<!ATTLIST root cdata CDATA #IMPLIED>\n"
            +"<!ATTLIST root id ID #IMPLIED>\n"
            +"<!ATTLIST root nmtoken NMTOKEN #IMPLIED>\n"
            +"<!ATTLIST root nmtokens NMTOKENS #IMPLIED>\n"
            +"]>\n"
            +"<root"
            +" cdata='content'"
            +" id='node1'"
            +" nmtoken='token'"
            +" nmtokens='token1 token2'"
            +"/>";
        /* Could/should extend to cover all types... but this should be
         * enough to at least determined reader does pass through the
         * type info (instead of always returning CDATA)
         */
        XMLStreamReader sr = getValidatingReader(XML, true);

        assertTokenType(DTD, sr.next());
        assertTokenType(START_ELEMENT, sr.next());

        assertEquals(4, sr.getAttributeCount());
        for (int i = 0; i < 4; ++i) {
            String ln = sr.getAttributeLocalName(i);
            String type = sr.getAttributeType(i);
            String expType = ln.toUpperCase();
            assertNotNull("Attribute type should never be null; CDATA should be returned if information not known/available");
            assertEquals("Incorrect attribute type for attribute '"+ln+"'",
                         expType, type);
        }
        assertTokenType(END_ELEMENT, sr.next());
    }

    public void testValidRequiredAttr()
        throws XMLStreamException
    {
        // this should be valid:
        String XML = "<!DOCTYPE root [\n"
            +"<!ELEMENT root EMPTY>\n"
            +"<!ATTLIST root attr CDATA #REQUIRED>\n"
            +"]>\n<root attr='value' />";
        XMLStreamReader sr = getValidatingReader(XML, true);
        assertTokenType(DTD, sr.next());
        assertTokenType(START_ELEMENT, sr.next());

        assertEquals(1, sr.getAttributeCount());
        assertEquals("attr", sr.getAttributeLocalName(0));
        assertEquals("value", sr.getAttributeValue(0));
    }

    public void testInvalidRequiredAttr()
        throws XMLStreamException
    {
        // Invalid as it's missing the required attribute
        String XML = "<!DOCTYPE root [\n"
            +"<!ELEMENT root EMPTY>\n"
            +"<!ATTLIST root attr CDATA #REQUIRED>\n"
            +"]>\n<root />";
        streamThroughFailing(getValidatingReader(XML, true),
                             "Missing required attribute value");
    }

    public void testFixedAttr()
        throws XMLStreamException
    {
        // Ok to omit altogether
        String XML = "<!DOCTYPE root [\n"
            +"<!ELEMENT root EMPTY>\n"
            +"<!ATTLIST root attr CDATA #FIXED 'fixed'>\n"
            +"]>\n<root/>";
        streamThrough(getValidatingReader(XML, true));

        // Or to use fixed value
        XML = "<!DOCTYPE root [\n"
            +"<!ELEMENT root EMPTY>\n"
            +"<!ATTLIST root attr CDATA #FIXED 'fixed'>\n"
            +"]>\n<root attr='fixed'/>";
        streamThrough(getValidatingReader(XML, true));

        // But not any other value; either completely different
        XML = "<!DOCTYPE root [\n"
            +"<!ELEMENT root EMPTY>\n"
            +"<!ATTLIST root attr CDATA #FIXED 'fixed'>\n"
            +"]>\n<root attr='wrong'/>";
        streamThroughFailing(getValidatingReader(XML),
                             "fixed attribute value not matching declaration");

        // Or one with extra white space (CDATA won't get normalized)
        XML = "<!DOCTYPE root [\n"
            +"<!ELEMENT root EMPTY>\n"
            +"<!ATTLIST root attr CDATA #FIXED 'fixed'>\n"
            +"]>\n<root attr=' fixed '/>";
        streamThroughFailing(getValidatingReader(XML),
                             "fixed attribute value not matching declaration");
    }

    /**
     * Unit test that verifies that the default attribute values are properly
     * used on validating mode.
     */
    public void testDefaultAttr()
        throws XMLStreamException
    {
        // Let's verify we get default value
        String XML = "<!DOCTYPE root [\n"
            +"<!ELEMENT root EMPTY>\n"
            +"<!ATTLIST root attr CDATA 'default'>\n"
            +"]>\n<root/>";
        XMLStreamReader sr = getValidatingReader(XML, true);
        assertTokenType(DTD, sr.next());
        assertTokenType(START_ELEMENT, sr.next());

        assertEquals(1, sr.getAttributeCount());
        assertEquals("attr", sr.getAttributeLocalName(0));
        assertEquals("default", sr.getAttributeValue(0));
    }

    /*
    ////////////////////////////////////////
    // Private methods
    ////////////////////////////////////////
     */
}