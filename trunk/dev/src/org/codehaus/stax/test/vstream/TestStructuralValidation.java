package org.codehaus.stax.test.vstream;

import javax.xml.stream.*;

/**
 * Unit test suite that tests structural validation using DTD.
 */
public class TestStructuralValidation
    extends BaseVStreamTest
{
    public TestStructuralValidation(String name) {
        super(name);
        // To see if we get exceptions we should be getting:
        //PRINT_EXP_EXCEPTION = true;
    }

    public void testValidStructure()
        throws XMLStreamException
    {
        for (int i = 0; i < 2; ++i) {
            boolean nsAware = (i > 0);
            String XML = "<!DOCTYPE root [\n"
            +"<!ELEMENT root (branch+, end)>\n"
                +"<!ELEMENT branch (#PCDATA)>\n"
                +"<!ELEMENT end EMPTY>\n"
                +"]>\n<root><branch />  <branch>Text</branch>  <end /> </root>";
            streamThrough(getReader(XML, nsAware));
        }
    }

    public void testInvalidStructureRoot()
        throws XMLStreamException
    {
        for (int i = 0; i < 2; ++i) {
            boolean nsAware = (i > 0);

            // First, wrong root element
            String XML = "<!DOCTYPE root [\n"
                +"<!ELEMENT root (branch+, end)>\n"
                +"<!ELEMENT branch (#PCDATA)>\n"
                +"<!ELEMENT end EMPTY>\n"
                +"]>\n<branch />";
            streamThroughFailing(getReader(XML, nsAware),
                                 "wrong root element");
            
            // Then undeclared (root) element
            XML = "<!DOCTYPE root [\n"
                +"<!ELEMENT branch EMPTY>\n"
                +"]>\n  <root />";
            streamThroughFailing(getReader(XML, nsAware),
                                 "undeclared element");
            
            // Then one wrong element content for root (empty)
            XML = "<!DOCTYPE root [\n"
                +"<!ELEMENT root (branch+, end)>\n"
                +"<!ELEMENT branch (#PCDATA)>\n"
                +"<!ELEMENT end EMPTY>\n"
                +"]>\n  <root />";
            streamThroughFailing(getReader(XML, nsAware),
                                 "wrong element content (empty) for root");
        }
    }

    public void testInvalidStructure()
        throws XMLStreamException
    {
        for (int i = 0; i < 2; ++i) {
            boolean nsAware = (i > 0);

            // And then just wrong ordering of child elements
            String XML = "<!DOCTYPE root [\n"
                +"<!ELEMENT root (branch+, end)>\n"
                +"<!ELEMENT branch (#PCDATA)>\n"
                +"<!ELEMENT end EMPTY>\n"
                +"]>\n  <root><end /></root>";
            streamThroughFailing(getReader(XML, nsAware),
                                 "wrong element content (ordering) for root");
            
            XML = "<!DOCTYPE root [\n"
                +"<!ELEMENT root (branch+, end)>\n"
                +"<!ELEMENT branch (#PCDATA)>\n"
                +"<!ELEMENT end EMPTY>\n"
                +"]>\n  <root><branch>xyz</branch></root>";
            streamThroughFailing(getReader(XML, nsAware),
                                 "wrong element content (missing 'end' element) for root");
        }
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
        setCoalescing(f, false); // shouldn't really matter
        setNamespaceAware(f, nsAware);
        setSupportDTD(f, true);
        setValidating(f, true);
        return constructStreamReader(f, contents);
    }
}
