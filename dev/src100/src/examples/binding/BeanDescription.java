package examples.binding;

import java.util.HashMap;
import java.util.Iterator;

public class BeanDescription {
  private String packageName;
  private String namespace;
  private HashMap beans = new HashMap();
  public BeanDescription() {}
  public void setPackageName(String name) {
    this.packageName = name;
  }
  public String getPackageName() { return packageName; }
  public void setNamespaceName(String name) {
    this.namespace = name;
  }
  public String getNamespaceName() { return namespace; }

  public void addBean(BeanInfo info) {
    beans.put(info.getName(),info);
  }
  public BeanInfo getBean(String name) {
    return (BeanInfo) beans.get(name);
  }
  public Iterator getBeans() {
    return beans.values().iterator();
  }
  public String toString() {
    StringBuffer b = new StringBuffer();
    b.append("Package: "+packageName+"\n"+
      "Namespace: "+namespace+"\n");
    Iterator i = beans.values().iterator();
    while(i.hasNext()) {
      b.append(i.next().toString());
    }
    return b.toString(); 
  }
}
