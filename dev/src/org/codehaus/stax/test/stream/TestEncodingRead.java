package org.codehaus.stax.test.stream;

import java.io.UnsupportedEncodingException;

import javax.xml.stream.*;

/**
 * Unit test suite that tests handling of text encoding, as specified
 * by XML declaration and/or specific byte-order markers.
 */
public class TestEncodingRead
    extends BaseStreamTest
{
    public TestEncodingRead(String name) {
        super(name);
    }

    final String UTF_1 = String.valueOf((char) 0x41); // 'A'
    final String UTF_2 = String.valueOf((char) 0xA0); // nbsp
    final String UTF_3 = String.valueOf((char) 0xB61); // some char that needs 3-byte encoding

    final String UTF_CONTENT = ""
        +UTF_1 + UTF_2 + UTF_3
        +UTF_1 + UTF_1 + UTF_2 + UTF_2 + UTF_3 + UTF_3
        +UTF_3 + UTF_3 + UTF_2 + UTF_2 + UTF_1 + UTF_1
        +UTF_1 + UTF_3 + UTF_2
        +UTF_2 + UTF_1 + UTF_3
        +UTF_2 + UTF_3 + UTF_1
        +UTF_3 + UTF_1 + UTF_2
        +UTF_3 + UTF_2 + UTF_1
        ;

    public void testUTF8()
        throws XMLStreamException,
               UnsupportedEncodingException
    {
        String XML = "<?xml version='1.0' encoding=\"UTF-8\"  ?>"
            +"<root>"+UTF_CONTENT+"</root>";
        byte[] b = XML.getBytes("UTF-8");

        XMLStreamReader sr = getReader(b);

        assertEquals("UTF-8", sr.getCharacterEncodingScheme());

        assertEquals(START_ELEMENT, sr.next());
        assertEquals(CHARACTERS, sr.next());

        assertEquals(UTF_CONTENT, getAndVerifyText(sr));

        assertEquals(END_ELEMENT, sr.next());
        assertEquals(END_DOCUMENT, sr.next());
    }

    /*
    ////////////////////////////////////////
    // Private methods, other
    ////////////////////////////////////////
     */

    private XMLStreamReader getReader(byte[] contents)
        throws XMLStreamException
    {
        XMLInputFactory f = getInputFactory();
        setValidating(f, false);
        return constructStreamReader(f, contents);
    }
}
