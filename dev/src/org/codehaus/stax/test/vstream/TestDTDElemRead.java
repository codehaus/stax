package org.codehaus.stax.test.vstream;

import javax.xml.stream.*;

/**
 * Unit test suite that tests handling of DTD element declarations.
 */
public class TestDTDElemRead
    extends BaseVStreamTest
{
    public TestDTDElemRead(String name) {
        super(name);
    }

    /*
    ///////////////////////////////////////
    // Element declaration tests:
    ///////////////////////////////////////
     */

    public void testValidElementDecl()
        throws XMLStreamException
    {
        /* Following should be ok; it is not an error to refer to
         * undeclared elements... although it is to encounter such
         * undeclared elements in content.
         */
        String XML = "<!DOCTYPE root [\n"
            +"<!ELEMENT root (node*)>\n"
            +"]>\n<root />";
        streamThrough(getReader(XML));
    }

    public void testInvalidElementDecl()
        throws XMLStreamException
    {
        /* Then let's make sure that duplicate element declarations
         * are caught (as they are errors):
         */
        String XML = "<!DOCTYPE root [\n"
            +"<!ELEMENT root (node*)>\n"
            +"<!ELEMENT node EMPTY>\n"
            +"<!ELEMENT root (node*)>\n"
            +"]>\n<root />";
        try {
            streamThrough(getReader(XML));
            fail("Expected an exception for duplicate ELEMENT declaration.");
        } catch (XMLStreamException ex) { // good
        } catch (RuntimeException ex2) { // ok
        } catch (Throwable t) { // not so good
            fail("Expected an XMLStreamException or RuntimeException for duplicate ELEMENT declaration, not: "+t);
        }
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
