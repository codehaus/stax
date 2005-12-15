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
        // Uncomment to see if we get exceptions we should be getting:
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

    public void testValidMixed()
        throws XMLStreamException
    {
        for (int i = 0; i < 2; ++i) {
            boolean nsAware = (i > 0);
            String XML = "<!DOCTYPE root [\n"
                +"<!ELEMENT root (#PCDATA | leaf)*>\n"
                +"<!ELEMENT leaf (#PCDATA)>\n"
                +"]><root>Text <leaf /></root>";
            streamThrough(getReader(XML, nsAware));
        }
    }

    public void testInvalidMixed()
        throws XMLStreamException
    {
        for (int i = 0; i < 2; ++i) {
            boolean nsAware = (i > 0);
            String XML = "<!DOCTYPE root [\n"
                +"<!ELEMENT root (leaf)*>\n"
                +"<!ELEMENT leaf (#PCDATA)>\n"
                +"]><root>Text <leaf /></root>";
            streamThroughFailing(getReader(XML, nsAware),
                                 "invalid mixed content");
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
            
            // Then one wrong element content for root
            XML = "<!DOCTYPE root [\n"
                +"<!ELEMENT root (branch+, end)>\n"
                +"<!ELEMENT branch (#PCDATA)>\n"
                +"<!ELEMENT end EMPTY>\n"
                +"]>\n  <root />";
            streamThroughFailing(getReader(XML, nsAware),
                                 "wrong element content (expected branch+, end; got nothing) for root");
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

    /**
     * Unit test that checks that it's illegal to add any content (including
     * comment or processing instructions) within an element that has
     * content declaration of EMPTY.
     */
    public void testInvalidEmpty()
        throws XMLStreamException
    {
        for (int i = 0; i < 2; ++i) {
            boolean nsAware = (i > 0);
            String XML = "<!DOCTYPE root [\n"
                +"<!ELEMENT root EMPTY>\n"
                +"]><root><!-- comment --></root>";
            streamThroughFailing(getReader(XML, nsAware),
                                 "comment within element that has EMPTY content type declaration");

            XML = "<!DOCTYPE root [\n"
                +"<!ELEMENT root EMPTY>\n"
                +"]><root><?proc instr?></root>";
            streamThroughFailing(getReader(XML, nsAware),
                                 "processing instruction within element that has EMPTY content type declaration");
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
