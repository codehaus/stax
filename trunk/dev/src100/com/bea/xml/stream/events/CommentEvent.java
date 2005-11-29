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
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.Comment;
public class CommentEvent 
  extends CharactersEvent
  implements Comment 
{
  public CommentEvent() {init();}
  public CommentEvent(String data) {
    init();
    setData(data);
  }

  protected void init() {setEventType(XMLEvent.COMMENT); }
  public String getText() { 
    return getData();
  }
  public String toString() {
    if (getText().length() ==0) return "<!---->";
    else 
      return ("<!--"+getText()+"-->");
  }

}