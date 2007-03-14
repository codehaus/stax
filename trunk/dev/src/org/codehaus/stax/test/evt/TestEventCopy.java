package org.codehaus.stax.test.evt;

import java.util.NoSuchElementException;

import javax.xml.stream.*;
import javax.xml.stream.events.*;

import java.io.*;
import java.util.*;

/**
 * This test tries to verify that events can be copied from event reader
 * to event writer, and result in well-formed output
 *
 * @author Tatu Saloranta
 */
public class TestEventCopy
    extends BaseEventTest
{
	public void testCopy()
        throws XMLStreamException
    {
        final String XML =
            "<root>\n"
            +" <branch>\n"
            +"   <leaf attr='123' />"
            +" </branch>\n"
            +" <leaf attr='\"a\"' />"
            +"</root>"
            ;
		XMLEventReader er = getEventReader(XML, true);
        StringWriter strw = new StringWriter();
        XMLOutputFactory f = getOutputFactory();
        XMLEventWriter ew = f.createXMLEventWriter(strw);

		while (er.hasNext()) {
            ew.add(er.nextEvent());
		}
        ew.close();

        // And let's then just verify it's well-formed still
        // (should also check it's still the same, too...)
        String results = strw.toString();
        er = getEventReader(results, true);
		while (er.hasNext()) {
            assertNotNull(er.nextEvent());
        }
	}

    private XMLEventReader getEventReader(String contents, boolean nsAware)
        throws XMLStreamException
    {
        XMLInputFactory f = getInputFactory();
        setNamespaceAware(f, nsAware);
        setCoalescing(f, true);
        setSupportDTD(f, true);
        setValidating(f, false);
        return constructEventReader(f, contents);
    }
}
