package org.codehaus.stax.test.wstream;

import javax.xml.stream.*;

import java.io.*;

/**
 * Set of unit tests for verifying operation of {@link XMLStreamWriter}
 * when outputting text nodes that contain characters that should
 * be quoted.
 *
 * @author Tatu Saloranta
 */
public class TestOutputEncoding
    extends BaseWriterTest
{
    final String ISO_LATIN_ENCODING = "ISO-8859-1";
    final String UTF8_ENCODING = "UTF-8";

    public void testSimpleContentQuoting()
        throws IOException, XMLStreamException
    {
        String TEXT = "<tag>&amp;";
        doTestSimpleQuoting(ISO_LATIN_ENCODING, TEXT);
        doTestSimpleQuoting(UTF8_ENCODING, TEXT);

        TEXT = "Need to quote this too: ]]>";
        doTestSimpleQuoting(ISO_LATIN_ENCODING, TEXT);
        doTestSimpleQuoting(UTF8_ENCODING, TEXT);

        TEXT = "And nbsp: \u00A0.";
        doTestSimpleQuoting(ISO_LATIN_ENCODING, TEXT);
        doTestSimpleQuoting(UTF8_ENCODING, TEXT);
    }

    public void testSimpleAttrQuoting()
        throws IOException, XMLStreamException
    {
        String TEXT = "<tag>&amp;";
        doTestSimpleAttr(ISO_LATIN_ENCODING, TEXT);
        doTestSimpleAttr(UTF8_ENCODING, TEXT);

        // Plus, need to quote single/double quotes properly
        TEXT = "'\"fab\"'";
        doTestSimpleAttr(ISO_LATIN_ENCODING, TEXT);
        doTestSimpleAttr(UTF8_ENCODING, TEXT);

        // And let's test non-ascii char too:
        TEXT = "Nbsp -> \u00A0.";
        doTestSimpleAttr(ISO_LATIN_ENCODING, TEXT);
        doTestSimpleAttr(UTF8_ENCODING, TEXT);
    }

    /*
    /////////////////////////////////////////////////////
    // Helper methods
    /////////////////////////////////////////////////////
     */

    private void doTestSimpleQuoting(String encoding, String content)
        throws IOException, XMLStreamException
    {
        StringWriter strw = new StringWriter();
        XMLStreamWriter w = getNonRepairingWriter(strw);

        w.writeStartDocument(encoding, "1.0");
        w.writeStartElement("root");
        w.writeCharacters(content);
        w.writeEndElement();
        w.writeEndDocument();
        w.close();
        
        // And then let's parse and verify it all:
        XMLStreamReader sr = constructNsStreamReader(strw.toString(), true);
        assertTokenType(START_DOCUMENT, sr.getEventType());
        assertEquals(encoding, sr.getCharacterEncodingScheme());
        assertTokenType(START_ELEMENT, sr.next());

        // May get multiple segments..
        assertTokenType(CHARACTERS, sr.next());
        assertEquals(content, getAllText(sr));
        assertTokenType(END_ELEMENT, sr.getEventType());
        assertTokenType(END_DOCUMENT, sr.next());
    }

    private void doTestSimpleAttr(String encoding, String attrValue)
        throws IOException, XMLStreamException
    {
        StringWriter strw = new StringWriter();
        XMLStreamWriter w = getNonRepairingWriter(strw);

        w.writeStartDocument(encoding, "1.0");
        w.writeStartElement("root");
        w.writeAttribute("attr", attrValue);
        w.writeEndElement();
        w.writeEndDocument();
        w.close();

        // And then let's parse and verify it all:
        XMLStreamReader sr = constructNsStreamReader(strw.toString(), true);
        assertTokenType(START_DOCUMENT, sr.getEventType());
        assertEquals(encoding, sr.getCharacterEncodingScheme());
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals(1, sr.getAttributeCount());
        assertEquals(attrValue, sr.getAttributeValue(0));
        assertTokenType(END_ELEMENT, sr.next());
        assertTokenType(END_DOCUMENT, sr.next());
    }
}
