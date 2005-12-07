package examples.binding;

import java.util.ArrayList;
import java.util.Iterator;

public class BeanInfo {
  ArrayList properties = new ArrayList();
  String name;

  public BeanInfo() {}
  public void setName(String name) {
    this.name = name;
  }
  public String getName() { return name; }
  public void addProperty(Property property) {
    properties.add(property);
  }
  public Property[] getProperties() {
    Property[] val = new Property[properties.size()];
    int i=0;
    Iterator I=properties.iterator();
    while (I.hasNext()) {
      val[i] = (Property) I.next();
      i++;
    }
    return val;
  }
  public String toString() {
    StringBuffer b = new StringBuffer();
    b.append(getName());
    b.append(" {\n");
    Property[] p=getProperties();
    for(int i=0; i < p.length; i++) {
      b.append(p[i]+"\n");
    }
    b.append("}\n");
    return b.toString();
  }
  
}
