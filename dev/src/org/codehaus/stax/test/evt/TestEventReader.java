package org.codehaus.stax.test.evt;

import java.util.NoSuchElementException;

import javax.xml.stream.*;
import javax.xml.stream.events.*;

/**
 * Class that contains simple tests for making sure that event objects
 * created by the {@link XMLEventFactory} have expected properties.
 */
public class TestEventReader
    extends BaseEventTest
{
    public TestEventReader(String name) {
        super(name);
    }

    public void testSimpleValid()
        throws XMLStreamException
    {
        /* Whether prolog/epilog white space is reported is not defined
         * by StAX specs, thus, let's not add any
         */
        String XML = "<?xml version='1.0' ?>"
            +"<!DOCTYPE root [  ]>"
            +"<root attr='123'><!-- comment -->\n"
            +"</root>";

        for (int i = 0; i < 4; ++i) {
            boolean ns = (i & 1) != 0;
            boolean coal = (i & 2) != 0;
            XMLEventReader er = getReader(XML, ns, coal);
            
            assertTokenType(START_DOCUMENT, er.nextEvent().getEventType());
            assertTokenType(DTD, er.nextEvent().getEventType());
            assertTokenType(START_ELEMENT, er.nextEvent().getEventType());
            assertTokenType(COMMENT, er.nextEvent().getEventType());
            assertTokenType(CHARACTERS, er.nextEvent().getEventType());
            assertTokenType(END_ELEMENT, er.nextEvent().getEventType());
            assertTokenType(END_DOCUMENT, er.nextEvent().getEventType());
            assertFalse(er.hasNext());
        }
    }

    /**
     * The main purpose of this test is to ensure that an exception
     * is thrown at the end.
     */
    public void testIterationEndException()
        throws XMLStreamException
    {
        String XML = "<root />";

        for (int i = 0; i < 4; ++i) {
            boolean coal = (i & 1) != 0;
            boolean checkHasNext = (i & 2) != 0;
            XMLEventReader er = getReader(XML, true, coal);
            
            assertTokenType(START_DOCUMENT, er.nextEvent().getEventType());
            assertTokenType(START_ELEMENT, er.nextEvent().getEventType());
            assertTokenType(END_ELEMENT, er.nextEvent().getEventType());
            assertTokenType(END_DOCUMENT, er.nextEvent().getEventType());

            if (checkHasNext) {
                assertFalse(er.hasNext());
            }

            XMLEvent ev = null;
            try {
                ev = er.nextEvent();
            } catch (NoSuchElementException nex) {
                continue; // good
            } catch (Throwable t) {
                fail("Expected a NoSuchElementException after iterating through the document; got "+t);
            }

            // Shouldn't get this far...
            fail("Expected a NoSuchElementException after iterating through the document; got event: "+ev);
        }
    }

    public void testNextTag()
        throws XMLStreamException
    {
        String XML = "<root>\n"
            +"<branch>   <leaf>  </leaf></branch>"
            +"</root>";

        for (int i = 0; i < 4; ++i) {
            boolean ns = (i & 1) != 0;
            boolean coal = (i & 2) != 0;
            XMLEventReader er = getReader(XML, ns, coal);
            
            assertTokenType(START_DOCUMENT, er.nextEvent().getEventType());

            assertTokenType(START_ELEMENT, er.nextTag().getEventType());
            assertTokenType(START_ELEMENT, er.nextTag().getEventType());
            assertTokenType(START_ELEMENT, er.nextTag().getEventType());

            assertTokenType(END_ELEMENT, er.nextTag().getEventType());
            assertTokenType(END_ELEMENT, er.nextTag().getEventType());
            assertTokenType(END_ELEMENT, er.nextTag().getEventType());

            assertTokenType(END_DOCUMENT, er.nextEvent().getEventType());
            assertFalse(er.hasNext());
        }
    }

    public void testSkip()
        throws XMLStreamException
    {
        String XML = "<?xml version='1.0' ?><!DOCTYPE root><root>\n"
            +"<branch>   <leaf>  </leaf></branch><!-- comment -->"
            +"</root>";

        for (int i = 0; i < 4; ++i) {
            boolean ns = (i & 1) != 0;
            boolean coal = (i & 2) != 0;
            XMLEventReader er = getReader(XML, ns, coal);
            XMLEvent ev;

            assertTokenType(START_DOCUMENT, er.peek().getEventType());
            assertTokenType(START_DOCUMENT, er.nextEvent().getEventType());
            assertTokenType(DTD, er.peek().getEventType());
            assertTokenType(DTD, er.nextEvent().getEventType());
            assertTokenType(START_ELEMENT, er.peek().getEventType());
            assertTokenType(START_ELEMENT, er.nextEvent().getEventType());

            assertTokenType(CHARACTERS, er.peek().getEventType());
            assertTokenType(CHARACTERS, er.nextEvent().getEventType());

            // branch
            assertTokenType(START_ELEMENT, er.peek().getEventType());
            assertTokenType(START_ELEMENT, er.nextEvent().getEventType());
            assertTokenType(CHARACTERS, er.peek().getEventType());
            assertTokenType(CHARACTERS, er.nextEvent().getEventType());

            // leaf
            assertTokenType(START_ELEMENT, er.peek().getEventType());
            assertTokenType(START_ELEMENT, er.nextEvent().getEventType());
            assertTokenType(CHARACTERS, er.peek().getEventType());
            assertTokenType(CHARACTERS, er.nextEvent().getEventType());
            assertTokenType(END_ELEMENT, er.peek().getEventType());
            assertTokenType(END_ELEMENT, er.nextEvent().getEventType());

            assertTokenType(END_ELEMENT, er.peek().getEventType());
            assertTokenType(END_ELEMENT, er.nextEvent().getEventType());

            assertTokenType(COMMENT, er.peek().getEventType());
            assertTokenType(COMMENT, er.nextEvent().getEventType());
            assertTokenType(END_ELEMENT, er.peek().getEventType());
            assertTokenType(END_ELEMENT, er.nextEvent().getEventType());

            assertTokenType(END_DOCUMENT, er.peek().getEventType());
            assertTokenType(END_DOCUMENT, er.nextEvent().getEventType());
            assertFalse(er.hasNext());
        }
    }

    /*
    /////////////////////////////////////////////////
    // Internal methods:
    /////////////////////////////////////////////////
     */

    private XMLEventReader getReader(String contents, boolean nsAware,
                                     boolean coalesce)
        throws XMLStreamException
    {
        XMLInputFactory f = getInputFactory();
        setNamespaceAware(f, nsAware);
        setCoalescing(f, coalesce);
        setSupportDTD(f, true);
        setValidating(f, false);
        return constructEventReader(f, contents);
    }
}
