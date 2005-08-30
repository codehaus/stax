package org.codehaus.stax.test.vstream;

import javax.xml.stream.*;

/**
 * Unit test suite that tests handling of attributes that are declared
 * by DTD to be of type ID, IDREF or IDREFS; such information is only
 * guranteed to be available in validation mode.
 */
public class TestIdAttrRead
    extends BaseVStreamTest
{
    public TestIdAttrRead(String name) {
        super(name);
    }

    /*
    ///////////////////////////////////////
    // Test cases
    ///////////////////////////////////////
     */

    /**
     * Test case that verifies behaviour of valid ID/IDREF/IDREF
     * attribute declarations.
     */
    public void testValidIdAttrDecl()
        throws XMLStreamException
    {
        // Following should be ok
        String XML = "<!DOCTYPE root [\n"
            +"<!ELEMENT root EMPTY>\n"
            +"<!ATTLIST root name ID #IMPLIED\n"
            +"    ref IDREF #IMPLIED>\n"
            +"<!ATTLIST root refs IDREFS #IMPLIED>\n"
            +"]>\n<root />";
        streamThrough(getValidatingReader(XML));
    }

    /**
     * Test case that verifies behaviour of invalid ID/IDREF/IDREF
     * attribute declarations.
     */
    public void testInvalidIdAttrDecl()
        throws XMLStreamException
    {
        /* First, let's check couple of invalid id attr declarations
         */

        // Can not have default value for id attr
        String XML = "<!DOCTYPE root [\n"
            +"<!ELEMENT root EMPTY>\n"
            +"<!ATTLIST root name ID 'defId'>\n"
            +"]>\n<root />";
        XMLStreamReader sr = getValidatingReader(XML);
        streamThroughFailing(sr, "invalid attribute id (default value not allowed)");

        // Nor require fixed value
        sr = getValidatingReader("<!DOCTYPE root [\n"
            +"<!ELEMENT root EMPTY>\n"
            +"<!ATTLIST root name ID #FIXED 'fixedId'>\n"
            +"]>\n<root />");
        streamThroughFailing(sr, "invalid id attribute (fixed value not allowed)");

        // Only one attr id per element
        sr = getValidatingReader("<!DOCTYPE root [\n"
            +"<!ELEMENT root EMPTY>\n"
            +"<!ATTLIST root name ID #IMPLIED name2 ID #IMPLIED>\n"
            +"]>\n<root />");
        streamThroughFailing(sr, "more than one attribute id per element");
    }

    public void testInvalidIdRefAttrDecl()
        throws XMLStreamException
    {
        // IDREF default value needs to be valid id
        XMLStreamReader sr = getValidatingReader("<!DOCTYPE root [\n"
            +"<!ELEMENT root EMPTY>\n"
            +"<!ATTLIST root name IDREF 'foo#bar'>\n"
            +"]>\n<root />");
        streamThroughFailing(sr, "invalid IDREF default value ('#' not allowed)");
        sr = getValidatingReader("<!DOCTYPE root [\n"
            +"<!ELEMENT root EMPTY>\n"
            +"<!ATTLIST root name IDREF ''>\n"
            +"]>\n<root />");
        streamThroughFailing(sr, "invalid (missing) IDREF default value");

        // IDREFS default value needs to be non-empty set of valid ids
        sr = getValidatingReader("<!DOCTYPE root [\n"
            +"<!ELEMENT root EMPTY>\n"
            +"<!ATTLIST root name IDREF 'foo b?'>\n"
            +"]>\n<root />");
        streamThroughFailing(sr, "invalid IDREFS default value ('?' not allowed)");
        sr = getValidatingReader("<!DOCTYPE root [\n"
            +"<!ELEMENT root EMPTY>\n"
            +"<!ATTLIST root name IDREF ''>\n"
            +"]>\n<root />");
        streamThroughFailing(sr, "invalid (missing) IDREFS default value");
    }

    public void testValidIdAttrUse()
        throws XMLStreamException
    {
        // Following should be ok; all ids are defined
        String XML = "<!DOCTYPE elem [\n"
            +"<!ELEMENT elem (elem*)>\n"
            +"<!ATTLIST elem id ID #IMPLIED>\n"
            +"<!ATTLIST elem ref IDREF #IMPLIED>\n"
            +"]>\n<elem ref='someId'>   <elem id='someId' /> </elem>";
        streamThrough(getValidatingReader(XML));
    }

    public void testInvalidIdAttrUse()
        throws XMLStreamException
    {
        // Error: undefined id 'someId'
        String XML = "<!DOCTYPE elem [\n"
            +"<!ELEMENT elem (elem*)>\n"
            +"<!ATTLIST elem id ID #IMPLIED>\n"
            +"<!ATTLIST elem ref IDREF #IMPLIED>\n"
            +"]>\n<elem ref='someId'/>";
        streamThroughFailing(getValidatingReader(XML),
                             "undefined id reference for 'someId'");

        // Error: empty idref value
        XML = "<!DOCTYPE elem [\n"
            +"<!ELEMENT elem (elem*)>\n"
            +"<!ATTLIST elem id ID #IMPLIED>\n"
            +"<!ATTLIST elem ref IDREF #IMPLIED>\n"
            +"]>\n<elem ref=''/>";
        streamThroughFailing(getValidatingReader(XML),
                             "empty IDREF value");
    }

    public void testValidIdAttrsUse()
        throws XMLStreamException
    {
        // Following should be ok; all ids are defined
        String XML = "<!DOCTYPE elem [\n"
            +"<!ELEMENT elem (elem*)>\n"
            +"<!ATTLIST elem id ID #IMPLIED>\n"
            +"<!ATTLIST elem refs IDREFS #IMPLIED>\n"
            +"]>\n<elem id='someId1'>\n"
            +"<elem id='someId2' refs='someId1  someId2  '  />\n"
            +"</elem>";
        streamThrough(getValidatingReader(XML));
    }

    public void testInvalidIdAttrsUse()
        throws XMLStreamException
    {
        // Error: undefined id 'someId'
        String XML = "<!DOCTYPE elem [\n"
            +"<!ELEMENT elem (elem*)>\n"
            +"<!ATTLIST elem id ID #IMPLIED>\n"
            +"<!ATTLIST elem refs IDREFS #IMPLIED>\n"
            +"]>\n<elem refs='someId'/>";
        streamThroughFailing(getValidatingReader(XML),
                             "undefined id reference for 'someId'");

        // Error: empty idrefs value
        XML = "<!DOCTYPE elem [\n"
            +"<!ELEMENT elem (elem*)>\n"
            +"<!ATTLIST elem id ID #IMPLIED>\n"
            +"<!ATTLIST elem refs IDREFS #IMPLIED>\n"
            +"]>\n<elem refs=''/>";
        streamThroughFailing(getValidatingReader(XML),
                             "empty IDREFS value");
    }

    /*
    ////////////////////////////////////////
    // Private methods
    ////////////////////////////////////////
     */
}
