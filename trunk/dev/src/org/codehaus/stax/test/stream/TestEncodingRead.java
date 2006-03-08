package org.codehaus.stax.test.stream;

import java.io.*;

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

    final static byte[] BE_BOM = new byte[] { (byte) 0xFE, (byte) 0xFF };
    final static byte[] LE_BOM = new byte[] { (byte) 0xFF, (byte) 0xFE };
    final static byte[] UTF8_BOM = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

    public void testUTF8()
        throws Exception
    {
        doTestEncoding("UTF-8", true, null);
        doTestEncoding("UTF-8", false, null);
    }

    public void testUTF8WithBOM()
        throws Exception
    {
        doTestEncoding("UTF-8", true, UTF8_BOM);
        doTestEncoding("UTF-8", false, UTF8_BOM);
    }

    /**
     * This unit test checks specifically whether the implementation can
     * use a BOM-tagged Reader; JDK seems to (sometimes?) leave BOM in
     * the Reader's stream...
     */
    public void testUTF8ViaReader()
        throws XMLStreamException, IOException
    {
        String XML = "...<?xml version='1.0' encoding='UTF-8'?><root></root>";
        byte[] b = XML.getBytes("UTF-8");
        b[0] = (byte) 0xEF;
        b[1] = (byte) 0xBB;
        b[2] = (byte) 0xBF;
        
        InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(b), "UTF-8");
        
        try {
            XMLInputFactory f = getInputFactory();
            setValidating(f, false);
            XMLStreamReader sr = f.createXMLStreamReader(reader);
            assertTokenType(START_DOCUMENT, sr.getEventType());
            assertTokenType(START_ELEMENT, sr.next());
            assertEquals("root", sr.getLocalName());
            sr.close();
        } finally {
            reader.close();
        }
    }

    public void testUTF8Surrogates()
        throws XMLStreamException, IOException
    {
        String XML = "<?xml version='1.0' encoding='UTF-8'?><root>XXXX</root>";
        int ix = XML.indexOf('X');
        byte[] src = XML.getBytes("UTF-8");

        // A somewhat random high-order Unicode char:
        src[ix] = (byte)0xF1;
        src[ix+1] = (byte)0x90;
        src[ix+2] = (byte)0x88;
        src[ix+3] = (byte)0x88;

        InputStream in = new ByteArrayInputStream(src);
        XMLInputFactory f = getInputFactory();
        XMLStreamReader sr = f.createXMLStreamReader(in);

        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        assertTokenType(CHARACTERS, sr.next());
        String str = getAndVerifyText(sr);
        // Should result in a surrogate pair...
        assertEquals(2, str.length());
        assertEquals((char) 0xd900, str.charAt(0));
        assertEquals((char) 0xde08, str.charAt(1));
        assertTokenType(END_ELEMENT, sr.next());
    }

    public void testUTF16()
        throws XMLStreamException,
               UnsupportedEncodingException
    {
        /* Not 100% sure on how to test it... I know JDK defaults to
         * big-endian... could maybe write using BE/LE variants, but
         * include declaration of plain 'UTF-16' instead?
         */
        /* For now, let's trust JDK to add the BE BOM, and leave it
         * at that.
         */
        doTestEncoding("UTF-16", true, null);
        doTestEncoding("UTF-16", false, null);
    }

    public void testUTF16BEWithBOM()
        throws XMLStreamException,
               UnsupportedEncodingException
    {
        doTestEncoding("UTF-16BE", true, BE_BOM);
        doTestEncoding("UTF-16BE", false, BE_BOM);

        doTestEncoding2(true);
    }

    public void testUTF16BEWithoutBOM()
        throws XMLStreamException,
               UnsupportedEncodingException
    {
        doTestEncoding("UTF-16BE", true, null);
        doTestEncoding("UTF-16BE", false, null);
    }

    public void testUTF16LEWithBOM()
        throws XMLStreamException,
               UnsupportedEncodingException
    {
        doTestEncoding("UTF-16LE", true, LE_BOM);
        doTestEncoding("UTF-16LE", false, LE_BOM);

        doTestEncoding2(false);
    }

    public void testUTF16LEWithoutBOM()
        throws XMLStreamException,
               UnsupportedEncodingException
    {
        doTestEncoding("UTF-16LE", true, null);
        doTestEncoding("UTF-16LE", false, null);
    }

    /*
    ////////////////////////////////////////
    // Private methods, shared test code
    ////////////////////////////////////////
     */

    public void doTestEncoding(String enc, boolean explicit,
			       byte[] bom)
        throws XMLStreamException,
               UnsupportedEncodingException
    {
        String XML = "<?xml version='1.0'";
        
        if (explicit) {
            XML +=  " encoding='"+enc+"'";
        }
        XML += "?><root>"+UTF_CONTENT+"</root>";
        
        byte[] b = XML.getBytes(enc);
        if (bom != null) {
            /* 22-Mar-2005, TSa: Hack! I'll be damned if I know for sure
             *   whether JDK is going to add BOMs... so let's just add
             *   a special check to make sure no duplicate BOMs are added.
             */
            if (bom[0] == b[0]) {
                System.err.println("Warning: JDK implicitly added BOM for '"+enc+"'; won't try to add it implicitly...");
            } else {
                byte[] orig = b;
                b = new byte[b.length + bom.length];
                System.arraycopy(bom, 0, b, 0, bom.length);
                System.arraycopy(orig, 0, b, bom.length, orig.length);
            }
        }
        
        XMLStreamReader sr = getReader(b);
        
        if (explicit) {
            assertEquals(enc, sr.getCharacterEncodingScheme());
        } else {
            // otherwise... should we get some info?
        }
        
        assertEquals(START_ELEMENT, sr.next());
        assertEquals(CHARACTERS, sr.next());
        
        assertEquals(UTF_CONTENT, getAndVerifyText(sr));

        assertEquals(END_ELEMENT, sr.next());
        assertEquals(END_DOCUMENT, sr.next());
    }

    private void doTestEncoding2(boolean bigEndian)
        throws XMLStreamException
    {

        /* 20-Jan-2006, TSa: Ok, let's try another variation that may
         *    causes problem; UTF-16 is vague, and if using JDK provided
         *    readers, parser has to indicate endianness.
         */
        final String XML = "<?xml version='1.0' encoding='UTF-16'?>\n"
            +"<!--comment--><root>text</root>";
        int len = XML.length();
        byte[] b = new byte[2 + len + len];

        if (bigEndian) {
            b[0] = (byte) 0xFE;
            b[1] = (byte) 0xFF;
        } else {
            b[0] = (byte) 0xFF;
            b[1] = (byte) 0xFE;
        }

        int offset = bigEndian ? 3 : 2;
        for (int i = 0; i < len; ++i) {
            b[offset + i + i] = (byte) XML.charAt(i);
        }
        XMLStreamReader sr = getReader(b);
        // may get white space...
        int type = sr.next();
        if (type == SPACE) {
            type = sr.next();
        }
        assertTokenType(COMMENT, type);
        assertTokenType(START_ELEMENT, sr.next());
        assertTokenType(CHARACTERS, sr.next());
        assertEquals("text", getAndVerifyText(sr));
        assertTokenType(END_ELEMENT, sr.next());
        sr.close();
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
