/*
 * Copyright 2002-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation and was
 * originally based on software copyright (c) 2001, 2002, International
 * Business Machines, Inc., http://www.apache.org.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.XMLEvent;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Junit test to test out
 * <a href="http://www.extreme.indiana.edu/bugzilla/show_bug.cgi?id=192">Bug 192
 * Escaped characters disappear in certain cases.</a> contributed by
 * Thijs Janssen.
 */
public class XmlEscapingTest extends TestCase {
    
    public static void main (String[] args) {
        junit.textui.TestRunner.run (new TestSuite(XmlEscapingTest.class));
    }
    
    public void testXmlError() throws Exception {
        InputStream stream = new ByteArrayInputStream(
            "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><document>&lt;text&gt;</document>"
                .getBytes());
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader reader = factory.createXMLEventReader(stream);
        StartDocument startdoc = (StartDocument) reader.nextEvent();
        assertEquals("UTF-8", startdoc.getCharacterEncodingScheme());
        assertEquals("1.0", startdoc.getVersion());
        assertTrue(reader.hasNext());
        XMLEvent event = reader.nextEvent();
        assertTrue(event.isStartElement());
        event = reader.nextEvent();
        assertTrue(event.isCharacters());
        String c = event.asCharacters().getData();
        
        event = reader.nextEvent();
        assertTrue(event.isCharacters());
        c+=event.asCharacters().getData();

        assertEquals("<text>",c); //FAILURE expected "<text>" but was "<text"
        event = reader.nextEvent();
        assertTrue(event.isEndElement());

    }
    
    public void testXmlError2() throws Exception {
        InputStream stream = new ByteArrayInputStream(
            "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><document>&lt;text&gt; </document>"
                .getBytes());
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader reader = factory.createXMLEventReader(stream);
        StartDocument startdoc = (StartDocument) reader.nextEvent();
        assertEquals("UTF-8", startdoc.getCharacterEncodingScheme());
        assertEquals("1.0", startdoc.getVersion());
        assertTrue(reader.hasNext());
        XMLEvent event = reader.nextEvent();
        assertTrue(event.isStartElement());
        event = reader.nextEvent();
        assertTrue(event.isCharacters());
        String c = event.asCharacters().getData();
        event = reader.nextEvent();
        assertTrue(event.isCharacters());
        c+=event.asCharacters().getData();
        event = reader.nextEvent();
        assertTrue(event.isEndElement());
        
        assertEquals("<text> ",c); //SUCCES
    }
    
}

