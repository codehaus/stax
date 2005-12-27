package org.codehaus.stax.test.vstream;

import javax.xml.stream.*;

import org.codehaus.stax.test.SimpleResolver;

/**
 * Unit test suite that tests how implementation handles combining of
 * internal and external DTD subsets.
 *
 * @author Tatu Saloranta
 */
public class TestSubsetCombination
    extends BaseVStreamTest
{
    /**
     * This unit test checks that a DTD definition that is evenly split
     * between subsets will be properly combined, and results in a usable
     * definition for validation.
     */
    public void testValidSubsets()
        throws XMLStreamException
    {
        // Note: need to resolve using a custom resolver
        String XML =
            "<!DOCTYPE root SYSTEM 'dummy-url' [\n"
            +"<!ELEMENT root (leaf+)>\n"
            +"<!ATTLIST root attrInt CDATA #IMPLIED>\n"
            +"<!ENTITY ent1 '&ent2;'>\n"
            +"]><root attrInt='value' attrExt='someValue'>  <leaf>Test entities: &ent1, &ent2;</leaf>"
            +"]><root attrInt='value'>  <leaf>Test entities: &ent1, &ent2;</leaf>"
            +"<leaf /></root>";
        String EXT_DTD = 
            "<!ELEMENT leaf (#PCDATA)>\n"
            +"<!ATTLIST root attrExt CDATA #IMPLIED>\n"
            ;
        streamThrough(getReader(XML, true, EXT_DTD));
        // Let's also test that non-ns works, just in case it's different
        streamThrough(getReader(XML, false, EXT_DTD));
    }

    /*
    ////////////////////////////////////////
    // Non-test methods
    ////////////////////////////////////////
     */

    private XMLStreamReader getReader(String contents, boolean nsAware,
                                      String extSubset)
        throws XMLStreamException
    {
        XMLInputFactory f = getInputFactory();
        setNamespaceAware(f, nsAware);
        setSupportDTD(f, true);
        setCoalescing(f, false);
        setReplaceEntities(f, true);
        setValidating(f, true);
        if (extSubset != null) {
            setResolver(f, new SimpleResolver(extSubset));
        }
        return constructStreamReader(f, contents);
    }
}

