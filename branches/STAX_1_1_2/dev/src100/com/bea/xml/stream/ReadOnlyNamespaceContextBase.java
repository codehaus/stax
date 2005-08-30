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

import java.util.Iterator;
import java.util.HashSet;

import javax.xml.namespace.NamespaceContext;

/**
 * <p> This class provides a ReadOnlyNamespace context that 
 * takes a snapshot of the current namespaces in scope </p>
 */

public class ReadOnlyNamespaceContextBase 
  implements NamespaceContext
{
  private String[] prefixes;
  private String[] uris;

  public ReadOnlyNamespaceContextBase(String[] prefixArray,
                                      String[] uriArray,
                                      int size) 
  {
    prefixes = new String[size];
    uris = new String[size];
    System.arraycopy(prefixArray, 0, prefixes, 0, prefixes.length);
    System.arraycopy(uriArray, 0, uris, 0, uris.length);
   }

  public String getNamespaceURI(String prefix) {
    if (prefix == null)
      throw new IllegalArgumentException("Prefix may not be null.");
    if(!"".equals(prefix)) {
      for( int i = uris.length -1; i >= 0; i--) {
        if( prefix.equals( prefixes[ i ] ) ) {
          return uris[ i ];
        }
      }
      if("xml".equals( prefix )) {
        return MXParser.XML_URI;
      } else if("xmlns".equals( prefix )) {
        return MXParser.XMLNS_URI;
      }
    } else {
      for( int i = uris.length -1; i >= 0; i--) {
        if( prefixes[ i ]  == null ) {
          return uris[ i ];
        }
      }
    }
    return null;
  }
  public String getPrefix(String uri) {
    if (uri == null)
      throw new IllegalArgumentException("uri may not be null");
    if ("".equals(uri))
      throw new IllegalArgumentException("uri may not be empty string");

    if(uri != null) {
            for( int i = uris.length -1; i >= 0; i--) {
                if( uri.equals( uris[ i ] ) ) {
                    return checkNull(prefixes[ i ]);
                }
            }
        } 
    return null;
  }

  public String getDefaultNameSpace() {
    for( int i = uris.length -1; i >= 0; i--) {
      if( prefixes[ i ]  == null ) {
        return uris[ i ];
      }
    }
    return null;
  }
  
  private String checkNull(String s) {
    if (s == null) return "";
    return s;
  }

  public Iterator getPrefixes(String uri) {
    if (uri == null)
      throw new IllegalArgumentException("uri may not be null");
    if ("".equals(uri))
      throw new IllegalArgumentException("uri may not be empty string");
    HashSet s = new HashSet();
    for( int i = uris.length -1; i >= 0; i--) {
      String prefix = checkNull(prefixes[i]);
      if( uri.equals( uris[ i ] ) && !s.contains(prefix)) {
        s.add(prefix);
      }
    }
    return s.iterator();
  }

  public String toString() {
    StringBuffer b = new StringBuffer();
    for (int i=0; i < uris.length; i++) {
      b.append("["+checkNull(prefixes[i])+"<->"+uris[i]+"]");
    }
    return b.toString();
  }

  public static void main(String[] args) throws Exception {
    MXParser p = new MXParser();
    p.setInput(new java.io.FileReader(args[0]));
    while (p.hasNext()) {
      if (p.isStartElement()) {
        System.out.println("context["+p.getNamespaceContext()+"]");
        Iterator i = p.getNamespaceContext().getPrefixes("a");
        while (i.hasNext())
          System.out.println("Found prefix:"+i.next());
      }
      p.next();
    }
  }
}
