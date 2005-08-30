/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 */

package com.bea.xml.stream;

import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;

import com.bea.xml.stream.util.EmptyIterator;
import com.bea.xml.stream.util.ElementTypeNames;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import com.wutka.dtd.DTDParser;
import com.wutka.dtd.DTD;
import com.wutka.dtd.DTDEntity;
import com.wutka.dtd.DTDAttlist;
import com.wutka.dtd.DTDAttribute;


/**
 * XML Streaming Parser.
 *
 * @author Aleksander Slominski
 */
public class MXParser
  implements XMLStreamReader, Location
{
    protected static final String FEATURE_XML_ROUNDTRIP=
        "http://xmlpull.org/v1/doc/features.html#xml-roundtrip";
    protected static final String FEATURE_NAMES_INTERNED =
        "http://xmlpull.org/v1/doc/features.html#names-interned";

    public static final String FEATURE_PROCESS_DOCDECL =
        "http://xmlpull.org/v1/doc/features.html#process-docdecl";

    /**
     * These constants are used for diagnostics messages, and need to
     * match with ones from {@link XMLStreamConstants}.
     */
    public static final String [] TYPES = {
        "[UNKNOWN]", // 0 not used
        "START_ELEMENT",
        "END_ELEMENT",
        "PROCESSING_INSTRUCTION",
        "CHARACTERS", // "TEXT",
        "COMMENT",
        "SPACE", // "IGNORABLE_WHITESPACE",
        "START_DOCUMENT",
        "END_DOCUMENT",
        "ENTITY_REFERENCE",
        "ATTRIBUTE",
        "DTD",
        "CDATA",
        "NAMESPACE",
        "NOTATION_DECLARATION",
        "ENTITY_DECLARATION",
    };
    
    // TODO - cwitt : split TEXT into CHARACTERS and WHITESPACE
    private static final int TEXT=0x00004000;
    // TODO - cwitt : remove DOCDECL ?
    private static final int DOCDECL=0x00008000;
    // TODO - cwitt : move to XMLEvent ?
    // will not be available in event interface (info under start_document
    // in that case), just in cursor
  //private static final int XML_DECLARATION=0x00010000;
    // NOTE - cwitt : from XmlPullParser interface
    public static final String NO_NAMESPACE = "";
    
    /**
     * Implementation notice:
     * the is instance variable that controls if newString() is interning.
     * <p><b>NOTE:</b> newStringIntern <b>always</b> returns interned strings
     * and newString MAY return interned String depending on this variable.
     * <p><b>NOTE:</b> by default in this minimal implementation it is false!
     */
    protected boolean allStringsInterned;

    protected void resetStringCache() {
        //System.out.println("resetStringCache() minimum called");
    }

    protected String newString(char[] cbuf, int off, int len) {
        return new String(cbuf, off, len);
    }

    protected String newStringIntern(char[] cbuf, int off, int len) {
        return (new String(cbuf, off, len)).intern();
    }


    private static final boolean TRACE_SIZING = false;

    // NOTE: features are not resetable and typicaly defaults to false ...
    // TODO - cwitt : we always want to set these to true, so the featues
    // don't need to be defined in the factory, will remove them later here
    public static final String FEATURE_PROCESS_NAMESPACES =
        "http://xmlpull.org/v1/doc/features.html#process-namespaces";
    protected boolean processNamespaces = true;
    protected boolean roundtripSupported = true;

    // global parser state
    protected int lineNumber;
    protected int columnNumber;
    protected boolean seenRoot;
    protected boolean reachedEnd;
    protected int eventType;
    protected boolean emptyElementTag;
    // element stack
    protected int depth;
    protected char[] elRawName[];
    protected int elRawNameEnd[];
    //pnrotected int elRawNameEnd[];
    protected String elName[];
    protected String elPrefix[];
    protected String elUri[];
    //protected String elValue[];
    protected int elNamespaceCount[];

    protected String xmlVersion;
    protected boolean standalone=true;
    protected boolean standaloneSet=false;
    protected String charEncodingScheme;

  protected String piTarget;
  protected String piData;

  protected HashMap defaultAttributes;
    /**
     * Make sure that we have enough space to keep element stack if passed size.
     * It will always create one additional slot then current depth
     */
    protected void ensureElementsCapacity() {
        int elStackSize = elName != null ? elName.length : 0;
        if( (depth + 1) >= elStackSize) {
            // we add at least one extra slot ...
            int newSize = (depth >= 7 ? 2 * depth : 8) + 2; // = lucky 7 + 1 //25
            if(TRACE_SIZING) {
                System.err.println("elStackSize "+elStackSize+" ==> "+newSize);
            }
            boolean needsCopying = elStackSize > 0;
            String[] arr = null;
            arr = new String[newSize];
            if(needsCopying) System.arraycopy(elName, 0, arr, 0, elStackSize);
            elName = arr;
            arr = new String[newSize];
            if(needsCopying) System.arraycopy(elPrefix, 0, arr, 0, elStackSize);
            elPrefix = arr;
            arr = new String[newSize];
            if(needsCopying) System.arraycopy(elUri, 0, arr, 0, elStackSize);
            elUri = arr;

            int[] iarr = new int[newSize];
            if(needsCopying) {
                System.arraycopy(elNamespaceCount, 0, iarr, 0, elStackSize);
            } else {
                // special initialization
                iarr[0] = 0;
            }
            elNamespaceCount = iarr;

            //TODO: avoid using element raw name ...
            iarr = new int[newSize];
            if(needsCopying) {
                System.arraycopy(elRawNameEnd, 0, iarr, 0, elStackSize);
            }
            elRawNameEnd = iarr;

            char[][] carr = new char[newSize][];
            if(needsCopying) {
                System.arraycopy(elRawName, 0, carr, 0, elStackSize);
            }
            elRawName = carr;
            //            arr = new String[newSize];
            //            if(needsCopying) System.arraycopy(elLocalName, 0, arr, 0, elStackSize);
            //            elLocalName = arr;
            //            arr = new String[newSize];
            //            if(needsCopying) System.arraycopy(elDefaultNs, 0, arr, 0, elStackSize);
            //            elDefaultNs = arr;
            //            int[] iarr = new int[newSize];
            //            if(needsCopying) System.arraycopy(elNsStackPos, 0, iarr, 0, elStackSize);
            //            for (int i = elStackSize; i < iarr.length; i++)
            //            {
            //                iarr[i] = (i > 0) ? -1 : 0;
            //            }
            //            elNsStackPos = iarr;
            //assert depth < elName.length;
        }
    }


    // nameStart / name lookup tables based on XML 1.1 http://www.w3.org/TR/2001/WD-xml11-20011213/
    protected static final int LOOKUP_MAX = 0x400;
    protected static final char LOOKUP_MAX_CHAR = (char)LOOKUP_MAX;
    //    protected static int lookupNameStartChar[] = new int[ LOOKUP_MAX_CHAR / 32 ];
    //    protected static int lookupNameChar[] = new int[ LOOKUP_MAX_CHAR / 32 ];
    protected static boolean lookupNameStartChar[] = new boolean[ LOOKUP_MAX ];
    protected static boolean lookupNameChar[] = new boolean[ LOOKUP_MAX ];

    private static final void setName(char ch)
        //{ lookupNameChar[ (int)ch / 32 ] |= (1 << (ch % 32)); }
    { lookupNameChar[ ch ] = true; }
    private static final void setNameStart(char ch)
        //{ lookupNameStartChar[ (int)ch / 32 ] |= (1 << (ch % 32)); setName(ch); }
    { lookupNameStartChar[ ch ] = true; setName(ch); }

    static {
        setNameStart(':');
        for (char ch = 'A'; ch <= 'Z'; ++ch) setNameStart(ch);
        setNameStart('_');
        for (char ch = 'a'; ch <= 'z'; ++ch) setNameStart(ch);
        for (char ch = '\u00c0'; ch <= '\u02FF'; ++ch) setNameStart(ch);
        for (char ch = '\u0370'; ch <= '\u037d'; ++ch) setNameStart(ch);
        for (char ch = '\u037f'; ch < '\u0400'; ++ch) setNameStart(ch);

        setName('-');
        setName('.');
        for (char ch = '0'; ch <= '9'; ++ch) setName(ch);
        setName('\u00b7');
        for (char ch = '\u0300'; ch <= '\u036f'; ++ch) setName(ch);
    }

    //private final static boolean isNameStartChar(char ch) {
    protected boolean isNameStartChar(char ch) {
        return (ch < LOOKUP_MAX_CHAR && lookupNameStartChar[ ch ])
            || (ch >= LOOKUP_MAX_CHAR && ch <= '\u2027')
            || (ch >= '\u202A' &&  ch <= '\u218F')
            || (ch >= '\u2800' &&  ch <= '\uFFEF')
            ;

        //      if(ch < LOOKUP_MAX_CHAR) return lookupNameStartChar[ ch ];
        //      else return ch <= '\u2027'
        //              || (ch >= '\u202A' &&  ch <= '\u218F')
        //              || (ch >= '\u2800' &&  ch <= '\uFFEF')
        //              ;
        //return false;
        //        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == ':'
        //          || (ch >= '0' && ch <= '9');
        //        if(ch < LOOKUP_MAX_CHAR) return (lookupNameStartChar[ (int)ch / 32 ] & (1 << (ch % 32))) != 0;
        //        if(ch <= '\u2027') return true;
        //        //[#x202A-#x218F]
        //        if(ch < '\u202A') return false;
        //        if(ch <= '\u218F') return true;
        //        // added pairts [#x2800-#xD7FF] | [#xE000-#xFDCF] | [#xFDE0-#xFFEF] | [#x10000-#x10FFFF]
        //        if(ch < '\u2800') return false;
        //        if(ch <= '\uFFEF') return true;
        //        return false;


        // else return (supportXml11 && ( (ch < '\u2027') || (ch > '\u2029' && ch < '\u2200') ...
    }

    //private final static boolean isNameChar(char ch) {
    protected boolean isNameChar(char ch) {
        //return isNameStartChar(ch);

        //        if(ch < LOOKUP_MAX_CHAR) return (lookupNameChar[ (int)ch / 32 ] & (1 << (ch % 32))) != 0;

        return (ch < LOOKUP_MAX_CHAR && lookupNameChar[ ch ])
            || (ch >= LOOKUP_MAX_CHAR && ch <= '\u2027')
            || (ch >= '\u202A' &&  ch <= '\u218F')
            || (ch >= '\u2800' &&  ch <= '\uFFEF')
            ;
        //return false;
        //        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == ':'
        //          || (ch >= '0' && ch <= '9');
        //        if(ch < LOOKUP_MAX_CHAR) return (lookupNameStartChar[ (int)ch / 32 ] & (1 << (ch % 32))) != 0;

        //else return
        //  else if(ch <= '\u2027') return true;
        //        //[#x202A-#x218F]
        //        else if(ch < '\u202A') return false;
        //        else if(ch <= '\u218F') return true;
        //        // added pairts [#x2800-#xD7FF] | [#xE000-#xFDCF] | [#xFDE0-#xFFEF] | [#x10000-#x10FFFF]
        //        else if(ch < '\u2800') return false;
        //        else if(ch <= '\uFFEF') return true;
        //else return false;
    }

    protected boolean isS(char ch) {
        return (ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t');
        // || (supportXml11 && (ch == '\u0085' || ch == '\u2028');
    }

    //protected boolean isChar(char ch) { return (ch < '\uD800' || ch > '\uDFFF')
    //  ch != '\u0000' ch < '\uFFFE'


    // attribute stack
    protected int attributeCount;
    protected String attributeName[];
    protected int attributeNameHash[];
    //protected int attributeNameStart[];
    //protected int attributeNameEnd[];
    protected String attributePrefix[];
    protected String attributeUri[];
    protected String attributeValue[];
    //protected int attributeValueStart[];
    //protected int attributeValueEnd[];


    /**
     * Make sure that in attributes temporary array is enough space.
     */
    protected  void ensureAttributesCapacity(int size) {
        int attrPosSize = attributeName != null ? attributeName.length : 0;
        if(size >= attrPosSize) {
            int newSize = size > 7 ? 2 * size : 8; // = lucky 7 + 1 //25
            if(TRACE_SIZING) {
                System.err.println("attrPosSize "+attrPosSize+" ==> "+newSize);
            }
            boolean needsCopying = attrPosSize > 0;
            String[] arr = null;

            arr = new String[newSize];
            if(needsCopying) System.arraycopy(attributeName, 0, arr, 0, attrPosSize);
            attributeName = arr;

            arr = new String[newSize];
            if(needsCopying) System.arraycopy(attributePrefix, 0, arr, 0, attrPosSize);
            attributePrefix = arr;

            arr = new String[newSize];
            if(needsCopying) System.arraycopy(attributeUri, 0, arr, 0, attrPosSize);
            attributeUri = arr;

            arr = new String[newSize];
            if(needsCopying) System.arraycopy(attributeValue, 0, arr, 0, attrPosSize);
            attributeValue = arr;

            if( ! allStringsInterned ) {
                int[] iarr = new int[newSize];
                if(needsCopying) System.arraycopy(attributeNameHash, 0, iarr, 0, attrPosSize);
                attributeNameHash = iarr;
            }

            arr = null;
            // //assert attrUri.length > size
        }
    }

    /* TSa, 28-Oct-2004: Need to either initialize them here, or check for
     *   nulls later on. This seems to work
     */
    private final static String[] NO_STRINGS = new String[0];
    private final static int[] NO_INTS = new int[0];
    private final static char[] NO_CHARS = new char[0];

    // namespace stack

    protected int namespaceEnd;
    protected String namespacePrefix[] = NO_STRINGS;
    protected int namespacePrefixHash[];
    protected String namespaceUri[] = NO_STRINGS;

    protected void ensureNamespacesCapacity(int size) {
        int namespaceSize = namespacePrefix != null ? namespacePrefix.length : 0;
        if(size >= namespaceSize) {
            int newSize = size > 7 ? 2 * size : 8; // = lucky 7 + 1 //25
            if(TRACE_SIZING) {
                System.err.println("namespaceSize "+namespaceSize+" ==> "+newSize);
            }
            String[] newNamespacePrefix = new String[newSize];
            String[] newNamespaceUri = new String[newSize];
            if(namespacePrefix != null) {
                System.arraycopy(
                    namespacePrefix, 0, newNamespacePrefix, 0, namespaceEnd);
                System.arraycopy(
                    namespaceUri, 0, newNamespaceUri, 0, namespaceEnd);
            }
            namespacePrefix = newNamespacePrefix;
            namespaceUri = newNamespaceUri;


            if( ! allStringsInterned ) {
                int[] newNamespacePrefixHash = new int[newSize];
                if(namespacePrefixHash != null) {
                    System.arraycopy(
                        namespacePrefixHash, 0, newNamespacePrefixHash, 0, namespaceEnd);
                }
                namespacePrefixHash = newNamespacePrefixHash;
            }
            //prefixesSize = newSize;
            // //assert nsPrefixes.length > size && nsPrefixes.length == newSize
        }
    }

  // local namespace stack
  protected int localNamespaceEnd;
  protected String localNamespacePrefix[];
  protected int localNamespacePrefixHash[];
  protected String localNamespaceUri[];
  
  protected void ensureLocalNamespacesCapacity(int size) {
    int localNamespaceSize = localNamespacePrefix != null ? localNamespacePrefix.length : 0;
    if(size >= localNamespaceSize) {
      int newSize = size > 7 ? 2 * size : 8; // = lucky 7 + 1 //25
      if(TRACE_SIZING) {
        System.err.println("localNamespaceSize "+localNamespaceSize+" ==> "+newSize);
      }
      String[] newLocalNamespacePrefix = new String[newSize];
      String[] newLocalNamespaceUri = new String[newSize];
      if(localNamespacePrefix != null) {
        System.arraycopy(
                         localNamespacePrefix, 0, newLocalNamespacePrefix, 0, localNamespaceEnd);
        System.arraycopy(
                         localNamespaceUri, 0, newLocalNamespaceUri, 0, localNamespaceEnd);
      }
      localNamespacePrefix = newLocalNamespacePrefix;
      localNamespaceUri = newLocalNamespaceUri;
      
      
      if( ! allStringsInterned ) {
        int[] newLocalNamespacePrefixHash = new int[newSize];
        if(localNamespacePrefixHash != null) {
          System.arraycopy(
                           localNamespacePrefixHash, 0, newLocalNamespacePrefixHash, 0, localNamespaceEnd);
        }
        localNamespacePrefixHash = newLocalNamespacePrefixHash;
      }
      //prefixesSize = newSize;
      // //assert nsPrefixes.length > size && nsPrefixes.length == newSize
    }
  }
  
  public int getLocalNamespaceCount() {
    int startNs = elNamespaceCount[ depth - 1 ];
    return namespaceEnd-startNs;
  }
  
  // This returns an array of all the namespaces uris defined
  // in the scope of this element
  // To index into it you need to add the namespaceCount from
  // the previous depth
  private String getLocalNamespaceURI(int pos) {
    return namespaceUri[pos];
  }
  

  // This returns an array of all the namespaces prefixes defined
  // in the scope of this element
  // the prefix for the default ns is bound to null
  // To index into it you need to add the namespaceCount from
  // the previous depth
  private String getLocalNamespacePrefix(int pos){
    return namespacePrefix[pos];
  }
  
    /**
     * simplistic implementation of hash function that has <b>constant</b>
     * time to compute - so it also means diminishing hash quality for long strings
     * but for XML parsing it should be good enough ...
     */
    protected static final int fastHash( char ch[], int off, int len ) {
        if(len == 0) return 0;
        //assert len >0
        int hash = ch[off]; // hash at beginnig
        //try {
        hash = (hash << 7) + ch[ off +  len - 1 ]; // hash at the end
        //} catch(ArrayIndexOutOfBoundsException aie) {
        //    aie.printStackTrace(); //should never happen ...
        //    throw new RuntimeException("this is violation of pre-condition");
        //}
        if(len > 16) hash = (hash << 7) + ch[ off + (len / 4)];  // 1/4 from beginning
        if(len > 8)  hash = (hash << 7) + ch[ off + (len / 2)];  // 1/2 of string size ...
        // notice that hash is at most done 3 times <<7 so shifted by 21 bits 8 bit value
        // so max result == 29 bits so it is quite just below 31 bits for long (2^32) ...
        //assert hash >= 0;
        return  hash;
    }

    // entity replacement stack
    protected int entityEnd;
    protected String entityName[];
    protected char[] entityNameBuf[];
    protected int entityNameHash[];
    protected char[] entityReplacementBuf[];
    protected String entityReplacement[];


    protected void ensureEntityCapacity() {
        int entitySize = entityReplacementBuf != null ? entityReplacementBuf.length : 0;
        if(entityEnd >= entitySize) {
            int newSize = entityEnd > 7 ? 2 * entityEnd : 8; // = lucky 7 + 1 //25
            if(TRACE_SIZING) {
                System.err.println("entitySize "+entitySize+" ==> "+newSize);
            }
            String[] newEntityName = new String[newSize];
            char[] newEntityNameBuf[] = new char[newSize][];
            String[] newEntityReplacement = new String[newSize];
            char[] newEntityReplacementBuf[] = new char[newSize][];
            if(entityName != null) {
                System.arraycopy(entityName, 0, newEntityName, 0, entityEnd);
                System.arraycopy(entityReplacementBuf, 0, newEntityReplacement, 0, entityEnd);
                System.arraycopy(entityReplacement, 0, newEntityReplacement, 0, entityEnd);
                System.arraycopy(entityReplacementBuf, 0, newEntityReplacementBuf, 0, entityEnd);
            }
            entityName = newEntityName;
            entityNameBuf = newEntityNameBuf;
            entityReplacement = newEntityReplacement;
            entityReplacementBuf = newEntityReplacementBuf;

            if( ! allStringsInterned ) {
                int[] newEntityNameHash = new int[newSize];
                if(entityNameHash != null) {
                    System.arraycopy(entityNameHash, 0, newEntityNameHash, 0, entityEnd);
                }
                entityNameHash = newEntityNameHash;
            }
        }
    }

    // input buffer management
    protected static final int READ_CHUNK_SIZE = 8*1024; //max data chars in one read() call
    protected Reader reader;
    protected String inputEncoding;


    protected int bufLoadFactor = 95;  // 99%
    //protected int bufHardLimit;  // only matters when expanding

    protected char buf[] = new char[
        Runtime.getRuntime().freeMemory() > 1000000L ? READ_CHUNK_SIZE : 256 ];
    protected int bufSoftLimit = ( bufLoadFactor * buf.length ) /100; // desirable size of buffer


    protected int bufAbsoluteStart; // this is buf
    protected int bufStart;
    protected int bufEnd;
    protected int pos;
    protected int posStart;
    protected int posEnd;

    protected char pc[] = new char[
        Runtime.getRuntime().freeMemory() > 1000000L ? READ_CHUNK_SIZE : 64 ];
    protected int pcStart;
    protected int pcEnd;


    // parsing state
    //protected boolean needsMore;
    //protected boolean seenMarkup;
    protected boolean usePC;


    protected boolean seenStartTag;
    protected boolean seenEndTag;
    protected boolean pastEndTag;
    protected boolean seenAmpersand;
    protected boolean seenMarkup;
    protected boolean seenDocdecl;

    // transient variable set during each call to next/Token()
    protected boolean tokenize;
    protected String text;
    protected String entityRefName;


    private void reset() {
        //System.out.println("reset() called");
        lineNumber = 1;
        columnNumber = 0;
        seenRoot = false;
        reachedEnd = false;
        eventType = XMLStreamConstants.START_DOCUMENT;
        emptyElementTag = false;

        depth = 0;

        attributeCount = 0;

        namespaceEnd = 0;
        localNamespaceEnd = 0;

        entityEnd = 0;

        reader = null;
        inputEncoding = null;

        bufAbsoluteStart = 0;
        bufEnd = bufStart = 0;
        pos = posStart = posEnd = 0;

        pcEnd = pcStart = 0;

        usePC = false;

        seenStartTag = false;
        seenEndTag = false;
        pastEndTag = false;
        seenAmpersand = false;
        seenMarkup = false;
        seenDocdecl = false;
        resetStringCache();

    }


    public MXParser() {
    }

    /**
     * Method setFeature
     *
     * @param    name                a  String
     * @param    state               a  boolean
     *
     * @throws   XMLStreamException
     *
     */
    public void setFeature(String name,
                           boolean state) throws XMLStreamException
    {
        if(name == null) throw new IllegalArgumentException("feature name should not be nulll");
        if(FEATURE_PROCESS_NAMESPACES.equals(name)) {
            if(eventType != XMLStreamConstants.START_DOCUMENT) throw new XMLStreamException(
                    "namespace processing feature can only be changed before parsing",
                    getLocation());
            processNamespaces = state;
            //        } else if(FEATURE_REPORT_NAMESPACE_ATTRIBUTES.equals(name)) {
            //      if(type != XMLStreamConstants.START_DOCUMENT) throw new XMLStreamException(
            //              "namespace reporting feature can only be changed before parsing",
            // getLineNumber(), getColumnNumber(), getPositionDescription(), null);
            //            reportNsAttribs = state;
        } else if(FEATURE_NAMES_INTERNED.equals(name)) {
            if(state != false) {
                throw new XMLStreamException(
                    "interning names in this implementation is not supported");
            }
        } else if(FEATURE_PROCESS_DOCDECL.equals(name)) {
            if(state != false) {
                throw new XMLStreamException(
                    "processing DOCDECL is not supported");
            }
            //} else if(REPORT_DOCDECL.equals(name)) {
            //    paramNotifyDoctype = state;
        } else if(FEATURE_XML_ROUNDTRIP.equals(name)) {
            if(state == false) {
                throw new XMLStreamException(
                    "roundtrip feature can not be switched off");
            }
        } else {
            throw new XMLStreamException("unknown feature "+name);
        }
    }

    /** Unknown properties are <string>always</strong> returned as false */
    public boolean getFeature(String name)
    {
        if(name == null) throw new IllegalArgumentException("feature name should not be nulll");
        if(FEATURE_PROCESS_NAMESPACES.equals(name)) {
            return processNamespaces;
            //        } else if(FEATURE_REPORT_NAMESPACE_ATTRIBUTES.equals(name)) {
            //            return reportNsAttribs;
        } else if(FEATURE_NAMES_INTERNED.equals(name)) {
            return false;
        } else if(FEATURE_PROCESS_DOCDECL.equals(name)) {
            return false;
            //} else if(REPORT_DOCDECL.equals(name)) {
            //    return paramNotifyDoctype;
        } else if(FEATURE_XML_ROUNDTRIP.equals(name)) {
            return true; //roundtripSupported;
        }
        return false;
    }

    public void setProperty(String name,
                            Object value)
        throws XMLStreamException
    {
        throw new XMLStreamException("unsupported property: '"+name+"'");
    }


  public boolean checkForXMLDecl()
    throws XMLStreamException
  {
    try {
      BufferedReader breader = new BufferedReader(reader,7);
      reader = breader;
      breader.mark(7);
      if (breader.read() == '<' &&
          breader.read() == '?' &&
          breader.read() == 'x' &&
          breader.read() == 'm' &&
          breader.read() == 'l') {
        breader.reset();
        return true;
      }
      breader.reset();
      return false;
    } catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

    public void setInput(Reader in)
      throws XMLStreamException
    {
        reset();
        reader = in;
        if(checkForXMLDecl()) {
          next();
        }
    }
  public void setInput(java.io.InputStream in)
    throws XMLStreamException
    {
      try {
      setInput(com.bea.xml.stream.reader.XmlReader.createReader(in));
      } catch (Exception e) {
        throw new XMLStreamException(e);
      }
    }

    public void setInput(java.io.InputStream inputStream, String inputEncoding)
        throws XMLStreamException
    {
        if(inputStream == null) {
            throw new IllegalArgumentException("input stream can not be null");
        }
        Reader reader;
        if(inputEncoding != null) {
            try {
                if(inputEncoding != null) {
                    reader = new InputStreamReader(inputStream, inputEncoding);
                } else {
                    reader = new InputStreamReader(inputStream);
                }
            } catch (UnsupportedEncodingException une) {
                throw new XMLStreamException(
                    "could not create reader for encoding "+inputEncoding+" : "+une,
                    getLocation(), une);
            }
        } else {
            reader = new InputStreamReader(inputStream);
        }
        setInput(reader);
        //must be  here as reest() was called in setInput() and has set this.inputEncoding to null ...
        this.inputEncoding = inputEncoding;
    }

    public String getInputEncoding() {
        return inputEncoding;
    }

    public void defineEntityReplacementText(String entityName,
                                            String replacementText)
        throws XMLStreamException
    {
        //      throw new XMLStreamException("not allowed");

        //protected char[] entityReplacement[];
        ensureEntityCapacity();

        // this is to make sure that if interning works we wil take advatage of it ...
        this.entityName[entityEnd] = newString(entityName.toCharArray(), 0, entityName.length());
        entityNameBuf[entityEnd] = entityName.toCharArray();

        entityReplacement[entityEnd] = replacementText;
        /* 06-Nov-2004, TSa: Null is apparently returned for external
         *   entities (including parsed ones); to prevent an NPE, let's
         *   just use a shared dummy array... (could use null too?)
         */
        char[] ch = (replacementText == null) ? NO_CHARS : replacementText.toCharArray();
        entityReplacementBuf[entityEnd] = ch;
        if(!allStringsInterned) {
            entityNameHash[ entityEnd ] =
                fastHash(entityNameBuf[entityEnd], 0, entityNameBuf[entityEnd].length);
        }
        ++entityEnd;
        //TODO disallow < or & in entity replacement text (or ]]>???)
        // TOOD keepEntityNormalizedForAttributeValue cached as well ...
    }


  public int getNamespaceCount()
  {
    if (!isElementEvent(eventType)) {
        throw new IllegalStateException("Current state not START_ELEMENT or END_ELEMENT");
    }
    return getNamespaceCount(depth);
  }

  public int getNamespaceCount(int depth)
  {
    if(processNamespaces == false || depth == 0) {
      return 0;
    }
    //int maxDepth = eventType == XMLStreamConstants.END_ELEMENT ? this.depth + 1 : this.depth;
    //if(depth < 0 || depth > maxDepth) throw new IllegalArgumentException(
    if(depth < 0) throw new IllegalArgumentException("namespace count may be 0.."+this.depth+" not "+depth);
    return elNamespaceCount[ depth ]-elNamespaceCount[depth-1];
  }
  
  public String getNamespacePrefix(int pos)
  {
    if (!isElementEvent(eventType)) {
        throw new IllegalStateException("Current state not START_ELEMENT or END_ELEMENT");
    }
    int currentDepth = depth;
    int end = getNamespaceCount(currentDepth);//eventType == XMLStreamConstants.END_ELEMENT ? elNamespaceCount[ depth + 1 ] : namespaceEnd;
    int newpos = pos + elNamespaceCount[currentDepth-1];
    if(pos < end) {
      return namespacePrefix[ newpos ];
    } else {
      throw new ArrayIndexOutOfBoundsException(
                                               "position "+pos+" exceeded number of available namespaces "+end);
    }
  }

  public String getNamespaceURI(int pos)
  {
    if (!isElementEvent(eventType)) {
        throw new IllegalStateException("Current state not START_ELEMENT or END_ELEMENT");
    }
    int currentDepth = depth;
    int end = getNamespaceCount(currentDepth); //eventType == XMLStreamConstants.END_ELEMENT ? elNamespaceCount[ depth + 1 ] : namespaceEnd;
    int newpos = pos + elNamespaceCount[currentDepth-1];
    if(pos < end) {
        return namespaceUri[ newpos ];
    } else {
         throw new ArrayIndexOutOfBoundsException(
                                              "position "+pos+" exceedded number of available namespaces "+end);
    }
  }

    public String getNamespaceURI( String prefix )
        //throws XMLStreamException
    {
        if (!isElementEvent(eventType)) {
            throw new IllegalStateException("Current state not START_ELEMENT or END_ELEMENT");
        }
        //int count = namespaceCount[ depth ];
        if(prefix != null && !"".equals(prefix)) {
            for( int i = namespaceEnd -1; i >= 0; i--) {
                if( prefix.equals( namespacePrefix[ i ] ) ) {
                    return namespaceUri[ i ];
                }
            }
            if("xml".equals( prefix )) {
                return XMLConstants.XML_NS_URI;
            } else if("xmlns".equals( prefix )) {
                return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
            }
        } else {
            for( int i = namespaceEnd -1; i >= 0; i--) {
                if( namespacePrefix[ i ]  == null ) {
                    return namespaceUri[ i ];
                }
            }

        }
        return null;
    }
 
    public int getDepth()
    {
        return depth;
    }


    private static int findFragment(int bufMinPos, char[] b, int start, int end) {
        //System.err.println("bufStart="+bufStart+" b="+printable(new String(b, start, end - start))+" start="+start+" end="+end);
        if(start < bufMinPos) {
            start = bufMinPos;
            if(start > end) start = end;
            return start;
        }
        if(end - start > 65) {
            start = end - 10; // try to find good location
        }
        int i = start + 1;
        while(--i > bufMinPos) {
            if((end - i) > 65) break;
            char c = b[i];
            if(c == '<' && (start - i) > 10) break;
        }
        return i;
    }


    /**
     * Return string describing current position of parsers as
     * text 'STATE [seen %s...] @line:column'.
     */
    public String getPositionDescription ()
    {
        String fragment = null;
        if(posStart <= pos) {
            int start = findFragment(0, buf, posStart, pos);
            //System.err.println("start="+start);
            if(start < pos) {
                fragment = new String(buf, start, pos - start);
            }
            if(bufAbsoluteStart > 0 || start > 0) fragment = "..." + fragment;
        }
        //        return " at line "+tokenizerPosRow
        //            +" and column "+(tokenizerPosCol-1)
        //            +(fragment != null ? " seen "+printable(fragment)+"..." : "");
        return " "+//TYPES[ eventType ] +
            (fragment != null ? " seen "+printable(fragment)+"..." : "")+
            " @"+getLineNumber()+":"+getColumnNumber();
    }

    public int getLineNumber()
    {
        return lineNumber;
    }

    public int getColumnNumber()
    {
        return columnNumber;
    }

  public String getLocationURI() { return null; }

    public boolean isWhiteSpace()
    // throws XMLStreamException
    {
        if(eventType == XMLStreamConstants.CHARACTERS || eventType == XMLStreamConstants.CDATA) {
            if(usePC) {
                for (int i = pcStart; i <pcEnd; i++)
                {
                    if(!isS(pc[ i ])) return false;
                }
                return true;
            } else {
                for (int i = posStart; i <posEnd; i++)
                {
                    if(!isS(buf[ i ])) return false;
                }
                return true;
            }
        } else if(eventType == /* XMLStreamConstants.IGNORABLE_WHITESPACE */ XMLStreamConstants.SPACE) {
            return true;

        // COMMENT - cwitt : our interface doesn't define this
        // (and is 'meant to be slightly different anyway' - quote cfry)
        // throw new XMLStreamException("no content available to check for whitespaces");

        } else {
            return false;
        }
    }
  
    public String getText()
    {
        checkTextEvent();

        /*
        if(eventType == XMLStreamConstants.START_DOCUMENT || eventType == XMLStreamConstants.END_DOCUMENT) {
            //throw new XMLStreamException("no content available to read");
            //      if(roundtripSupported) {
            //          text = new String(buf, posStart, posEnd - posStart);
            //      } else {
            return null;
            //      }
        } else
        */
        if(eventType == XMLStreamConstants.ENTITY_REFERENCE) {
            return text;
        }
        if(usePC) {
          text = new String(pc, pcStart, pcEnd - pcStart);
        } else {
          text = new String(buf, posStart, posEnd - posStart);
        }
        return text;
    }

    public String getNamespaceURI()
    {
        if(eventType == XMLStreamConstants.START_ELEMENT ||
           eventType == XMLStreamConstants.END_ELEMENT) {
            return processNamespaces ? elUri[ depth  ] : NO_NAMESPACE;
        }
        return null;
    }

    public String getLocalName()
    {
        if(eventType == XMLStreamConstants.START_ELEMENT) {
            //return elName[ depth - 1 ] ;
            return elName[ depth ] ;
        } else if(eventType == XMLStreamConstants.END_ELEMENT) {
            return elName[ depth ] ;
        } else if(eventType == XMLStreamConstants.ENTITY_REFERENCE) {
            if(entityRefName == null) {
                entityRefName = newString(buf, posStart, posEnd - posStart);
            }
            return entityRefName;
        }
        /* TSa, 28-Oct-2004: StAX specs: need to throw IllegalStateException
         * here...
         */
        throw new IllegalStateException("Current state ("+eventTypeDesc(eventType)+") not START_ELEMENT, END_ELEMENT or ENTITY_REFERENCE");
    }

    public String getPrefix()
    {
        if(eventType == XMLStreamConstants.START_ELEMENT ||
           eventType == XMLStreamConstants.END_ELEMENT) {
            return elPrefix[ depth ] ;
        }
        return null;
     }


    public boolean isEmptyElementTag() throws XMLStreamException
    {
        if(eventType != XMLStreamConstants.START_ELEMENT) throw new XMLStreamException(
                "parser must be on XMLStreamConstants.START_ELEMENT to check for empty element",
                getLocation());
        return emptyElementTag;
    }

    public int getAttributeCount()
    {
        if(eventType != XMLStreamConstants.START_ELEMENT) {
            // As per specs, needs to throw an exception
            throw new IllegalStateException("Current state not START_ELEMENT");
        }
        return attributeCount;
    }

    public String getAttributeNamespace(int index)
    {
        if(eventType != XMLStreamConstants.START_ELEMENT) {
            throw new IllegalStateException("only START_ELEMENT can have attributes");
        }
        if(processNamespaces == false) return NO_NAMESPACE;
        if(index < 0 || index >= attributeCount) throw new IndexOutOfBoundsException(
                "attribute position must be 0.."+(attributeCount-1)+" and not "+index);
        return attributeUri[ index ];
    }

    public String getAttributeLocalName(int index)
    {
        if(eventType != XMLStreamConstants.START_ELEMENT) {
            throw new IllegalStateException("only START_ELEMENT can have attributes");
        }
        if(index < 0 || index >= attributeCount) throw new IndexOutOfBoundsException(
                "attribute position must be 0.."+(attributeCount-1)+" and not "+index);
        return attributeName[ index ];
    }

    public String getAttributePrefix(int index)
    {
        if(eventType != XMLStreamConstants.START_ELEMENT) {
            throw new IllegalStateException("only START_ELEMENT can have attributes");
        }
        if(processNamespaces == false) return null;
        if(index < 0 || index >= attributeCount) throw new IndexOutOfBoundsException(
                "attribute position must be 0.."+(attributeCount-1)+" and not "+index);
        return attributePrefix[ index ];
    }

    public String getAttributeType(int index) {
        if(eventType != XMLStreamConstants.START_ELEMENT) {
            throw new IllegalStateException("only START_ELEMENT can have attributes");
        }
        if(index < 0 || index >= attributeCount) throw new IndexOutOfBoundsException(
                "attribute position must be 0.."+(attributeCount-1)+" and not "+index);
        return "CDATA";
    }

    public boolean isAttributeSpecified(int index) {
        if(eventType != XMLStreamConstants.START_ELEMENT) {
            throw new IllegalStateException("only START_ELEMENT can have attributes");
        }
        if(index < 0 || index >= attributeCount) throw new IndexOutOfBoundsException(
                "attribute position must be 0.."+(attributeCount-1)+" and not "+index);
        return true;
    }

    public String getAttributeValue(int index)
    {
        if(eventType != XMLStreamConstants.START_ELEMENT) {
            throw new IllegalStateException("only START_ELEMENT can have attributes");
        }
        if(index < 0 || index >= attributeCount) throw new IndexOutOfBoundsException(
                "attribute position must be 0.."+(attributeCount-1)+" and not "+index);
        return attributeValue[ index ];
    }

    public String getAttributeValue(String namespace,
                                    String name)
    {
        if(eventType != XMLStreamConstants.START_ELEMENT) {
            throw new IllegalStateException("only START_ELEMENT can have attributes");
        }
        if(name == null) {
            throw new IllegalArgumentException("attribute name can not be null");
        }
        // TODO make check if namespace is interned!!! etc. for names!!!
        if(namespace != null) {
            for(int i = 0; i < attributeCount; ++i) {
                if(namespace.equals(attributeUri[i])
                   && name.equals(attributeName[i]))
                {
                    return attributeValue[i];
                }
            }
        } else {
            for(int i = 0; i < attributeCount; ++i) {
                if(name.equals(attributeName[i]))
                {
                    return attributeValue[i];
                }
            }
        }
        return null;
    }
  
 

    public int getEventType() {
        return eventType;
    }

    public void require(int type, String namespace, String name)
        throws XMLStreamException
    {
        int currType = getEventType();
        boolean ok = (type == currType);

        if (ok && name != null) {
            if (currType == START_ELEMENT || currType == END_ELEMENT
                || currType == ENTITY_REFERENCE) {
                ok = name.equals(getLocalName());
            } else {
                throw new XMLStreamException("Using non-null local name argument for require(); "
                                             +ElementTypeNames.getEventTypeString(currType)
                                             +" event does not have local name",
                                             getLocation());
            }
        }

        if (ok && namespace != null) {
            if (currType == START_ELEMENT || currType == START_ELEMENT) {
                ok = namespace.equals(getNamespaceURI());
            }
        }

        if (!ok) {
            throw new XMLStreamException (
                "expected event "+ElementTypeNames.getEventTypeString(type)
                    +(name != null ? " with name '"+name+"'" : "")
                    +(namespace != null && name != null ? " and" : "")
                    +(namespace != null ? " with namespace '"+namespace+"'" : "")
                    +" but got"
                    +(type != getEventType() ? " "+ElementTypeNames.getEventTypeString(getEventType()) : "")
                    +(name != null && getLocalName() != null && !name.equals (getName ())
                          ? " name '"+getLocalName()+"'" : "")
                    +(namespace != null && name != null
                          && getLocalName() != null && !name.equals (getName ())
                          && getNamespaceURI() != null && !namespace.equals (getNamespaceURI())
                          ? " and" : "")
                    +(namespace != null && getNamespaceURI() != null && !namespace.equals (getNamespaceURI())
                          ? " namespace '"+getNamespaceURI()+"'" : "")
                    +(" (position:"+ getPositionDescription())+")",
                getLocation());
        }
    }

    public String nextText() throws XMLStreamException
    {
        if(getEventType() != XMLStreamConstants.START_ELEMENT) {
            throw new XMLStreamException(
                "parser must be on START_ELEMENT to read next text",
                getLocation());
        }
        int eventType = next();
        if(eventType == XMLStreamConstants.CHARACTERS) {
            String result = getText();
            eventType = next();
            if(eventType != XMLStreamConstants.END_ELEMENT) {
                throw new XMLStreamException(
                    "TEXT must be immediately followed by END_ELEMENT and not "
                    +ElementTypeNames.getEventTypeString(getEventType()),
                    getLocation());
            }
            return result;
        } else if(eventType == XMLStreamConstants.END_ELEMENT) {
            return "";
        } else {
            throw new XMLStreamException(
                "parser must be on START_ELEMENT or TEXT to read text",
                getLocation());
        }
    }

  public int nextTag()
    throws XMLStreamException
  {
    next();
    // Skip white space, comments and processing instructions:
    while ((eventType == XMLStreamConstants.CHARACTERS && isWhiteSpace())
           || (eventType == XMLStreamConstants.CDATA && isWhiteSpace())
           // skip whitespace
           || eventType == XMLStreamConstants.SPACE
           || eventType == XMLStreamConstants.COMMENT
           || eventType == XMLStreamConstants.PROCESSING_INSTRUCTION) {
        next();
    }
    if (eventType != XMLStreamConstants.START_ELEMENT && eventType != XMLStreamConstants.END_ELEMENT) {
      throw new XMLStreamException("expected XMLStreamConstants.START_ELEMENT or XMLStreamConstants.END_ELEMENT not "
                                   +ElementTypeNames.getEventTypeString(getEventType()),
                                   getLocation());
    }
    return eventType;
  }

  public String getElementText()
    throws XMLStreamException
  {
    StringBuffer buf = new StringBuffer();
    if(getEventType() != START_ELEMENT)
      throw new XMLStreamException(
        "Precondition for readText is getEventType() == START_ELEMENT");
    do {
      if(next() == END_DOCUMENT)
        throw new XMLStreamException("Unexpected end of Document");
      if(isStartElement())
        throw new XMLStreamException("Unexpected Element start");
      if(isCharacters() || getEventType() == XMLStreamConstants.ENTITY_REFERENCE)
        buf.append(getText());
    } while(!isEndElement());
    return buf.toString();
  }

  public int next() throws XMLStreamException {
    tokenize = true;
    pcEnd = pcStart = 0;
    usePC = false;
    return nextImpl();
  }
  
  public int nextToken() throws XMLStreamException {
    tokenize = true;
    return nextImpl();
  }
  
  public int nextElement() throws XMLStreamException {
    return nextTag();
  }
  
  public boolean hasNext() throws XMLStreamException {
    return !(eventType == XMLStreamConstants.END_DOCUMENT);
  }

  public void skip() throws XMLStreamException {
    nextToken();
  }

  public void close() throws XMLStreamException {

  }
  
  public boolean isStartElement() {
    return (eventType == XMLStreamConstants.START_ELEMENT);
  }
  
  public boolean isEndElement() {
    return (eventType == XMLStreamConstants.END_ELEMENT);
  }
  
  public boolean isCharacters() {
    return (eventType == XMLStreamConstants.CHARACTERS);
  }
  
  public boolean isEOF() {
    return (eventType == XMLStreamConstants.END_DOCUMENT);
  }
  
  public boolean moveToStartElement() throws XMLStreamException {
    if (isStartElement()) return true;
    while(hasNext()) {
      if (isStartElement()) return true;
      else
        next();
    }
    return false;
  }
  
  public boolean moveToStartElement(String localName)
    throws XMLStreamException
  {
    if (localName == null) return false;
    while( moveToStartElement() ) {
      if (localName.equals(getLocalName())) return true;
      if (!hasNext()) return false;
      next();
    }
    return false;
  }
  
  public boolean moveToStartElement(String localName, String namespaceUri)
    throws XMLStreamException
  {
    if (localName == null || namespaceUri == null) return false;
    while(moveToStartElement(localName)) {
      if(namespaceUri.equals(getNamespaceURI())) return true;
      if (!hasNext()) return false;
      next();
    }
    return false;
  }
  
  public boolean moveToEndElement() throws XMLStreamException {
    if (isEndElement()) return true;
    while (hasNext()) {
      if (isEndElement()) return true;
      else
        next();
    }
    return false;
  }
  
  public boolean moveToEndElement(String localName)
    throws XMLStreamException
  {
    if (localName == null) return false;
    while( moveToEndElement() ) {
      if (localName.equals(getLocalName())) return true;
      if (!hasNext()) return false;
      next();
    }
    return false;
  }
  
  public boolean moveToEndElement(String localName, String namespaceUri)
    throws XMLStreamException
  {
    if (localName == null || namespaceUri == null) return false;
    while(moveToEndElement(localName)) {
      if(namespaceUri.equals(getNamespaceURI())) return true;
      if (!hasNext()) return false;
      next();
    }
    return false;
  }
  
  public boolean hasAttributes() {
    if (getAttributeCount() > 0)
      return true;
    return false;
  }
  
  public boolean hasNamespaces() {
    if(getNamespaceCount() > 0)
      return true;
    return false;
  }

  public Iterator getAttributes() {
    if (!hasAttributes()) return EmptyIterator.emptyIterator;
    int attributeCount = getAttributeCount();
    ArrayList atts = new ArrayList();
    for (int i = 0; i < attributeCount; i++){
      atts.add(new AttributeBase(getAttributePrefix(i),
                                 getAttributeNamespace(i),
                                 getAttributeLocalName(i),
                                 getAttributeValue(i),
                                 getAttributeType(i)));
    }
    return atts.iterator();
  }

  public Iterator internalGetNamespaces(int depth,
                                                 int namespaceCount) {
    ArrayList ns = new ArrayList();
    int startNs = elNamespaceCount[ depth - 1 ];
    for (int i = 0; i < namespaceCount; i++){
      String prefix = getLocalNamespacePrefix(i+startNs);
      if(prefix == null){
        ns.add(new NamespaceBase(getLocalNamespaceURI(i+startNs)));
      } else {
        ns.add(new NamespaceBase(prefix,
                                 getLocalNamespaceURI(i+startNs)));
      }
    }
    return ns.iterator();
  }


  public Iterator getNamespaces() {
    if (!hasNamespaces()) return EmptyIterator.emptyIterator;
    int namespaceCount = getLocalNamespaceCount();
    return internalGetNamespaces(depth,namespaceCount);
  }

  public Iterator getOutOfScopeNamespaces() {
    int startNs = elNamespaceCount[ depth-1  ];
    int endNs = elNamespaceCount[depth];
    int namespaceCount = endNs-startNs;
    return internalGetNamespaces(depth,
                                 namespaceCount);
  }
  
  public XMLStreamReader subReader() throws XMLStreamException {
    return new SubReader(this);
  }
  
  public void recycle() throws XMLStreamException {
    reset();
  }
  
  public Reader getTextStream() {
    throw new UnsupportedOperationException();
  }

    private void checkTextEvent() {
        if (!hasText()) {
            throw new IllegalStateException("Current state ("+eventTypeDesc(eventType)+") does not have textual content");
        }
    }

  public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length)
    throws XMLStreamException
  {
      checkTextEvent();

    if (getTextStart()+sourceStart >= getTextLength())
      throw new ArrayIndexOutOfBoundsException();
    int numCopy;
    if (getTextStart()+sourceStart+length < getTextLength())
      numCopy = length;
    else
      numCopy = getTextLength() - (getTextStart()+sourceStart);
    System.arraycopy(getTextCharacters(), getTextStart() + sourceStart, target, targetStart, numCopy);
    return numCopy;
  }

  public char[] getTextCharacters() {
    checkTextEvent();

    if( eventType == XMLStreamConstants.CHARACTERS ) {
      if(usePC) {
        return pc;
      } else {
        return buf;
      }
    }
    return buf;
  }

  
  public int getTextStart() {
    checkTextEvent();
    if(usePC) {
      return pcStart;
    } else {
      return posStart;
    }
  }

  public int getTextLength() {
    checkTextEvent();
    if(usePC) {
      return pcEnd - pcStart;
    } else {
      return  posEnd - posStart;
    }
  }

  public boolean hasText() {
      /*
    return (0 != (eventType & (XMLStreamConstants.CHARACTERS |
                               XMLStreamConstants.DTD |
                               XMLStreamConstants.COMMENT |
                               XMLStreamConstants.ENTITY_REFERENCE)));
      */
    return (eventType == XMLStreamConstants.CHARACTERS
            || eventType == XMLStreamConstants.DTD
            || eventType == XMLStreamConstants.COMMENT
            || eventType == XMLStreamConstants.SPACE
            || eventType == XMLStreamConstants.ENTITY_REFERENCE);
  }
  
  public String getValue() {
    return getText();
  }
  
  public String getEncoding() {
    return getInputEncoding();
  }
  
  public int getCharacterOffset() {
    // TODO - cwitt : which buffer are we using?
    return posEnd;
  }

  private static final String checkNull(String s) {
    if (s != null) return s;
    else return "";
  }

  private static String eventTypeDesc(int type) {
      return (type < 0 || type >= TYPES.length) ? "[UNKNOWN]" : TYPES[type];
  }

  private static boolean isElementEvent(int type) {
      return type ==  XMLStreamConstants.START_ELEMENT
          || type == XMLStreamConstants.END_ELEMENT;
  }

  public QName getAttributeName(int index) {
    if (!isElementEvent(eventType)) {
        throw new IllegalStateException("Current state ("+eventTypeDesc(eventType)+") not START_ELEMENT or END_ELEMENT");
    }
    return new QName(checkNull(getAttributeNamespace(index)),
                     getAttributeLocalName(index),
                     checkNull(getAttributePrefix(index)));
  }



  public QName getName() {
    /* getLocalName() would verify event type, but it also allows
     * ENTITY_REFERENCE, which is not allowed here
     */
    if (!isElementEvent(eventType)) {
        throw new IllegalStateException("Current state not START_ELEMENT or END_ELEMENT");
    }
    return new QName(checkNull(getNamespaceURI()),
                     getLocalName(),
                     checkNull(getPrefix()));
  }
  
  public boolean hasName() {
        /*
    return (0 != (eventType  & (XMLStreamConstants.START_ELEMENT
                            | XMLStreamConstants.END_ELEMENT
                            | XMLStreamConstants.ENTITY_REFERENCE)));
        */
      // StAX specs indicate ENTITY_REFERENCE should return false
      return isElementEvent(eventType);
  }
  
  public String getVersion() {
    return xmlVersion;
  }
  
  public boolean isStandalone() {
    return standalone;
  }
     public boolean standaloneSet() {
    return standaloneSet;
  }

  public String getCharacterEncodingScheme() {
    return charEncodingScheme;
  }
  
  // COMMENT - cwitt : end of added XMLStreamReader impl
  
  protected int nextImpl() throws XMLStreamException
  {
    try {
      
      text = null;
      bufStart = posEnd;
      if(pastEndTag) {
        pastEndTag = false;
        --depth;
        namespaceEnd = elNamespaceCount[ depth ]; // less namespaces available
      }
      if(emptyElementTag) {
        emptyElementTag = false;
        pastEndTag = true;
        return eventType = XMLStreamConstants.END_ELEMENT;
      }

      // [1] document ::= prolog element Misc*
      if(depth > 0) {

        if(seenStartTag) {
          seenStartTag = false;
          return eventType = parseStartTag();
        }
        if(seenEndTag) {
          seenEndTag = false;
          return eventType = parseEndTag();
        }

        // ASSUMPTION: we are _on_ first character of content or markup!!!!
        // [43] content ::= CharData? ((element | Reference | XMLStreamConstants.CDATA | PI | XMLStreamConstants.COMMENT) CharData?)*
        char ch;
        if(seenMarkup) {  // we have read ahead ...
          seenMarkup = false;
          ch = '<';
        } else if(seenAmpersand) {
          seenAmpersand = false;
          ch = '&';
        } else {
          ch = more();
        }
        posStart = pos - 1; // VERY IMPORTANT: this is correct start of event!!!

        // when true there is some potential event TEXT to return - keep gathering
        boolean hadCharData = false;

        // when true TEXT data is not continous (like <![CDATA[text]]>) and requires PC merging
        boolean needsMerging = false;

      MAIN_LOOP:
        while(true) {
          // work on MARKUP
          if(ch == '<') {
            if(hadCharData) {
              //posEnd = pos - 1;
              if(tokenize) {
                seenMarkup = true;
                return eventType = XMLStreamConstants.CHARACTERS;
              }
            }
            ch = more();
            if(ch == '/') {
              if(!tokenize && hadCharData) {
                seenEndTag = true;
                //posEnd = pos - 2;
                return eventType = XMLStreamConstants.CHARACTERS;
              }
              return eventType = parseEndTag();
            } else if(ch == '!') {
              ch = more();
              if(ch == '-') {
                // note: if(tokenize == false) posStart/End is NOT changed!!!!
                parseComment();
                if(tokenize) return eventType = XMLStreamConstants.COMMENT;
                if( !usePC && hadCharData ) needsMerging = true;
              } else if(ch == '[') {
                //posEnd = pos - 3;
                // must remeber previous posStart/End as it merges with content of CDATA
                int oldStart = posStart;
                int oldEnd = posEnd;
                parseCDATA();

                int cdStart = posStart;
                int cdEnd = posEnd;
                posStart = oldStart;
                posEnd = oldEnd;
                int cdLen = cdEnd - cdStart;
                if(cdLen > 0) { // was there anything insdie CDATA section?
                  if(hadCharData) {
                    // do merging if there was anything in XMLStreamConstants.CDATA!!!!
                    if(!usePC) {
                      // posEnd is correct already!!!
                      if(posEnd > posStart) {
                        joinPC();
                      } else {
                        usePC = true;
                        pcStart = pcEnd = 0;
                      }
                    }
                    if(pcEnd + cdLen >= pc.length) ensurePC(pcEnd + cdLen);
                    // copy [cdStart..cdEnd) into PC
                    System.arraycopy(buf, cdStart, pc, pcEnd, cdLen);
                    pcEnd += cdLen;
                  } else {
                    needsMerging = true;
                    posStart = cdStart;
                    posEnd = cdEnd;
                  }
                  hadCharData = true;
                } else {
                  if( !usePC && hadCharData ) needsMerging = true;
                }
                //                if(tokenize) return eventType = XMLStreamConstants.CDATA;
              } else {
                throw new XMLStreamException(
                                             "unexpected character in markup "+printable(ch),
                                             getLocation());
              }

            } else if(ch == '?') {
              parsePI();
              if(tokenize) return eventType = XMLStreamConstants.PROCESSING_INSTRUCTION;
              if( !usePC && hadCharData ) needsMerging = true;
            } else if( isNameStartChar(ch) ) {
              if(!tokenize && hadCharData) {
                seenStartTag = true;
                //posEnd = pos - 2;
                return eventType = XMLStreamConstants.CHARACTERS;
              }
              return eventType = parseStartTag();
            } else {
              throw new XMLStreamException(
                                           "unexpected character in markup "+printable(ch),
                                           getLocation());
            }
            // do content comapctation if it makes sense!!!!

          } else if(ch == '&') {
            // work on ENTITTY
            //posEnd = pos - 1;
            
            if(tokenize && hadCharData) {
              seenAmpersand = true;
              return eventType = XMLStreamConstants.CHARACTERS;
            }
            
            int oldStart = posStart;
            int oldEnd = posEnd;
            char[] resolvedEntity = parseEntityRef();
            if (!getConfigurationContext().isReplacingEntities())
              return eventType = XMLStreamConstants.ENTITY_REFERENCE;
            eventType = XMLStreamConstants.CHARACTERS;
            // check if replacement text can be resolved !!!
            if(resolvedEntity == null) {
              if(entityRefName == null) {
                entityRefName = newString(buf, posStart, posEnd - posStart);
              }
              throw new XMLStreamException(
                                           "could not resolve entity named '"+printable(entityRefName)+"'",
                                           getLocation());
            }
            //int entStart = posStart;
            //int entEnd = posEnd;
            posStart = oldStart;
            posEnd = oldEnd;
            if(!usePC) {
              if(hadCharData) {
                joinPC(); // posEnd is already set correctly!!!
                needsMerging = false;
              } else {
                usePC = true;
                pcStart = pcEnd = 0;
              }
            }
            //assert usePC == true;
            // write into PC replacement text - do merge for replacement text!!!!
            for (int i = 0; i < resolvedEntity.length; i++)
              {
                if(pcEnd >= pc.length) ensurePC(pcEnd);
                pc[pcEnd++] = resolvedEntity[ i ];

              }
            //assert needsMerging == false;
          } else {

            if(needsMerging) {
              //assert usePC == false;
              joinPC();  // posEnd is already set correctly!!!
              //posStart = pos  -  1;
              needsMerging = false;
            }


            //no MARKUP not ENTITIES so work on character data ...



            // [14] CharData ::=   [^<&]* - ([^<&]* ']]>' [^<&]*)


            hadCharData = true;

            boolean normalizedCR = false;
            // use loop locality here!!!!
            do {

              //if(tokenize == false) {
              if (true) {
                // deal with normalization issues ...
                if(ch == '\r') {
                  normalizedCR = true;
                  posEnd = pos -1;
                                // posEnd is alreadys set
                  if(!usePC) {
                    if(posEnd > posStart) {
                      joinPC();
                    } else {
                      usePC = true;
                      pcStart = pcEnd = 0;
                    }
                  }
                                //assert usePC == true;
                  if(pcEnd >= pc.length) ensurePC(pcEnd);
                  pc[pcEnd++] = '\n';
                } else if(ch == '\n') {
                                //   if(!usePC) {  joinPC(); } else { if(pcEnd >= pc.length) ensurePC(); }
                  if(!normalizedCR && usePC) {
                    if(pcEnd >= pc.length) ensurePC(pcEnd);
                    pc[pcEnd++] = '\n';
                  }
                  normalizedCR = false;
                } else {
                  if(usePC) {
                    if(pcEnd >= pc.length) ensurePC(pcEnd);
                    pc[pcEnd++] = ch;
                  }
                  normalizedCR = false;
                }
              }
              ch = more();
            } while(ch != '<' && ch != '&');
            posEnd = pos - 1;
            continue MAIN_LOOP;  // skip ch = more() from below - we are alreayd ahead ...
          }
          ch = more();
        } // endless while(true)
      } else {
        if(seenRoot) {
          return parseEpilog();
        } else {
          return parseProlog();
        }
      }
        
    } catch(EOFException eofe) {
      throw new XMLStreamException(eofe);
    }
        
  }


  protected int parseProlog()
        throws XMLStreamException
    {
        // [2] prolog: ::= XMLDecl? Misc* (doctypedecl Misc*)? and look for [39] element
        
        try {

        char ch;
        if(seenMarkup) {
            ch = buf[ pos - 1 ];
        } else {
            ch = more();
        }

        if(eventType == XMLStreamConstants.START_DOCUMENT) {
            // bootstrap parsing with getting first character input!
            // deal with BOM
            // detect BOM and frop it (Unicode int Order Mark)
            if(ch == '\uFFFE') {
                throw new XMLStreamException(
                    "first character in input was UNICODE noncharacter (0xFFFE)"+
                        "- input requires int swapping",
                    getLocation());
            }
            if(ch == '\uFEFF') {
                // skipping UNICODE int Order Mark (so called BOM)
                ch = more();
            }
        }
        seenMarkup = false;
        boolean gotS = false;
        posStart = pos - 1;
        while(true) {
            // deal with Misc
            // [27] Misc ::= XMLStreamConstants.COMMENT | PI | S
            // deal with docdecl --> mark it!
            // else parseStartTag seen <[^/]
            if(ch == '<') {
                if(gotS && tokenize) {
                    posEnd = pos - 1;
                    seenMarkup = true;
                    return eventType = /* XMLStreamConstants.IGNORABLE_WHITESPACE */ XMLStreamConstants.SPACE;
                }
                ch = more();
                if(ch == '?') {
                    // check if it is 'xml'
                    // deal with XMLDecl
                    boolean isXMLDecl = parsePI();
                    if(tokenize) {
                      if (isXMLDecl)
                        return eventType = XMLStreamConstants.START_DOCUMENT;
                      return eventType = XMLStreamConstants.PROCESSING_INSTRUCTION;
                    }
                } else if(ch == '!') {
                    ch = more();
                    if(ch == 'D') {
                        if(seenDocdecl) {
                            throw new XMLStreamException(
                                "only one docdecl allowed in XML document",
                                getLocation());
                        }
                        seenDocdecl = true;
                        parseDocdecl();
                        if(tokenize) return eventType = XMLStreamConstants.DTD;
                    } else if(ch == '-') {
                        parseComment();
                        if(tokenize) return eventType = XMLStreamConstants.COMMENT;
                    } else {
                        throw new XMLStreamException(
                            "unexpected markup <!"+printable(ch),
                            getLocation());
                    }
                } else if(ch == '/') {
                    throw new XMLStreamException(
                        "expected start tag name and not "+printable(ch),
                        getLocation());
                } else if(isNameStartChar(ch)) {
                    seenRoot = true;
                    return parseStartTag();
                } else {
                    throw new XMLStreamException(
                        "expected start tag name and not "+printable(ch),
                        getLocation());
                }
            } else if(isS(ch)) {
                gotS = true;
            } else {
                throw new XMLStreamException(
                    "only whitespace content allowed before start tag and not "+printable(ch),
                    getLocation());
            }
            ch = more();
        }

        } catch(EOFException eofe) {
            throw new XMLStreamException(eofe);
        }
    }

    protected int parseEpilog()
        throws XMLStreamException
    {
        if(eventType == XMLStreamConstants.END_DOCUMENT) {
            throw new XMLStreamException(
              "already reached end document",
              getLocation());
        }
        if(reachedEnd) {
            return eventType = XMLStreamConstants.END_DOCUMENT;
        }
        boolean gotS = false;
        try {
            // epilog: Misc*
            char ch;
            if(seenMarkup) {
                ch = buf[ pos - 1 ];
            } else {
                ch = more();
            }
            seenMarkup = false;
            posStart = pos - 1;
            while(true) {
                // deal with Misc
                // [27] Misc ::= XMLStreamConstants.COMMENT | PI | S
                if(ch == '<') {
                    if(gotS && tokenize) {
                        posEnd = pos - 1;
                        seenMarkup = true;
                        return eventType = /* XMLStreamConstants.IGNORABLE_WHITESPACE */ XMLStreamConstants.SPACE;
                    }
                    ch = more();
                    if(ch == '?') {
                        // check if it is 'xml'
                        // deal with XMLDecl
                        parsePI();
                        if(tokenize) return eventType = XMLStreamConstants.PROCESSING_INSTRUCTION;

                    } else if(ch == '!') {
                        ch = more();
                        if(ch == 'D') {
                            parseDocdecl(); //FIXME
                            if(tokenize) return eventType = XMLStreamConstants.DTD;
                        } else if(ch == '-') {
                            parseComment();
                            if(tokenize) return eventType = XMLStreamConstants.COMMENT;
                        } else {
                            throw new XMLStreamException(
                                "unexpected markup <!"+printable(ch),
                                getLocation());
                        }
                    } else if(ch == '/') {
                        throw new XMLStreamException(
                            "end tag not allowed in epilog but got "+printable(ch),
                            getLocation());
                    } else if(isNameStartChar(ch)) {
                        throw new XMLStreamException(
                            "start tag not allowed in epilog but got "+printable(ch),
                            getLocation());
                    } else {
                        throw new XMLStreamException(
                            "in epilog expected ignorable content and not "+printable(ch),
                            getLocation());
                    }
                } else if(isS(ch)) {
                    gotS = true;
                } else {
                    throw new XMLStreamException(
                        "in epilog non whitespace content is not allowed but got "+printable(ch),
                        getLocation());
                }
                ch = more();
            }

            // throw Exceptin("unexpected content in epilog
            // cach EOFException return END_DOCUEMENT
            //try {
        } catch(EOFException ex) {
            reachedEnd = true;
            if(tokenize && gotS) {
                posEnd = pos; // well - this is LAST available character pos
                return eventType = /* XMLStreamConstants.IGNORABLE_WHITESPACE */ XMLStreamConstants.SPACE;
            }
            return eventType = XMLStreamConstants.END_DOCUMENT;
        }
    }


    public int parseEndTag() throws XMLStreamException {
        //ASSUMPTION ch is past "</"
        // [42] ETag ::=  '</' Name S? '>'

        /* 28-Oct-2004, TSa: Let's update this right away; otherwise it's
         *   impossible to call public methods that check that current state
         *   is correct.
         */
        eventType = XMLStreamConstants.END_ELEMENT;

        try {

        char ch = more();
        if(!isNameStartChar(ch)) {
            throw new XMLStreamException(
                "expected name start and not "+printable(ch),
                getLocation());
        }
        posStart = pos - 3;
        int nameStart = pos - 1 + bufAbsoluteStart;
        do {
            ch = more();
        } while(isNameChar(ch));

        // now we go one level down -- do checks
        //--depth;  //FIXME

        // check that end tag name is the same as start tag
        //String name = new String(buf, nameStart - bufAbsoluteStart,
        //                           (pos - 1) - (nameStart - bufAbsoluteStart));
        int last = pos - 1;
        int off = nameStart - bufAbsoluteStart;
        int len = last - off;
        char[] cbuf = elRawName[depth];
        if(elRawNameEnd[depth] != len) {
            // construct strings for exception
            String startname = new String(cbuf, 0, elRawNameEnd[depth]);
            String endname = new String(buf, off, len);
            throw new XMLStreamException(
                "end tag name '"+endname+"' must match start tag name '"+startname+"'",
                getLocation());
        }
        for (int i = 0; i < len; i++)
        {
            if(buf[off++] != cbuf[i]) {
                // construct strings for exception
                String startname = new String(cbuf, 0, len);
                String endname = new String(buf, off - i - 1, len);
                throw new XMLStreamException(
                    "end tag name '"+endname+"' must be the same as start tag '"+startname+"'",
                    getLocation());
            }
        }

        while(isS(ch)) { ch = more(); } // skip additional white spaces
        if(ch != '>') throw new XMLStreamException(
                "expected > to finsh end tag not "+printable(ch),
                getLocation());



        //namespaceEnd = elNamespaceCount[ depth ]; //FIXME

        posEnd = pos;
        pastEndTag = true;
        
        } catch(EOFException eofe) {
            throw new XMLStreamException(eofe);
        }
        
        return XMLStreamConstants.END_ELEMENT;
    }

    public int parseStartTag() throws XMLStreamException {
        //ASSUMPTION ch is past <T
        // [40] STag ::=  '<' Name (S Attribute)* S? '>'
        // [44] EmptyElemTag ::= '<' Name (S Attribute)* S? '/>'

        /* 28-Oct-2004, TSa: Let's update this right away; otherwise it's
         *   impossible to call public methods that check that current state
         *   is correct.
         */
        eventType = XMLStreamConstants.START_ELEMENT;

        try {
        
        ++depth; //FIXME

        posStart = pos - 2;

        emptyElementTag = false;
        attributeCount = 0;
        localNamespaceEnd = 0;
        // retrieve name
        int nameStart = pos - 1 + bufAbsoluteStart;
        int colonPos = -1;
        char ch = buf[ pos - 1];
        if(ch == ':' && processNamespaces) throw new XMLStreamException(
                "when namespaces processing enabled colon can not be at element name start",
                getLocation());
        while(true) {
            ch = more();
            if(!isNameChar(ch)) break;
            if(ch == ':' && processNamespaces) {
                if(colonPos != -1) throw new XMLStreamException(
                        "only one colon is allowed in name of element when namespaces are enabled",
                        getLocation());
                colonPos = pos - 1 + bufAbsoluteStart;
            }
        }

        // retrieve name
        ensureElementsCapacity();


        //TODO check for efficient interning and then use elRawNameInterned!!!!

        int elLen = (pos - 1) - (nameStart - bufAbsoluteStart);
        if(elRawName[ depth ] == null || elRawName[ depth ].length < elLen) {
            elRawName[ depth ] = new char[ 2 * elLen ];
        }
        System.arraycopy(buf, nameStart - bufAbsoluteStart, elRawName[ depth ], 0, elLen);
        elRawNameEnd[ depth ] = elLen;

        String name = null;

        // work on prefixes and namespace URI
        String prefix = null;
        if(processNamespaces) {
            if(colonPos != -1) {
                prefix = elPrefix[ depth ] = newString(buf, nameStart - bufAbsoluteStart,
                                                       colonPos - nameStart);
                name = elName[ depth ] = newString(buf, colonPos + 1 - bufAbsoluteStart,
                                                   //(pos -1) - (colonPos + 1));
                                                   pos - 2 - (colonPos - bufAbsoluteStart));
            } else {
                prefix = elPrefix[ depth ] = null;
                name = elName[ depth ] = newString(buf, nameStart - bufAbsoluteStart, elLen);
            }
        } else {
            name = elName[ depth ] = newString(buf, nameStart - bufAbsoluteStart, elLen);
        }

        while(true) {
            while(isS(ch)) { ch = more(); } // skip additional white spaces
            if(ch == '>') {
                break;
            } else if(ch == '/') {
                if(emptyElementTag) throw new XMLStreamException(
                        "repeated / in tag declaration",
                        getLocation());
                emptyElementTag = true;
                ch = more();
                if(ch != '>') throw new XMLStreamException(
                        "expected > to end empty tag not "+printable(ch), getLocation());
                break;
            } else if(isNameStartChar(ch)) {
                ch = parseAttribute();
                ch = more();
                continue;
            } else {
                throw new XMLStreamException(
                    "start tag unexpected character "+printable(ch),
                    getLocation());
            }
            //ch = more(); // skip space
        }

        // now when namespaces were declared we can resolve them
        if(processNamespaces) {
            String uri = getNamespaceURI(prefix);
            if(uri == null) {
                if(prefix == null) { // no prefix and no uri => use default namespace
                    uri = NO_NAMESPACE;
                } else {
                    throw new XMLStreamException(
                        "could not determine namespace bound to element prefix "+prefix,
                        getLocation());
                }

            }
            elUri[ depth ] = uri;


            //String uri = getNamespaceURI(prefix);
            //if(uri == null && prefix == null) { // no prefix and no uri => use default namespace
            //  uri = "";
            //}
            // resolve attribute namespaces
            for (int i = 0; i < attributeCount; i++)
            {
                String attrPrefix = attributePrefix[ i ];
                if(attrPrefix != null) {
                    String attrUri = getNamespaceURI(attrPrefix);
                    if(attrUri == null) {
                        throw new XMLStreamException(
                            "could not determine namespace bound to attribute prefix "+attrPrefix,
                            getLocation());

                    }
                    attributeUri[ i ] = attrUri;
                } else {
                    attributeUri[ i ] = NO_NAMESPACE;
                }
            }

            //TODO
            //[ WFC: Unique Att Spec ]
            // check namespaced attribute uniqueness contraint!!!

            for (int i = 1; i < attributeCount; i++)
            {
                for (int j = 0; j < i; j++)
                {
                    if( attributeUri[j] == attributeUri[i]
                       && (allStringsInterned && attributeName[j].equals(attributeName[i])
                               || (!allStringsInterned
                                       && attributeNameHash[ j ] == attributeNameHash[ i ]
                                       && attributeName[j].equals(attributeName[i])) )

                      ) {
                        // prepare data for nice error messgae?
                        String attr1 = attributeName[j];
                        if(attributeUri[j] != null) attr1 = attributeUri[j]+":"+attr1;
                        String attr2 = attributeName[i];
                        if(attributeUri[i] != null) attr2 = attributeUri[i]+":"+attr2;
                        throw new XMLStreamException(
                            "duplicated attributes "+attr1+" and "+attr2,
                            getLocation());
                    }
                }
            }


        } else { // ! processNamespaces

            //[ WFC: Unique Att Spec ]
            // check raw attribute uniqueness contraint!!!
            for (int i = 1; i < attributeCount; i++)
            {
                for (int j = 0; j < i; j++)
                {
                    if((allStringsInterned && attributeName[j].equals(attributeName[i])
                            || (!allStringsInterned
                                    && attributeNameHash[ j ] == attributeNameHash[ i ]
                                    && attributeName[j].equals(attributeName[i])) )

                      ) {
                        // prepare data for nice error messgae?
                        String attr1 = attributeName[j];
                        String attr2 = attributeName[i];
                        throw new XMLStreamException(
                            "duplicated attributes "+attr1+" and "+attr2,
                            getLocation());
                    }
                }
            }
        }

        elNamespaceCount[ depth ] = namespaceEnd;
        posEnd = pos;

        if (defaultAttributes != null)
          if (prefix != null)
            addDefaultAttributes(prefix+":"+name);
          else
            addDefaultAttributes(name);
        } catch (EOFException eofe) {
            throw new XMLStreamException(eofe);
        }

        return XMLStreamConstants.START_ELEMENT;
    }

  protected void addDefaultAttributes(String elementName)
    throws XMLStreamException
  {
    if (defaultAttributes == null) return;
    DTDAttlist attList = (DTDAttlist) defaultAttributes.get(elementName);
    if (elementName == null || attList == null) return;
    DTDAttribute[] atts = attList.getAttribute();
    for (int i=0; i < atts.length; i++) {
      DTDAttribute att = atts[i];
      if (att.getDefaultValue() != null) {
        boolean found = false;
        int count = attributeCount;
        for (int j=0; j < count; j++) {
          if (attributeName[j].equals(att.getName())) {
            found = true;
            break;
          }
        }
        if (!found) {
          attributeCount++;
          ensureAttributesCapacity(attributeCount);
          attributePrefix[attributeCount-1] = "";
          attributeUri[attributeCount-1]="";
          attributeName[attributeCount-1] = att.getName();
          attributeValue[attributeCount-1] = att.getDefaultValue();
        }
      }
    }
  }

    protected char parseAttribute() throws XMLStreamException
    {
        try {
        
        // parse attribute
        // [41] Attribute ::= Name Eq AttValue
        // [WFC: No External Entity References]
        // [WFC: No < in Attribute Values]
        int prevPosStart = posStart + bufAbsoluteStart;
        int nameStart = pos - 1 + bufAbsoluteStart;
        int colonPos = -1;
        char ch = buf[ pos - 1 ];
        if(ch == ':' && processNamespaces) throw new XMLStreamException(
                "when namespaces processing enabled colon can not be at attribute name start",
                getLocation());


        boolean startsWithXmlns = processNamespaces && ch == 'x';
        int xmlnsPos = 0;

        ch = more();
        while(isNameChar(ch)) {
            if(processNamespaces) {
                if(startsWithXmlns && xmlnsPos < 5) {
                    ++xmlnsPos;
                    if(xmlnsPos == 1) { if(ch != 'm') startsWithXmlns = false; }
                    else if(xmlnsPos == 2) { if(ch != 'l') startsWithXmlns = false; }
                    else if(xmlnsPos == 3) { if(ch != 'n') startsWithXmlns = false; }
                    else if(xmlnsPos == 4) { if(ch != 's') startsWithXmlns = false; }
                    else if(xmlnsPos == 5) {
                        if(ch != ':') throw new XMLStreamException(
                                "after xmlns in attribute name must be colon"
                                    +"when namespaces are enabled",
                                getLocation());
                        //colonPos = pos - 1 + bufAbsoluteStart;
                    }
                }
                if(ch == ':') {
                    if(colonPos != -1) throw new XMLStreamException(
                            "only one colon is allowed in attribute name"
                                +" when namespaces are enabled",
                            getLocation());
                    colonPos = pos - 1 + bufAbsoluteStart;
                }
            }
            ch = more();
        }

        ensureAttributesCapacity(attributeCount);

        // --- start processing attributes
        String name = null;
        String prefix = null;
        // work on prefixes and namespace URI
        if(processNamespaces) {
            if(xmlnsPos < 4) startsWithXmlns = false;
            if(startsWithXmlns) {
                if(colonPos != -1) {
                    //prefix = attributePrefix[ attributeCount ] = null;
                    name = //attributeName[ attributeCount ] =
                        newString(buf, colonPos - bufAbsoluteStart + 1,
                                  //pos - 1 - (colonPos + 1 - bufAbsoluteStart)
                                  pos - 2 - (colonPos - bufAbsoluteStart)
                                 );
                }
            } else {
                if(colonPos != -1) {
                    prefix = attributePrefix[ attributeCount ] =
                        newString(buf, nameStart - bufAbsoluteStart,
                                  //colonPos - (nameStart - bufAbsoluteStart));
                                  colonPos - nameStart);
                    name = attributeName[ attributeCount ] =
                        newString(buf, colonPos - bufAbsoluteStart + 1,
                                  //pos - 1 - (colonPos + 1 - bufAbsoluteStart));
                                  pos - 2 - (colonPos - bufAbsoluteStart));
                    //name.substring(0, colonPos-nameStart);
                } else {
                    prefix = attributePrefix[ attributeCount ]  = null;
                    name = attributeName[ attributeCount ] =
                        newString(buf, nameStart - bufAbsoluteStart,
                                  pos - 1 - (nameStart - bufAbsoluteStart));
                }
                if(!allStringsInterned) {
                    attributeNameHash[ attributeCount ] = name.hashCode();
                }
            }

        } else {
            // retrieve name
            name = attributeName[ attributeCount ] =
                newString(buf, nameStart - bufAbsoluteStart,
                          pos - 1 - (nameStart - bufAbsoluteStart));
            ////assert name != null;
            if(!allStringsInterned) {
                attributeNameHash[ attributeCount ] = name.hashCode();
            }
        }

        // [25] Eq ::=  S? '=' S?
        while(isS(ch)) { ch = more(); } // skip additional spaces
        if(ch != '=') throw new XMLStreamException(
                "expected = after attribute name",
                getLocation());
        ch = more();
        while(isS(ch)) { ch = more(); } // skip additional spaces

        // [10] AttValue ::=   '"' ([^<&"] | Reference)* '"'
        //                  |  "'" ([^<&'] | Reference)* "'"
        char delimit = ch;
        if(delimit != '"' && delimit != '\'') throw new XMLStreamException(
                "attribute value must start with quotation or apostrophe not "
                    +printable(delimit),
                getLocation());
        // parse until delimit or < and resolve Reference
        //[67] Reference ::= EntityRef | CharRef
        //int valueStart = pos + bufAbsoluteStart;


        boolean normalizedCR = false;
        usePC = false;
        pcStart = pcEnd;
        posStart = pos;

        while(true) {
            ch = more();
            if(ch == delimit) {
                break;
            } if(ch == '<') {
                throw new XMLStreamException(
                    "markup not allowed inside attribute value - illegal < ", getLocation());
            } if(ch == '&') {
                // extractEntityRef
                posEnd = pos - 1;
                if(!usePC) {
                    boolean hadCharData = posEnd > posStart;
                    if(hadCharData) {
                        // posEnd is already set correctly!!!
                        joinPC();
                    } else {
                        usePC = true;
                        pcStart = pcEnd = 0;
                    }
                }
                //assert usePC == true;

                char[] resolvedEntity = parseEntityRef();
                // check if replacement text can be resolved !!!
                if(resolvedEntity == null) {
                    if(entityRefName == null) {
                        entityRefName = newString(buf, posStart, posEnd - posStart);
                    }
                    throw new XMLStreamException(
                        "could not resolve entity named '"+printable(entityRefName)+"'",
                        getLocation());
                }
                // write into PC replacement text - do merge for replacement text!!!!
                for (int i = 0; i < resolvedEntity.length; i++)
                {
                    if(pcEnd >= pc.length) ensurePC(pcEnd);
                    pc[pcEnd++] = resolvedEntity[ i ];

                }
            } else if(ch == '\t' || ch == '\n' || ch == '\r') {
                // do attribute value normalization
                // as described in http://www.w3.org/TR/REC-xml#AVNormalize
                // TODO add test for it form spec ...
                // handle EOL normalization ...
                if(!usePC) {
                    posEnd = pos - 1;
                    if(posEnd > posStart) {
                        joinPC();
                    } else {
                        usePC = true;
                        pcEnd = pcStart = 0;
                    }
                }
                //assert usePC == true;
                if(pcEnd >= pc.length) ensurePC(pcEnd);
                if(ch != '\n' || !normalizedCR) {
                    pc[pcEnd++] = ' '; //'\n';
                }

            } else {
                if(usePC) {
                    if(pcEnd >= pc.length) ensurePC(pcEnd);
                    pc[pcEnd++] = ch;
                }
            }
            normalizedCR = ch == '\r';
        }


        if(processNamespaces && startsWithXmlns) {

            String ns = null;
            if(!usePC) {
                ns = newStringIntern(buf, posStart, pos - 1 - posStart);
            } else {
                ns = newStringIntern(pc, pcStart, pcEnd - pcStart);
            }
            ensureNamespacesCapacity(namespaceEnd);
            int prefixHash = -1;
            if(colonPos != -1) {
                if(ns.length() == 0) {
                    throw new XMLStreamException(
                        "non-default namespace can not be declared to be empty string",
                        getLocation());
                }
                // declare new namespace
                namespacePrefix[ namespaceEnd ] = name;
                if(!allStringsInterned) {
                    prefixHash = namespacePrefixHash[ namespaceEnd ] = name.hashCode();
                }
            } else {
                // declare  new default namespace ...
                namespacePrefix[ namespaceEnd ] = null;
                 if(!allStringsInterned) {
                    prefixHash = namespacePrefixHash[ namespaceEnd ] = -1;
                }
            }
            namespaceUri[ namespaceEnd ] = ns;

            // detect duplicate namespace declarations!!!
            int startNs = elNamespaceCount[ depth - 1 ];
            for (int i = namespaceEnd - 1; i >= startNs; --i)
            {
                if(((allStringsInterned || name == null) && namespacePrefix[ i ] == name)
                   || (!allStringsInterned && name != null &&
                           namespacePrefixHash[ i ] == prefixHash
                           && name.equals(namespacePrefix[ i ])
                      ))
                {
                    String s = name == null ? "default" : "'"+name+"'";
                    throw new XMLStreamException(
                        "duplicated namespace declaration for "+s+" prefix", getLocation());

                }
            }
            //            ++localNamespaceEnd;
            ++namespaceEnd;

        } else {
            if(!usePC) {
                attributeValue[ attributeCount ] =
                    new String(buf, posStart, pos - 1 - posStart);
            } else {
                attributeValue[ attributeCount ] =
                    new String(pc, pcStart, pcEnd - pcStart);
            }
            ++attributeCount;
        }
        posStart = prevPosStart - bufAbsoluteStart;
        return ch;

        } catch (EOFException eofe) {
            throw new XMLStreamException(eofe);
        }
    }

    protected char[] charRefOneCharBuf = new char[1];

    protected char[] parseEntityRef()
        throws XMLStreamException
    {
       boolean replace = getConfigurationContext().isReplacingEntities();
        try {
        
        // ASSUMPTION just after &
        entityRefName = null;
        posStart = pos;
        char ch = more();
        if(ch == '#') {
            // parse character reference
            char charRef = 0;
            ch = more();
            if(ch == 'x') {
                //encoded in hex
                while(true) {
                    ch = more();
                    if(ch >= '0' && ch <= '9') {
                        charRef = (char)(charRef * 16 + (ch - '0'));
                    } else if(ch >= 'a' && ch <= 'f') {
                        charRef = (char)(charRef * 16 + (ch - ('a' - 10)));
                    } else if(ch >= 'A' && ch <= 'F') {
                        charRef = (char)(charRef * 16 + (ch - ('A' - 10)));
                    } else if(ch == ';') {
                        break;
                    } else {
                        throw new XMLStreamException(
                            "character reference (with hex value) may not contain "
                                +printable(ch), getLocation());
                    }
                }
            } else {
                // encoded in decimal
                while(true) {
                    if(ch >= '0' && ch <= '9') {
                        charRef = (char)(charRef * 10 + (ch - '0'));
                    } else if(ch == ';') {
                        break;
                    } else {
                        throw new XMLStreamException(
                            "character reference (with decimal value) may not contain "
                                +printable(ch), getLocation());
                    }
                    ch = more();
                }
            }
            posEnd = pos - 1;
            charRefOneCharBuf[0] = charRef;
            if(!replace) {
                text = newString(charRefOneCharBuf, 0, 1);
            }
            return charRefOneCharBuf;
        } else {
            // name reference

            // scan until ;
            do{ ch = more(); } while(ch != ';');
            posEnd = pos - 1;
            // determine what name maps to
            int len = posEnd - posStart;
            if(len == 2 && buf[posStart] == 'l' && buf[posStart+1] == 't') {
                if(!replace)
                    text = "<";
                charRefOneCharBuf[0] = '<';
                
                return charRefOneCharBuf;
                //if(paramPC || isParserTokenizing) {
                //    if(pcEnd >= pc.length) ensurePC();
                //   pc[pcEnd++] = '<';
                //}
            } else if(len == 3 && buf[posStart] == 'a'
                      && buf[posStart+1] == 'm' && buf[posStart+2] == 'p') {
                if(!replace)
                    text = "&";
                charRefOneCharBuf[0] = '&';
                
                return charRefOneCharBuf;
            } else if(len == 2 && buf[posStart] == 'g' && buf[posStart+1] == 't') {
                if(!replace)
                    text = ">";
                charRefOneCharBuf[0] = '>';
                
                return charRefOneCharBuf;
            } else if(len == 4 && buf[posStart] == 'a' && buf[posStart+1] == 'p'
                      && buf[posStart+2] == 'o' && buf[posStart+3] == 's')
            {
                if(!replace)
                    text = "'";
                charRefOneCharBuf[0] = '\'';
                
                return charRefOneCharBuf;
            } else if(len == 4 && buf[posStart] == 'q' && buf[posStart+1] == 'u'
                      && buf[posStart+2] == 'o' && buf[posStart+3] == 't')
            {
                if(!replace)
                    text = "\"";
                charRefOneCharBuf[0] = '"';

                return charRefOneCharBuf;
            } else {
                char[] result = lookupEntityReplacement(len);
                if(result != null) {
                    return result;
                }
            }
            if(!replace) text = null;
            return null;
        }

        } catch (EOFException eofe) {
            throw new XMLStreamException(eofe);
        }
    }

  protected char[] lookupEntityReplacement(int entitNameLen)
    throws XMLStreamException
  {

      if(!allStringsInterned) {
        int hash = fastHash(buf, posStart, posEnd - posStart);
      LOOP:
        for (int i = entityEnd - 1; i >= 0; --i)
          {
            if(hash == entityNameHash[ i ] && entitNameLen == entityNameBuf[ i ].length) {
              char[] entityBuf = entityNameBuf[ i ];
              for (int j = 0; j < entitNameLen; j++)
                {
                  if(buf[posStart + j] != entityBuf[j]) continue LOOP;
                }
              if(tokenize) text = entityReplacement[ i ];
              return entityReplacementBuf[ i ];
            }
          }
      } else {
        entityRefName = newString(buf, posStart, posEnd - posStart);
        for (int i = entityEnd - 1; i >= 0; --i)
          {
            // take advantage that interning for newStirng is enforced
            if(entityRefName == entityName[ i ]) {
              if(tokenize) text = entityReplacement[ i ];
              return entityReplacementBuf[ i ];
            }
          }
      }
      return null;
    }


    protected void parseComment()
        throws XMLStreamException
    {
        // implements XML 1.0 Section 2.5 XMLStreamConstants.COMMENTs
        //ASSUMPTION: seen <!-
        
        try {
            
        char ch = more();
        if(ch != '-') throw new XMLStreamException(
                "expected <!-- for COMMENT start", getLocation());
        if(tokenize) posStart = pos;

        int curLine = lineNumber;
        int curColumn = columnNumber;
        try {
            boolean seenDash = false;
            boolean seenDashDash = false;
            while(true) {
                // scan until it hits -->
                ch = more();
                if(seenDashDash && ch != '>') {
                    throw new XMLStreamException(
                        "in COMMENT after two dashes (--) next character must be >"
                            +" not "+printable(ch), getLocation());
                }
                if(ch == '-') {
                    if(!seenDash) {
                        seenDash = true;
                    } else {
                        seenDashDash = true;
                        seenDash = false;
                    }
                } else if(ch == '>') {
                    if(seenDashDash) {
                        break;  // found end sequence!!!!
                    } else {
                        seenDashDash = false;
                    }
                    seenDash = false;
                } else {
                    seenDash = false;
                }
            }
        } catch(EOFException ex) {
            // detect EOF and create meaningful error ...
            throw new XMLStreamException(
                "COMMENT started on line "+curLine+" and column "+curColumn+" was not closed",
                getLocation(), ex);
        }
        if(tokenize) posEnd = pos - 3;

        } catch (EOFException eofe) {
            throw new XMLStreamException(eofe);
        }
    }

  public String getPITarget() {
    if (eventType != XMLStreamConstants.PROCESSING_INSTRUCTION) return null;
    return piTarget;
  }
  public String getPIData() {
    if (eventType != XMLStreamConstants.PROCESSING_INSTRUCTION) return null;
    return piData;
  }
  public NamespaceContext getNamespaceContext() {
    return new ReadOnlyNamespaceContextBase(namespacePrefix,
                                            namespaceUri,
                                            namespaceEnd);
  }
                                           
  protected boolean parsePI()
    throws XMLStreamException
  {
    // implements XML 1.0 Section 2.6 Processing Instructions
    
    // [16] PI ::= '<?' PITarget (S (Char* - (Char* '?>' Char*)))? '?>'

    //ASSUMPTION: seen <?
    boolean isXMLDecl=false;
    piTarget = null;
    piData = null;
    if(tokenize) posStart = pos;
    int curLine = lineNumber;
    int curColumn = columnNumber;
    int piTargetStart = pos;
    int piTargetBegin = pos;
    int piTargetEnd = -1;

    try {
      // Let's first scan for the PI target:
      char ch;

      while (true) {
          ch = more();
          if (ch == '?') {
              // Let's assume it'll be followed by a '>'...
              break;
          } else if (isNameChar(ch)) {
              ; // good
          } else if (isS(ch)) {
              break;
          } else {
              throw new XMLStreamException("unexpected character "+printable(ch)+" after processing instruction name; expected a white space or '?>'",
                                           getLocation());
          }
      }
      
      piTargetEnd = pos - 1;
      piTarget=new String(buf,piTargetStart,piTargetEnd-piTargetStart);
      // [17] PITarget ::= Name - (('X' | 'x') ('M' | 'm') ('L' | 'l'))

      // Let's first verify there was a target:
      int targetLen = piTargetEnd - piTargetStart;
      if(targetLen == 0) { // missing target
          throw new XMLStreamException("processing instruction must have PITarget name", getLocation());
      }

      /* And then let's skip (unnecessary) white space, if we hit white
       * space.
       */
      if (ch != '?') {
          do {
              ch = more();
          } while (isS(ch));
      }

      // Ok, got the target name; need to check it then:
      piTargetBegin = pos-1;
      
      // Do we now have the xml declaration?
      if(targetLen == 3
         && ((buf[piTargetStart] == 'x' || buf[piTargetStart] == 'X')
             && (buf[piTargetStart+1] == 'm' || buf[piTargetStart+1] == 'M')
             && (buf[piTargetStart+2] == 'l' || buf[piTargetStart+2] == 'L')
             )) {
          if(piTargetStart != 2) {  //<?xml is allowed as first characters in input ...
              throw new XMLStreamException("processing instruction can not have PITarget with reserved xml name",
                                           getLocation());
          } else {
              if(buf[piTargetStart] != 'x'
                 && buf[piTargetStart+1] != 'm'
                 && buf[piTargetStart+2] != 'l') {
                  throw new XMLStreamException("XMLDecl must have xml name in lowercase",
                                               getLocation());
              }
              parseXmlDecl(ch);
              isXMLDecl = true;
          }
      } else { // nope, just a regular PI:
          data_loop:
          while(true) {
              while (ch != '?') {
                  ch = more();
              }
              do {
                  ch = more();
                  if (ch == '>') {
                      break data_loop;
                  }
              } while (ch == '?');
          }
      }

    } catch(EOFException ex) {
      // detect EOF and create meaningful error ...
      throw new XMLStreamException("processing instruction started on line "+curLine+" and column "+curColumn
                                   +" was not closed",
                                   getLocation(), ex);
    }
    if(tokenize) posEnd = pos - 2;
    piData = new String(buf,piTargetBegin,posEnd - piTargetBegin);
    return isXMLDecl;
  }

    protected final static char[] VERSION = {'v','e','r','s','i','o','n'};
    protected final static char[] ENCODING = {'e','n','c','o','d','i','n','g'};
    protected final static char[] STANDALONE = {'s','t','a','n','d','a','l','o','n','e'};
    protected final static char[] YES = {'y','e','s'};
    protected final static char[] NO = {'n','o'};

    protected char requireInput(char ch, char[] input)
        throws XMLStreamException
    {
        for (int i = 0; i < input.length; i++)
        {
            if(ch != input[i]) {
                throw new XMLStreamException(
                    "expected "+printable(input[i])+" in "+new String(input)
                        +" and not "+printable(ch), getLocation());
            }
            try {
                ch = more();
            } catch (EOFException eofe) {
                throw new XMLStreamException(eofe);
            }
        }
        return ch;
    }


    protected char requireNextS()
        throws XMLStreamException
    {
        char ch;
        try {
            ch = more();
        } catch (EOFException eofe) {
            throw new XMLStreamException(eofe);
        }
        if(!isS(ch)) {
            throw new XMLStreamException(
                "white space is required and not "+printable(ch), getLocation());
        }
        return skipS(ch);
    }

    protected char skipS(char ch)
        throws XMLStreamException
    {
        try {
        while(isS(ch)) { ch = more(); } // skip additional spaces
        return ch;
        } catch (EOFException eofe) {
            throw new XMLStreamException(eofe);
        }
    }

    protected void parseXmlDecl(char ch)
        throws XMLStreamException
    {
        // [23] XMLDecl ::= '<?xml' VersionInfo EncodingDecl? SDDecl? S? '?>'

        // --- parse VersionInfo

        // [24] VersionInfo ::= S 'version' Eq ("'" VersionNum "'" | '"' VersionNum '"')
        // parse is positioned just on first S past <?xml
        
        try {

        ch = skipS(ch);
        ch = requireInput(ch, VERSION);
        // [25] Eq ::= S? '=' S?
        ch = skipS(ch);
        if(ch != '=') {
            throw new XMLStreamException(
                "expected equals sign (=) after version and not "+printable(ch), getLocation());
        }
        ch = more();
        ch = skipS(ch);
        if(ch != '\'' && ch != '"') {
            throw new XMLStreamException(
                "expected apostrophe (') or quotation mark (\") after version and not "
                    +printable(ch), getLocation());
        }
        char quotChar = ch;
        int versionStart = pos;
        ch = more();
        // [26] VersionNum ::= ([a-zA-Z0-9_.:] | '-')+
        while(ch != quotChar) {
            if((ch  < 'a' || ch > 'z') && (ch  < 'A' || ch > 'Z') && (ch  < '0' || ch > '9')
               && ch != '_' && ch != '.' && ch != ':' && ch != '-')
            {
                throw new XMLStreamException(
                    "<?xml version value expected to be in ([a-zA-Z0-9_.:] | '-')"
                        +" not "+printable(ch), getLocation());
            }
            ch = more();
        }
        int versionEnd = pos - 1;
        parseXmlDeclWithVersion(versionStart, versionEnd);

        } catch (EOFException eofe) {
            throw new XMLStreamException(eofe);
        }
    }
    //protected String xmlDeclVersion;

    protected void parseXmlDeclWithVersion(int versionStart, int versionEnd)
        throws XMLStreamException
    {

        try {

        // check version is "1.0"
        if((versionEnd - versionStart != 3)
           || buf[versionStart] != '1'
           || buf[versionStart+1] != '.'
           || buf[versionStart+2] != '0')
        {
            throw new XMLStreamException(
                "only 1.0 is supported as <?xml version not '"
                    +printable(new String(buf, versionStart, versionEnd))+"'", getLocation());
        }
        xmlVersion = new String(buf, versionStart, versionEnd-versionStart);

        // [80] EncodingDecl ::= S 'encoding' Eq ('"' EncName '"' | "'" EncName "'" )
        char ch = more();
        ch = skipS(ch);
        if(ch != '?') {
            ch = skipS(ch);
            /* 07-Nov-2004, TSa: This is wrong (see bug #187); encoding
             *    declaration is optional: may as well get stand-alone
             *    declaration at this point.
             */
            ch = requireInput(ch, ENCODING);
            ch = skipS(ch);
            if(ch != '=') {
                throw new XMLStreamException(
                    "expected equals sign (=) after encoding and not "+printable(ch), getLocation());
            }
            ch = more();
            ch = skipS(ch);
            if(ch != '\'' && ch != '"') {
                throw new XMLStreamException(
                    "expected apostrophe (') or quotation mark (\") after encoding and not "
                        +printable(ch), getLocation());
            }
            char quotChar = ch;
            int encodingStart = pos;
            ch = more();
            // [81] EncName ::= [A-Za-z] ([A-Za-z0-9._] | '-')*
            if((ch  < 'a' || ch > 'z') && (ch  < 'A' || ch > 'Z'))
            {
                throw new XMLStreamException(
                    "<?xml encoding name expected to start with [A-Za-z]"
                        +" not "+printable(ch), getLocation());
            }
            ch = more();
            while(ch != quotChar) {
                if((ch  < 'a' || ch > 'z') && (ch  < 'A' || ch > 'Z') && (ch  < '0' || ch > '9')
                   && ch != '.' && ch != '_' && ch != '-')
                {
                    throw new XMLStreamException(
                        "<?xml encoding value expected to be in ([A-Za-z0-9._] | '-')"
                            +" not "+printable(ch), getLocation());
                }
                ch = more();
            }
            int encodingEnd = pos - 1;
            //String encodingName = newStringIntern(buf, encodingStart, encodingEnd);
            // TODO reconcile with setInput encodingName
            charEncodingScheme = newString(buf, encodingStart, encodingEnd-encodingStart);
            ch = more();
            ch = skipS(ch);

            // [32] SDDecl ::= S 'standalone' Eq (("'" ('yes' | 'no') "'") | ('"' ('yes' | 'no') '"'))
            if(ch != '?') {
                ch = skipS(ch);
                ch = requireInput(ch, STANDALONE);
                ch = skipS(ch);
                if(ch != '=') {
                    throw new XMLStreamException(
                        "expected equals sign (=) after standalone and not "+printable(ch),
                        getLocation());
                }
                ch = more();
                ch = skipS(ch);
                if(ch != '\'' && ch != '"') {
                    throw new XMLStreamException(
                        "expected apostrophe (') or quotation mark (\") after encoding and not "
                            +printable(ch), getLocation());
                }
                quotChar = ch;
                int standaloneStart = pos;
                ch = more();
                if(ch == 'y') {
                    ch = requireInput(ch, YES);
                    standalone = true;
                } else if(ch == 'n') {
                    ch = requireInput(ch, NO);
                    standalone = false;
                } else {
                    throw new XMLStreamException(
                        "expected 'yes' or 'no' after standalone and not "
                            +printable(ch), getLocation());
                }
                standaloneSet = true;
                if(ch != quotChar) {
                    throw new XMLStreamException(
                        "expected "+quotChar+" after standalone value not "
                            +printable(ch), getLocation());
                }
                ch = more();
            }
        }
        ch = skipS(ch);
        if(ch != '?') {
            throw new XMLStreamException(
                "expected ?> as last part of <?xml not "
                    +printable(ch), getLocation());
        }
        ch = more();
        if(ch != '>') {
            throw new XMLStreamException(
                "expected ?> as last part of <?xml not "
                    +printable(ch), getLocation());
        }

        } catch (EOFException eofe) {
            throw new XMLStreamException(eofe);
        }
    }

    protected void parseDocdecl()
        throws XMLStreamException
    {
        //ASSUMPTION: seen <!D
        posStart = pos-3;
        try {
          if(more() != 'O'
             || more() != 'C'
             || more() != 'T'
             || more() != 'Y'
             || more() != 'P'
             || more() != 'E') {
              throw new XMLStreamException("expected <!DOCTYPE", getLocation());
          }
          
          // do simple and crude scanning for end of doctype
          
          // [28]  doctypedecl ::= '<!DOCTYPE' S Name (S ExternalID)? S? ('['
          //                      (markupdecl | DeclSep)* ']' S?)? '>'

          /* 07-Nov-2004, TSa: Should be fairly easy to verify (obligatory)
           *   root element, and optional public/system ids too.
           */

          int bracketLevel = 0;
          while(true) {
              char ch = more();
              if(ch == '[') ++bracketLevel;
              if(ch == ']') --bracketLevel;
              if(ch == '>' && bracketLevel == 0) break;
          }
          posEnd = pos ;
          processDTD();
        } catch (EOFException eofe) {
            throw new XMLStreamException(eofe);
        }
    }

  protected void processDTD()
    throws XMLStreamException
  {
    try {
        String internalDTD = new String(buf, posStart, posEnd - posStart);
        int start = internalDTD.indexOf('[');
        int end = internalDTD.lastIndexOf(']');
        if (start == -1 || end == -1 || end <= start) return;
        String expectedDTD = internalDTD.substring(start+1,end);
        DTDParser dtdParser = new DTDParser(
                              new java.io.StringReader(expectedDTD));
        DTD dtd = dtdParser.parse();
        java.io.StringWriter out = new java.io.StringWriter();
        // Get general entities
        Vector v = dtd.getItemsByType(
            (new com.wutka.dtd.DTDEntity()).getClass());
        Enumeration e = v.elements();
        while(e.hasMoreElements()) {
          DTDEntity entity = (DTDEntity) e.nextElement();
          if (!entity.isParsed())
            defineEntityReplacementText(entity.getName(),
                                        entity.getValue());

        }

        // Get default attributes
        v = dtd.getItemsByType(
          (new com.wutka.dtd.DTDAttlist()).getClass());
        e = v.elements();
        while(e.hasMoreElements()) {
          DTDAttlist list = (DTDAttlist) e.nextElement();
          DTDAttribute[] atts = list.getAttribute();
          for (int i=0; i < atts.length; i++) {

            DTDAttribute att = atts[i];
            if (att.getDefaultValue() != null) {
              if (defaultAttributes == null)
                defaultAttributes = new HashMap();
              defaultAttributes.put(list.getName(),
                                    list);
            }
          }
        }
    } catch (IOException ioe) {
      //System.out.println(ioe);
      ioe.printStackTrace();
      throw new XMLStreamException(ioe);
    }
  }
    protected void parseCDATA()
        throws XMLStreamException
    {
        // implements XML 1.0 Section 2.7 CDATA Sections

        // [18] XMLStreamConstants.CDATA ::= CDStart CData CDEnd
        // [19] CDStart ::=  '<![CDATA['
        // [20] CData ::= (Char* - (Char* ']]>' Char*))
        // [21] CDEnd ::= ']]>'

        //ASSUMPTION: seen <![

      try {

        if(more() != 'C'
           || more() != 'D'
           || more() != 'A'
           || more() != 'T'
           || more() != 'A'
           || more() != '['
           ) {
            throw new XMLStreamException("expected <[CDATA[ for CDATA start", getLocation());
        }
      } catch (EOFException eofe) {
        throw new XMLStreamException(eofe);
      }

      char ch;

        //if(tokenize) {
        posStart = pos;
        int curLine = lineNumber;
        int curColumn = columnNumber;
        try {
            boolean seenBracket = false;
            boolean seenBracketBracket = false;
            while(true) {
                // scan until it hits -->
                ch = more();
                if(ch == ']') {
                    if(!seenBracket) {
                        seenBracket = true;
                    } else {
                        seenBracketBracket = true;
                        seenBracket = false;
                    }
                } else if(ch == '>') {
                    if(seenBracketBracket) {
                        break;  // found end sequence!!!!
                    } else {
                        seenBracketBracket = false;
                    }
                    seenBracketBracket = false;
                } else {
                    seenBracketBracket = false;
                }
            }
        } catch(EOFException ex) {
            // detect EOF and create meaningful error ...
            throw new XMLStreamException(
                "CDATA section on line "+curLine+" and column "+curColumn+" was not closed",
                getLocation(), ex);
        }
        //if(tokenize)
        posEnd = pos - 3;
    }

    protected void fillBuf() throws XMLStreamException, EOFException {

        if(reader == null) throw new XMLStreamException(
                "reader must be set before parsing is started");

        // see if we are in comapction area
        if(bufEnd > bufSoftLimit) {

            // expand buffer it makes sense!!!!
            boolean compact = bufStart > bufSoftLimit;
            boolean expand = false;
            if(!compact) {
                //freeSpace
                if(bufStart < buf.length / 2) {
                    // less then half buffer available forcompactin --> expand instead!!!
                    expand = true;
                } else {
                    // at least half of buffer cna reclaimed --> worthwhile effort!!!
                    compact = true;
                }
            }

            // if buffer almost full then compact it
            if(compact) {
                //TODO: look on trashing
                // //assert bufStart > 0
                System.arraycopy(buf, bufStart, buf, 0, bufEnd - bufStart);
                if(TRACE_SIZING) System.out.println("fillBuf() compacting "+bufStart);

            } else if(expand) {
                int newSize = 2 * buf.length;
                char newBuf[] = new char[ newSize ];
                if(TRACE_SIZING) System.out.println("fillBuf() "+buf.length+" => "+newSize);
                System.arraycopy(buf, bufStart, newBuf, 0, bufEnd - bufStart);
                buf = newBuf;
                if(bufLoadFactor > 0) {
                    bufSoftLimit = ( bufLoadFactor * buf.length ) /100;
                }

            } else {
                throw new XMLStreamException("internal error in fillBuffer()");
            }
            bufEnd -= bufStart;
            pos -= bufStart;
            posStart -= bufStart;
            posEnd -= bufStart;
            bufAbsoluteStart += bufStart;
            bufStart = 0;
        }
        // at least one charcter must be read or error
        int len = buf.length - bufEnd > READ_CHUNK_SIZE ? READ_CHUNK_SIZE : buf.length - bufEnd;
        int ret;
        try {
            ret = reader.read(buf, bufEnd, len);
        } catch (IOException ioe) {
            throw new XMLStreamException(ioe);
        }
        if(ret > 0) {
            bufEnd += ret;
            return;
        }
        if(ret == -1) {
            throw new EOFException("no more data available");
        } else {
            throw new XMLStreamException("error reading input, returned "+ret);
        }

    }

    protected char more() throws XMLStreamException, EOFException {
        if(pos >= bufEnd) fillBuf();
        char ch = buf[pos++];
        //line/columnNumber
        if(ch == '\n') { ++lineNumber; columnNumber = 1; }
        else { ++columnNumber; }
        return ch;
    }

    //protected char printable(char ch) { return ch; }
    protected String printable(char ch) {
        if(ch == '\n') {
            return "\\n";
        } else if(ch == '\r') {
            return "\\r";
        } else if(ch == '\t') {
            return "\\t";
        } else if(ch == '\'') {
            return "\\'";
        } if(ch > 127 || ch < 32) {
            return "\\u"+Integer.toHexString((int)ch);
        }
        return ""+ch;
    }

    protected String printable(String s) {
        if(s == null) return null;
        StringBuffer buf = new StringBuffer();
        for(int i = 0; i < s.length(); ++i) {
            buf.append(printable(s.charAt(i)));
        }
        s = buf.toString();
        return s;
    }

    protected void ensurePC(int end) {
        //assert end >= pc.length;
        int newSize = end > READ_CHUNK_SIZE ? 2 * end : 2 * READ_CHUNK_SIZE;
        char[] newPC = new char[ newSize ];
        if(TRACE_SIZING) System.out.println("ensurePC() "+pc.length+" ==> "+newSize+" end="+end);
        System.arraycopy(pc, 0, newPC, 0, pcEnd);
        pc = newPC;
        //assert end < pc.length;
    }

    protected void joinPC() {
        //assert usePC == false;
        //assert posEnd > posStart;
        int len = posEnd - posStart;
        int newEnd = pcEnd + len + 1;
        if(newEnd >= pc.length) ensurePC(newEnd); // add 1 for extra space for one char
        //assert newEnd < pc.length;
        System.arraycopy(buf, posStart, pc, pcEnd, len);
        pcEnd += len;
        usePC = true;

    }
  public Location getLocation() {
    return this;
  }
  public String getPublicId() {
    return null;
  }
  public String getSystemId() {
    return null;
  }
  private ConfigurationContextBase configurationContext;
  public void setConfigurationContext(ConfigurationContextBase c) {
    configurationContext = c;
  }
  public ConfigurationContextBase getConfigurationContext() {
    return configurationContext;
  }
  public  Object getProperty(String name) {
    return configurationContext.getProperty(name);
  }
}


/*
 * Indiana University Extreme! Lab Software License, Version 1.1.1
 *
 *
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the Indiana
 *        University Extreme! Lab (http://www.extreme.indiana.edu/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Indiana University" and "Indiana University
 *    Extreme! Lab" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact http://www.extreme.indiana.edu/.
 *
 * 5. Products derived from this software may not use "Indiana
 *    University" name nor may "Indiana University" appear in their name,
 *    without prior written permission of the Indiana University.
 *
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */


