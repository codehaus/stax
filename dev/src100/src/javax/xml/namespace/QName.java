/*
 * Copyright 2001 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package javax.xml.namespace;

import java.io.Serializable;

/**
 * <code>QName</code> class represents a combined definition of a
 * <b>qualified name</b> as defined in the following two
 * specifications:
 * <ul>
 *    <li><a href="http://www.w3.org/TR/xmlschema-2/#QName">XML
 *    Schema Part2: Datatypes specification</a></li>
 *    <li><a href="http://www.w3.org/TR/REC-xml-names/#ns-qualnames">
 *    Namespaces in XML</a></li>
 * </ul>
 *
 * <p>The value of a QName contains a <b>namespaceURI</b> and a
 * <b>localPart</b>.  The localPart provides the local part of the
 * qualified name. The namespaceURI is a URI reference identifying the
 * namespace.
 *
 * <p>The <b>prefix</b> is included in the QName class to retain
 * lexical information <i>where present</i> in an XML input
 * source. The prefix is <i>NOT</i> used to compute the hash code or
 * in the <code>equals</code> operation. In other words, equality is
 * defined only using the <b>namespaceURI</b> and the
 * <b>localPart</b>.
 *
 *  @version 1.1
 **/
public class QName implements java.io.Serializable {

    private String namespaceURI;
    private String localPart;

    private String prefix;


    /**
     *  Constructor for the QName with just a local part. This assigns
     *  an empty string as the default values for
     *  <code>namespaceURI</code>, <code>prefix</code> and
     *  <code>localPart</code>.
     *
     *
     *  @param localPart     Local part of the QName
     *  @throws java.lang.IllegalArgumentException If null
     *                       local part is specified
     **/
    public QName(String localPart) {
        this("", localPart);
    }


    /**
     * Constructor for the QName with a namespace URI and a local
     * part. This method assigns an empty string as the default value
     * for <code>prefix</code>.
     *
     *  @param namespaceURI  Namespace URI for the QName
     *  @param localPart     Local part of the QName
     *  @throws java.lang.IllegalArgumentException If null
     *                       local part or namespaceURI is specified
     **/
    public QName(String namespaceURI, String localPart) {
        this(namespaceURI, localPart, "");
    }


    /**
     * Constructor for the QName.
     *
     *  @param namespaceURI  Namespace URI for the QName
     *  @param localPart     Local part of the QName
     *  @param prefix        The prefix of the QName
     *  @throws java.lang.IllegalArgumentException If null
     *                       local part or prefix is specified
     **/
    public QName(String namespaceURI, String localPart, String prefix) {
        if (localPart == null)
            throw
                new IllegalArgumentException("Local part not allowed to be null");

        if (namespaceURI == null)
          namespaceURI = "";
        //     throw new IllegalArgumentException("namespaceURI not allowed to be null");

        if (prefix == null)
          prefix = "";
        //  throw new IllegalArgumentException("prefix not allowed to be null");

        this.namespaceURI = namespaceURI;
        this.localPart = localPart;
        this.prefix = prefix;
    }





    /** Gets the Namespace URI for this QName.
     *  @return Namespace URI
     **/
    public String getNamespaceURI() {
        return namespaceURI;
    }

    /** Gets the Local part for this QName.
     *  @return Local part
     **/
    public String getLocalPart() {
        return localPart;
    }

    /**
     * Gets the prefix for this QName. Note that the prefix assigned
     * to a QName may not be valid in a different context. For
     * example, a QName may be assigned a prefix in the context of
     * parsing a document but that prefix may be invalid in the
     * context of a different document.
     *
     *  @return a <code>String</code> value of the prefix.
     */
    public String getPrefix() {
        return prefix;
    }

    /** Returns a string representation of this QName.
     *
     *  @return a string representation of the QName
     **/
    public String toString() {
        if (namespaceURI.equals("")) {
            return localPart;
        }
        else {
            return "{" + namespaceURI + "}" + localPart;
        }
    }

    /** Returns a QName holding the value of the specified String.
     *
     *  The string must be in the form returned by the QName.toString()
     *  method, i.e. "{namespaceURI}localPart", with the "{namespaceURI}"
     *  part being optional.
     *
     *  This method doesn't do a full validation of the resulting QName.
     *  In particular, it doesn't check that the resulting namespace URI
     *  is a legal URI (per RFC 2396 and RFC 2732), nor that the resulting
     *  local part is a legal NCName per the XML Namespaces specification.
     *
     *  @param s the string to be parsed
     *  @return QName corresponding to the given String
     *  @throws java.lang.IllegalArgumentException If the specified
     *          String cannot be parsed as a QName
     **/
    public static QName valueOf(String s) {
        if (s == null || s.equals("")) {
            throw new IllegalArgumentException("invalid QName literal");
        }

        if (s.charAt(0) == '{') {
            // qualified name
            int i = s.indexOf('}');
            if (i == -1) {
                throw new IllegalArgumentException("invalid QName literal");
            }
            if (i == s.length() - 1) {
                throw new IllegalArgumentException("invalid QName literal");
            }
            return new QName(s.substring(1, i), s.substring(i + 1));
        }
        else {
            return new QName(s);
        }
    }

    /** Returns a hash code value for this QName object. The hash code
     *  is based on both the localPart and namespaceURI parts of the
     *  QName. This method satisfies the  general contract of the
     * {@link java.lang.Object#hashCode() Object.hashCode} method. </p>
     *
     *  @return A hash code value for this Qname object
     **/
    public final int hashCode() {
        return namespaceURI.hashCode() ^ localPart.hashCode();
    }

    /** Tests this QName for equality with another object.
     *
     *  <p>If the given object is not a QName or is null then this method
     *  returns <tt>false</tt>.
     *
     *  <p>For two QNames to be considered equal requires that both
     *  localPart and namespaceURI must be equal. This method uses
     *  <code>String.equals</code> to check equality of localPart
     *  and namespaceURI.
     *
     *  <p>This method satisfies the general contract of the {@link
     *  java.lang.Object#equals(Object) Object.equals} method. </p>
     *
     *
     *  @param   obj the reference object with which to compare.
     *  @return  <tt>true</tt> if the given object is identical to this
     *           QName: <tt>false</tt> otherwise.
     **/
    public final boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof QName)) {
            return false;
        }

        QName qname = (QName) obj;

        return this.localPart.equals(qname.localPart)
            && this.namespaceURI.equals(qname.namespaceURI);
    }
}
