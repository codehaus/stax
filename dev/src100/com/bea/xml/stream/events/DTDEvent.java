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
package com.bea.xml.stream.events;

import java.util.List;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.DTD;

public class DTDEvent 
  extends BaseEvent
  implements DTD
{
  private List notations;
  private List entities;
  private String dtd;
  public DTDEvent(){}
  public DTDEvent(String dtd) {
    init();
    setDTD(dtd);
  }

  protected void init() {setEventType(XMLEvent.DTD); }
  public void setDTD(String dtd) {
    this.dtd=dtd;
  }

  public Object getProcessedDTD() {
    return null;
  }
  public String getDocumentTypeDeclaration() {
    return dtd;
  }
  public String toString() {
    return dtd;
  }
  public List getEntities() {
    return entities;
  }
  public List getNotations() {
    return notations;
  }
}
