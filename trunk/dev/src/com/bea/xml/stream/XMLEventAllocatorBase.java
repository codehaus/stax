/*   Copyright 2004 BEA Systems, Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.bea.xml.stream;

import com.bea.xml.stream.util.ElementTypeNames;
import java.util.Iterator;

import javax.xml.namespace.QName;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.XMLEventAllocator;
import javax.xml.stream.util.XMLEventConsumer;
import javax.xml.stream.XMLStreamException;

import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.EndDocument;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.DTD;

import com.bea.xml.stream.util.EmptyIterator;
import java.util.ArrayList;

/**
 * <p> An allocator that creates an event per method call. </p>
 */

public class XMLEventAllocatorBase 
  implements XMLEventAllocator 
{
  XMLEventFactory factory;

  public XMLEventAllocatorBase() {
    factory = XMLEventFactory.newInstance();
  }

  public XMLEventAllocator newInstance() {
    return new XMLEventAllocatorBase();
  }

  public static Iterator getAttributes(XMLStreamReader reader) {
    if (reader.getAttributeCount()==0) return EmptyIterator.emptyIterator;
    int attributeCount = reader.getAttributeCount();
    ArrayList atts = new ArrayList();
    for (int i = 0; i < attributeCount; i++){
      atts.add(new AttributeBase(reader.getAttributePrefix(i),
                                 reader.getAttributeNamespace(i),
                                 reader.getAttributeLocalName(i),
                                 reader.getAttributeValue(i),
                                 reader.getAttributeType(i)));
    }
    return atts.iterator();
  }
  
  public static Iterator getNamespaces(XMLStreamReader reader) {
    if (reader.getNamespaceCount()==0) return EmptyIterator.emptyIterator;
    ArrayList ns = new ArrayList();
    for (int i = 0; i < reader.getNamespaceCount(); i++){  
      String prefix = reader.getNamespacePrefix(i);
      if(prefix == null ||
         prefix.equals("")){
        ns.add(new NamespaceBase(reader.getNamespaceURI(i)));
      } else {
        ns.add(new NamespaceBase(prefix,
                                 reader.getNamespaceURI(i)));
      }
    }
    return ns.iterator();
  }

  public StartElement allocateStartElement(XMLStreamReader reader) 
    throws XMLStreamException 
  {
    String prefix = reader.getPrefix();
    String uri = reader.getNamespaceURI();
    if (prefix == null) prefix = "";
    if (uri == null) uri = "";
    return factory.createStartElement(prefix,
                                      uri,
                                      reader.getLocalName(),
                                      getAttributes(reader),
                                      getNamespaces(reader));
  }

  public EndElement allocateEndElement(XMLStreamReader reader) 
    throws XMLStreamException 
  {
    String prefix = reader.getPrefix();
    String uri = reader.getNamespaceURI();
    if (prefix == null) prefix = "";
    if (uri == null) uri = "";
    return factory.createEndElement(prefix,
                                    uri,
                                    reader.getLocalName(),
                                    getNamespaces(reader)
                                    );
  }

  public Characters allocateCharacters(XMLStreamReader reader) 
    throws XMLStreamException
  {
    int start = reader.getTextStart();
    int length = reader.getTextLength();
    String result = new String(reader.getTextCharacters(),
                               start,
                               length);
    if (reader.isWhiteSpace())
      return factory.createSpace(result);
    else
      return factory.createCharacters(result);
  }

  public Characters allocateCData(XMLStreamReader reader) 
    throws XMLStreamException
  {
    return factory.createCData(reader.getText());
  }

  public Characters allocateSpace(XMLStreamReader reader) 
    throws XMLStreamException
  {
    return factory.createSpace(reader.getText());
  }

  public EntityReference allocateEntityReference(XMLStreamReader reader) 
    throws XMLStreamException
  {
    return factory.createEntityReference(reader.getLocalName(),
                                         null);
  }

  public ProcessingInstruction allocatePI(XMLStreamReader reader) 
    throws XMLStreamException
  {
    return factory.createProcessingInstruction(reader.getPITarget(),
                                               reader.getPIData());
  }

  public Comment allocateComment(XMLStreamReader reader) 
    throws XMLStreamException
  {
    return factory.createComment(reader.getText());
  }

  public StartDocument allocateStartDocument(XMLStreamReader reader)
    throws XMLStreamException
  {
    return allocateXMLDeclaration(reader);
  }

  public EndDocument allocateEndDocument(XMLStreamReader reader)
    throws XMLStreamException
  {
    return factory.createEndDocument();
  }

  public DTD allocateDTD(XMLStreamReader reader)
    throws XMLStreamException
  {
    return factory.createDTD(reader.getText());
  }

  public StartDocument allocateXMLDeclaration(XMLStreamReader reader)
    throws XMLStreamException
  {
    String encoding = reader.getCharacterEncodingScheme();
    String version = reader.getVersion();
    boolean standalone = reader.isStandalone();
    if (encoding != null && 
        version != null &&
        !standalone ) {
      return factory.createStartDocument(encoding,
                                         version,
                                         standalone);
    }
    if (version != null && 
        encoding != null)
      return factory.createStartDocument(encoding,
                                         version);

    if (encoding != null)
    return factory.createStartDocument(encoding);

    return factory.createStartDocument();
  }
  

  public XMLEvent allocate(XMLStreamReader reader) 
    throws XMLStreamException
  {
    switch (reader.getEventType()) {
    case XMLEvent.START_ELEMENT: return allocateStartElement(reader);
    case XMLEvent.END_ELEMENT: return allocateEndElement(reader);
    case XMLEvent.CHARACTERS: return allocateCharacters(reader);
    case XMLEvent.SPACE: return allocateCharacters(reader);
    case XMLEvent.CDATA: return allocateCData(reader);
    case XMLEvent.ENTITY_REFERENCE: return allocateEntityReference(reader);
    case XMLEvent.PROCESSING_INSTRUCTION: return allocatePI(reader);
    case XMLEvent.COMMENT: return allocateComment(reader);
      //    case XMLEvent.XML_DECLARATION: return allocateXMLDeclaration(reader);
    case XMLEvent.START_DOCUMENT: return allocateStartDocument(reader);
    case XMLEvent.END_DOCUMENT: return allocateEndDocument(reader);
    case XMLEvent.DTD: return allocateDTD(reader);
    default:
      throw new XMLStreamException("Unable to allocate event["+
                                   reader.getEventType()+" , "+
                                   ElementTypeNames.getEventTypeString(reader.getEventType())+"]");
    }
    //    return new com.bea.xml.stream.events.NullEvent();
  }

  public void allocate(XMLStreamReader reader,
                       XMLEventConsumer consumer) 
    throws XMLStreamException
  {
    consumer.add(allocate(reader));
  }

  public String toString() {
    return "NonStaticAllocator";
  }
}





