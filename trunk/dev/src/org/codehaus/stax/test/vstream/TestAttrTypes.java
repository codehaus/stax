package org.codehaus.stax.test.vstream;

import javax.xml.stream.*;

/**
 * Unit test suite that tests that attribute type information returned
 * for all recognized types is as expected
 */
public class TestAttrTypes
    extends BaseVStreamTest
{
    public TestAttrTypes(String name) {
        super(name);
    }

    /*
    ///////////////////////////////////////
    // Test cases
    ///////////////////////////////////////
     */

    public void testAttrTypes()
        throws XMLStreamException
    {
        // Let's verify we get default value
        String XML = "<!DOCTYPE root [\n"
            +"<!ELEMENT root EMPTY>\n"
            +"<!ATTLIST root \n"
            +"attrCData CDATA #IMPLIED\n"
            +"attrId ID #IMPLIED\n"
            +"attrIdref IDREF #IMPLIED\n"
            +"attrIdrefs IDREFS #IMPLIED\n"
            +"attrEnum (val1| val2) #IMPLIED\n"
            +"attr ENTITY #IMPLIED\n"
            +"attr ENTITIES #IMPLIED\n"
            +"attr NMTOKEN #IMPLIED\n"
            +"attr NMTOKENS #IMPLIED\n"
            +">\n"
            +"]>\n<root/>";
        XMLStreamReader sr = getValidatingReader(XML, true);
        assertTokenType(DTD, sr.next());
        assertTokenType(START_ELEMENT, sr.next());

        // !!! TBI
    }

    /*
    ////////////////////////////////////////
    // Private methods
    ////////////////////////////////////////
     */

    private XMLStreamReader getReader(String contents)
        throws XMLStreamException
    {
        XMLInputFactory f = getInputFactory();
        setCoalescing(f, false); // shouldn't really matter
        //setNamespaceAware(f, nsAware);
        setSupportDTD(f, true);
        // Let's make sure DTD is really parsed?
        setValidating(f, true);
        return constructStreamReader(f, contents);
    }
}
