package org.codehaus.stax.test.stream;

import javax.xml.stream.*;

/**
 * Unit test suite that tests that the stream reader does in fact
 * coalesce adjacent text/CDATA segments when told to do so.
 */
public class TestCDataRead
    extends BaseStreamTest
{
    final static String CDATA1;
    final static String CDATA2;
    static {
        StringBuffer sb1 = new StringBuffer(8000);
        StringBuffer sb2 = new StringBuffer(8000);

        sb1.append("...");
        sb2.append("\n \n\n ");

        /* Let's add enough stuff to probably cause segmentation...
         */
        for (int i = 0; i < 200; ++i) {
            String txt = "Round #"+i+"; & that's fun: &x"+i+"; <> %xx; &ent  <<< %%% <![CDATA ]]  > ]> ";
            sb1.append(txt);
            sb1.append("  ");
            sb2.append("\n");
            sb2.append(txt);
        }

        CDATA1 = sb1.toString();
        CDATA2 = sb2.toString();
    }

    final static String CDATA3 = " ]] ";

    final static String EXP_CDATA = CDATA1 + CDATA2 + CDATA3;

    final static String VALID_XML =
        "<root>"
        +"<![CDATA["+CDATA1+"]]>"
        +"<![CDATA["+CDATA2+"]]>"
        +"<![CDATA[]]>"
        +"<![CDATA["+CDATA3+"]]>"
        +"</root>";

    public TestCDataRead(String name) {
        super(name);
    }

    public void testCDataCoalescing()
        throws XMLStreamException
    {
        XMLStreamReader sr = getReader(VALID_XML, true);
        assertTokenType(START_ELEMENT, sr.next());
        /* In coalescing mode, all CDATA are reported as CHARACTERS
         */
        assertTokenType(CHARACTERS, sr.next());
        String act = getAndVerifyText(sr);
        assertEquals(EXP_CDATA, act);
        assertTokenType(END_ELEMENT, sr.next());
    }

    public void testCDataNonCoalescing()
        throws XMLStreamException
    {
        XMLStreamReader sr = getReader(VALID_XML, false);
        assertTokenType(START_ELEMENT, sr.next());
        assertTokenType(CDATA, sr.next());

        StringBuffer sb = new StringBuffer(16000);
        do {
            sb.append(getAndVerifyText(sr));
        } while (sr.next() == CDATA);
        assertEquals(EXP_CDATA, sb.toString());
        assertTokenType(END_ELEMENT, sr.getEventType());
    }

    public void testInvalidCData()
        throws XMLStreamException
    {
        String XML = "<root><![CDATA[   </root>";
        String MSG = "unfinished CDATA section";
        streamThroughFailing(getReader(XML, false), MSG);
        streamThroughFailing(getReader(XML, true), MSG);

        XML = "<root><![CDATA  [text]]>   </root>";
        MSG = "malformed CDATA section";
        streamThroughFailing(getReader(XML, false), MSG);
        streamThroughFailing(getReader(XML, true), MSG);

        XML = "<root><!  [ CDATA  [text]]>   </root>";
        streamThroughFailing(getReader(XML, false), MSG);
        streamThroughFailing(getReader(XML, true), MSG);

        XML = "<root><![CDATA[text   ]] >   </root>";
        streamThroughFailing(getReader(XML, false), MSG);
        streamThroughFailing(getReader(XML, true), MSG);
    }

    /*
    ////////////////////////////////////////
    // Private methods, other
    ////////////////////////////////////////
     */

    private XMLStreamReader getReader(String contents, boolean coalescing)
        throws XMLStreamException
    {
        XMLInputFactory f = getInputFactory();
        setCoalescing(f, coalescing);
        setReplaceEntities(f, true);
        setValidating(f, false);
        return constructStreamReader(f, contents);
    }

}
