package org.codehaus.stax.test.vstream;

import javax.xml.stream.*;

/**
 * Unit test suite that tests handling of attributes that are declared
 * by DTD to be of type NOTATION.
 */
public class TestEntityAttrRead
    extends BaseVStreamTest
{
    public TestEntityAttrRead(String name) {
        super(name);
    }

    /*
    ///////////////////////////////////////
    // Test cases
    ///////////////////////////////////////
     */

    public void testValidEntityAttrDecl()
        throws XMLStreamException
    {
        // Following should be ok; notations have been declared ok
        String XML = "<!DOCTYPE root [\n"
            +"<!ELEMENT root EMPTY>\n"
            +"<!NOTATION not1 PUBLIC 'public-notation-id'>\n"
            +"<!ENTITY unpEnt SYSTEM 'system-ent-id' NDATA not1>\n"
            +"<!ATTLIST root ent ENTITY #IMPLIED>\n"
            +"]>\n<root />";
        streamThrough(getValidatingReader(XML));

        // Likewise for default values
        XML = "<!DOCTYPE root [\n"
            +"<!ELEMENT root EMPTY>\n"
            +"<!NOTATION not1 PUBLIC 'public-notation-id'>\n"
            +"<!ENTITY unpEnt SYSTEM 'system-ent-id' NDATA not1>\n"
            +"<!ATTLIST root ent ENTITY 'unpEnt'>\n"
            +"]>\n<root />";
        streamThrough(getValidatingReader(XML));
    }

    public void testValidEntitiesAttrDecl()
        throws XMLStreamException
    {
        // Following should be ok; notations have been declared ok
        String XML = "<!DOCTYPE root [\n"
            +"<!ELEMENT root EMPTY>\n"
            +"<!NOTATION not1 PUBLIC 'public-notation-id'>\n"
            +"<!ENTITY unpEnt SYSTEM 'system-ent-id' NDATA not1>\n"
            +"<!ATTLIST root ent ENTITIES #IMPLIED>\n"
            +"]>\n<root />";
        streamThrough(getValidatingReader(XML));

        // and for default values
        XML = "<!DOCTYPE root [\n"
            +"<!ELEMENT root EMPTY>\n"
            +"<!NOTATION not1 PUBLIC 'public-notation-id'>\n"
            +"<!NOTATION not2 PUBLIC 'public-notation-id2'>\n"
            +"<!ENTITY unpEnt SYSTEM 'system-ent-id' NDATA not1>\n"
            +"<!ENTITY unpEnt2 SYSTEM 'system-ent-id' NDATA not1>\n"
            +"<!ATTLIST root ent ENTITIES 'unpEnt   unpEnt2  '>\n"
            +"]>\n<root />";
        streamThrough(getValidatingReader(XML));
    }

    public void testInvalidEntityAttrDecl()
        throws XMLStreamException
    {
        // First, let's check that undeclared notation throws an exception
        String XML = "<!DOCTYPE root [\n"
            +"<!ELEMENT root EMPTY>\n"
            +"<!NOTATION notX PUBLIC 'public-notation-id'>\n"
            +"<!ENTITY unpEnt SYSTEM 'system-ent-id' NDATA not1>\n"
            +"<!ATTLIST root ent ENTITY #IMPLIED>\n"
            +"]>\n<root />";
        streamThroughFailing(getValidatingReader(XML),
                             "undeclared notation for ENTITY attribute");

        // Similarly, undeclared entity via default value
        XML = "<!DOCTYPE root [\n"
            +"<!ELEMENT root EMPTY>\n"
            +"<!NOTATION notX PUBLIC 'public-notation-id'>\n"
            +"<!ENTITY unpEnt SYSTEM 'system-ent-id' NDATA notX>\n"
            +"<!ATTLIST root ent ENTITY 'foobar'>\n"
            +"]>\n<root />";
        streamThroughFailing(getValidatingReader(XML),
                             "undeclared entity for ENTITY default value");
    }

    public void testInvalidEntitiesAttrDecl()
        throws XMLStreamException
    {
        // First, let's check that undeclared notation throws an exception
        String XML = "<!DOCTYPE root [\n"
            +"<!ELEMENT root EMPTY>\n"
            +"<!NOTATION notX PUBLIC 'public-notation-id'>\n"
            +"<!ENTITY unpEnt SYSTEM 'system-ent-id' NDATA not1>\n"
            +"<!ATTLIST root ent ENTITIES #IMPLIED>\n"
            +"]>\n<root />";
        streamThroughFailing(getValidatingReader(XML),
                             "undeclared notation for ENTITIES attribute");

        // Similarly, undeclared entity via default value
        XML = "<!DOCTYPE root [\n"
            +"<!ELEMENT root EMPTY>\n"
            +"<!NOTATION notX PUBLIC 'public-notation-id'>\n"
            +"<!ENTITY unpEnt SYSTEM 'system-ent-id' NDATA notX>\n"
            +"<!ATTLIST root ent ENTITIES 'foobar'>\n"
            +"]>\n<root />";
        streamThroughFailing(getValidatingReader(XML),
                             "undeclared entity for ENTITIES default value");
    }

    /*
    public void testValidEntityAttrUse()
        throws XMLStreamException
    {
    }
    */

    /*
    public void testValidEntitiesAttrUse()
        throws XMLStreamException
    {
    }
    */

    /*
    public void testInvalidEntityAttrUse()
        throws XMLStreamException
    {
    }
    */

    /*
    public void testInvalidEntitiesAttrUse()
        throws XMLStreamException
    {
    }
    */

}
