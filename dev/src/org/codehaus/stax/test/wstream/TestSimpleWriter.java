package org.codehaus.stax.test.wstream;

import javax.xml.stream.*;

import java.io.*;

/**
 * Set of unit tests for verifying operation of {@link XMLStreamWriter}
 * in "non-repairing" mode.
 */
public class TestSimpleWriter
    extends BaseWriterTest
{
    final String ISO_LATIN_ENCODING = "ISO-8859-1";

    public void testXmlDeclImplicit()
        throws IOException, XMLStreamException
    {
        doTextXmlDecl(3);
    }

    public void testXmlDecl0args()
        throws IOException, XMLStreamException
    {
        doTextXmlDecl(0);
    }

    public void testXmlDecl1arg()
        throws IOException, XMLStreamException
    {
        doTextXmlDecl(1);
    }

    public void testXmlDecl2args()
        throws IOException, XMLStreamException
    {
        doTextXmlDecl(2);
    }

    /*
    //////////////////////////////////
    // Private methods
    //////////////////////////////////
     */

    private void doTextXmlDecl(int i)
        throws IOException, XMLStreamException
    {
        /* 4 modes: writeStartDocument with 0 args, 1 arg, 2 args,
         *   and without a call
         */

        StringWriter strw = new StringWriter();
        XMLStreamWriter w = getNonRepairingWriter(strw);
        
        switch (i) {
        case 0:
            w.writeStartDocument();
            break;
        case 1:
            /* Might well be ok to output other than 1.0, but the
             * reader may choke on others (like 1.1)?
             */
            w.writeStartDocument("1.0");
            break;
        case 2:
            w.writeStartDocument(ISO_LATIN_ENCODING, "1.0");
            break;
        case 3:
            // No output (shouldn't print out xml decl)
            break;
        }
        w.writeEmptyElement("root");
        w.writeEndDocument();
        w.close();
        
        XMLStreamReader sr = constructNsStreamReader(strw.toString());
        assertTokenType(START_DOCUMENT, sr.getEventType());
        
        // correct version?
        if (i == 3) {
            // Shouldn't have output anything:
            String ver =sr.getVersion();
            if (ver != null && ver.length() > 0) {
                fail("Non-null/empty version ('"+ver+"') when no START_DOCUMENT written explicitly");
            }
        } else {
            assertEquals("1.0", sr.getVersion());
        }
        
        // encoding?
        String enc = sr.getCharacterEncodingScheme();
        switch (i) {
        case 0:
            /* Not sure why the encoding has to default to utf-8... would
             * make sense to rather leave it out
             */
            assertEquals("utf-8", enc);
            break;
        case 1:
            /* Interestingly enough, API comments do not indicate an encoding
             * default for 1-arg method!
             */
            assertNull(enc);
            break;
        case 2:
            assertEquals(ISO_LATIN_ENCODING, enc);
            break;
        case 3:
            assertNull(enc);
            break;
        }
        
        // What should sr.getEncoding() return? null? can't check...
        
        // but stand-alone we can check:
        assertFalse("XMLStreamReader.standalonSet() should return false if pseudo-attr not found",
                    sr.standaloneSet());
        assertFalse("XMLStreamReader.isStandalone() should return false if pseudo-attr not found",
                    sr.isStandalone());
        sr.close();
    }
}
