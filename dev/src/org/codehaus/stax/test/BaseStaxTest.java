package org.codehaus.stax.test;

import java.io.*;
import java.util.HashMap;

import junit.framework.TestCase;

import javax.xml.stream.*;

/**
 * Base class for all StaxTest unit test classes. Contains shared
 * functionality for many common set up tasks, as well as for
 * outputting diagnostics.
 *
 * @author Tatu Saloranta
 */
public class BaseStaxTest
    extends TestCase
    implements XMLStreamConstants
{
    final static HashMap mTokenTypes = new HashMap();
    static {
        mTokenTypes.put(new Integer(START_ELEMENT), "START_ELEMENT");
        mTokenTypes.put(new Integer(END_ELEMENT), "END_ELEMENT");
        mTokenTypes.put(new Integer(START_DOCUMENT), "START_DOCUMENT");
        mTokenTypes.put(new Integer(END_DOCUMENT), "END_DOCUMENT");
        mTokenTypes.put(new Integer(CHARACTERS), "CHARACTERS");
        mTokenTypes.put(new Integer(CDATA), "CDATA");
        mTokenTypes.put(new Integer(COMMENT), "COMMENT");
        mTokenTypes.put(new Integer(PROCESSING_INSTRUCTION), "PROCESSING_INSTRUCTION");
        mTokenTypes.put(new Integer(DTD), "DTD");
        mTokenTypes.put(new Integer(SPACE), "SPACE");
        mTokenTypes.put(new Integer(ENTITY_REFERENCE), "ENTITY_REFERENCE");
    }

    /*
    ///////////////////////////////////////////////////
    // Consts for expected values
    ///////////////////////////////////////////////////
     */

    /**
     * Expected return value for streamReader.getNamespaceURI() in
     * non-namespace-aware mode
     */
    protected final String DEFAULT_URI_NON_NS = null;

    protected final String DEFAULT_URI_NS = "";

    /*
    ///////////////////////////////////////////////////
    // Other consts
    ///////////////////////////////////////////////////
     */

    /*
    ///////////////////////////////////////////////////
    // Cached instances
    ///////////////////////////////////////////////////
     */

    XMLInputFactory mInputFactory;

    protected BaseStaxTest(String name) {
        super(name);
    }

    /*
    //////////////////////////////////////////////////
    // Factory methods
    //////////////////////////////////////////////////
     */
    
    protected XMLInputFactory getInputFactory()
    {
        if (mInputFactory == null) {
            mInputFactory = getNewInputFactory();
        }
        return mInputFactory;
    }

    protected static XMLInputFactory getNewInputFactory()
    {
        return XMLInputFactory.newInstance();
    }

    protected static XMLStreamReader constructStreamReader(XMLInputFactory f, String content)
        throws XMLStreamException
    {
        return f.createXMLStreamReader(new StringReader(content));
    }

    protected static XMLStreamReader constructStreamReader(XMLInputFactory f, byte[] b)
        throws XMLStreamException
    {
        return f.createXMLStreamReader(new ByteArrayInputStream(b));
    }

    protected static XMLStreamReader constructStreamReaderForFile(XMLInputFactory f, String filename)
        throws IOException, XMLStreamException
    {
        File inf = new File(filename);
        XMLStreamReader sr = f.createXMLStreamReader(inf.toURL().toString(),
                                                     new FileReader(inf));
        assertEquals(START_DOCUMENT, sr.getEventType());
        return sr;
    }

    /*
    //////////////////////////////////////////////////
    // Configuring input factory
    //////////////////////////////////////////////////
     */

    protected static boolean isCoalescing(XMLInputFactory f)
        throws XMLStreamException
    {
        return ((Boolean) f.getProperty(XMLInputFactory.IS_COALESCING)).booleanValue();
    }

    protected static void setCoalescing(XMLInputFactory f, boolean state)
        throws XMLStreamException
    {
        f.setProperty(XMLInputFactory.IS_COALESCING, Boolean.valueOf(state));
        // Let's just double-check it...
        assertEquals(state, isCoalescing(f));
    }

    protected static boolean isValidating(XMLInputFactory f)
        throws XMLStreamException
    {
        return ((Boolean) f.getProperty(XMLInputFactory.IS_VALIDATING)).booleanValue();
    }

    protected static void setValidating(XMLInputFactory f, boolean state)
        throws XMLStreamException
    {
        try {
            f.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.valueOf(state));
        } catch (IllegalArgumentException iae) {
            fail("Could not set DTD validating mode to "+state+": "+iae);
            //throw new XMLStreamException(iae.getMessage(), iae);
        }
        assertEquals(state, isValidating(f));
    }

    protected static boolean isNamespaceAware(XMLInputFactory f)
        throws XMLStreamException
    {
        return ((Boolean) f.getProperty(XMLInputFactory.IS_NAMESPACE_AWARE)).booleanValue();
    }

    /**
     * @return True if setting succeeded, and property supposedly was
     *   succesfully set to the value specified; false if there was a problem.
     */
    protected static boolean setNamespaceAware(XMLInputFactory f, boolean state)
        throws XMLStreamException
    {
        try {
            f.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.valueOf(state));
            assertEquals(state, isNamespaceAware(f));
            return true;
        } catch (Exception e) {
            /* Let's assume, then, that the property (or specific value for it)
             * is NOT supported...
             */
            return false;
        }
    }

    protected static void setReplaceEntities(XMLInputFactory f, boolean state)
        throws XMLStreamException
    {
        Boolean b = Boolean.valueOf(state);
        f.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, b);
        assertEquals(b, f.getProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES));
    }

    protected static void setSupportDTD(XMLInputFactory f, boolean state)
        throws XMLStreamException
    {
        Boolean b = Boolean.valueOf(state);
        f.setProperty(XMLInputFactory.SUPPORT_DTD, b);
        assertEquals(b, f.getProperty(XMLInputFactory.SUPPORT_DTD));
    }

    /*
    //////////////////////////////////////////////////
    // Derived assert/fail methods
    //////////////////////////////////////////////////
     */

    protected static void assertTokenType(int expType, int actType)
    {
        if (expType != actType) {
            String expStr = (String) mTokenTypes.get(new Integer(expType));
            String actStr = (String) mTokenTypes.get(new Integer(actType));

            if (expStr == null) {
                expStr = ""+expType;
            }
            if (actStr == null) {
                actStr = ""+actType;
            }
            fail("Expected token "+expStr+"; got "+actStr+".");
        }
    }

    protected static void failStrings(String msg, String exp, String act)
    {
        // !!! TODO: Indicate position where Strings differ
        fail(msg+": expected "+quotedPrintable(exp)+", got "
             +quotedPrintable(act));
    }

    /*
    //////////////////////////////////////////////////
    // Debug/output helpers
    //////////////////////////////////////////////////
     */

    protected static String tokenTypeDesc(int tt)
    {
        String desc = (String) mTokenTypes.get(new Integer(tt));
        return (desc == null) ? ("["+tt+"]") : desc;
    }

    protected static String printable(char ch)
    {
        if (ch == '\n') {
            return "\\n";
        }
        if (ch == '\r') {
            return "\\r";
        }
        if (ch == '\t') {
            return "\\t";
        }
        if (ch == ' ') {
            return "_";
        }
        if (ch > 127 || ch < 32) {
            StringBuffer sb = new StringBuffer(6);
            sb.append("\\u");
            String hex = Integer.toHexString((int)ch);
            for (int i = 0, len = 4 - hex.length(); i < len; i++) {
                sb.append('0');
            }
            sb.append(hex);
            return sb.toString();
        }
        return null;
    }

    protected static String printable(String str)
    {
        if (str == null || str.length() == 0) {
            return str;
        }

        int len = str.length();
        StringBuffer sb = new StringBuffer(len + 64);
        for (int i = 0; i < len; ++i) {
            char c = str.charAt(i);
            String res = printable(c);
            if (res == null) {
                sb.append(c);
            } else {
                sb.append(res);
            }
        }
        return sb.toString();
    }

    protected static String quotedPrintable(String str)
    {
        if (str == null || str.length() == 0) {
            return "[0]''";
        }
        return "[len: "+str.length()+"] '"+printable(str)+"'";
    }
}
