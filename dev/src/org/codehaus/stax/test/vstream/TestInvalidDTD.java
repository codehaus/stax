package org.codehaus.stax.test.vstream;

import javax.xml.stream.*;

/**
 * Simple unit test suite that checks for set of well-formedness problems
 * with DTDs
 *
 * @author Tatu Saloranta
 */
public class TestInvalidDTD
    extends BaseVStreamTest
{
    public void testInvalidDirectives()
        throws XMLStreamException
    {
        String XML = "<!DOCTYPE root [\n"
            +"<!ELEM root EMPTY>\n"
            +"]>\n<root />";
        streamThroughFailing(getValidatingReader(XML), "invalid directive '<!ELEM ...>'");

        XML = "<!DOCTYPE root [\n"
            +"<!ELEMENT root EMPTY>\n"
            +"<!ATTRLIST root attr CDATA #IMPLIED>\n"
            +"]>\n<root />";
        streamThroughFailing(getValidatingReader(XML), "invalid directive '<!ATRLIST ...>'");
    }

    public void testInvalidComment()
        throws XMLStreamException
    {
        String XML = "<!DOCTYPE root [\n"
            +"<!ELEMENT root EMPTY>\n"
            +"<!-- Can not have '--' in here! (unlike in SGML) -->\n"
            +"]><root />";
        streamThroughFailing(getValidatingReader(XML), "invalid directive '<!ELEM ...>'");
    }

    /**
     * CDATA directive not allowed in DTD subsets.
     */
    public void testInvalidCData()
        throws XMLStreamException
    {
        String XML = "<!DOCTYPE root [\n"
            +"<!ELEMENT root EMPTY>\n"
            +"<![CDATA[ hah! ]]>\n"
            +"]><root />";
        streamThroughFailing(getValidatingReader(XML), "invalid CDATA directive in int. DTD subset");
    }
}
