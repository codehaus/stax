package examples.binding;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.StringWriter;
import java.util.Iterator;

public class BeanGen {

  private String destdir;
  public BeanGen() {}

  public void setDestDir(String dir) {
    destdir = dir;
  }

  public String getDestDir() {
    return destdir;
  }

  private static String capital(String oldString) {
   return (oldString.substring(0,1)).toUpperCase()+
      oldString.substring(1,oldString.length());
  }

  public void generatePropertyDecl(Property p,
                                   PrintWriter writer) {
    writer.print("   private ");
    if (p.getType() == Property.STRING)
      writer.println("String _"+p.getName()+";");
    if (p.getType() == Property.INTEGER)
      writer.println("int _"+p.getName()+";");
    if (p.getType() == Property.BEAN)
      writer.println(p.getRefType()+" _"+p.getName()+";");
  }
  public void generateSetters(Property p,
                              PrintWriter writer) {
    String name = capital(p.getName());
    if (p.getType() == Property.STRING) {
      writer.print("   public void set"+name+"(String val)");
      writer.println("{ _"+p.getName()+"=val;}");
      writer.print("   public String get"+name+"()");
      writer.println("{ return _"+p.getName()+";}");
    }
    if (p.getType() == Property.INTEGER) {
      writer.print("   public void set"+name+"(int val)");
      writer.println("{ _"+p.getName()+"=val;}");
      writer.print("   public int get"+name+"()");
      writer.println("{ return _"+p.getName()+";}");
    }
    if (p.getType() == Property.BEAN) {
      writer.print("   public void set"+name+"("+p.getRefType()+" val)");
      writer.println("{ _"+p.getName()+"=val;}");
      writer.print("   public "+p.getRefType()+" get"+name+"()");
      writer.println("{ return _"+p.getName()+";}");
    }
  }

  public void generateBean(String packageName,
                           BeanInfo info) 
    throws IOException
  {

    String className = capital(info.getName());
    PrintWriter writer = new PrintWriter(new FileWriter(getDestDir()+"/"+className+".java"));
    writer.println("//**-- "+className+".java--**"); 
    writer.println("package "+packageName+";");
    writer.println("public class "+className+" {");
    writer.println("   public "+className+"(){}");
    Property[] properties = info.getProperties();
    for(int i=0; i < properties.length; i++) {
      generatePropertyDecl(properties[i], writer);
    }
    properties = info.getProperties();
    for(int i=0; i < properties.length; i++) {
      generateSetters(properties[i], writer);
    }
    writer.println("}");
    writer.close();
  }

  public void generate(BeanDescription desc)
    throws IOException
  {
    
    Iterator i = desc.getBeans();
    while(i.hasNext()) {
      BeanInfo info = (BeanInfo) i.next();
      generateBean(desc.getPackageName(),
                   info);
      generateCodec(desc.getNamespaceName(),
                    desc.getPackageName(),
                   info);
    }
  }

 
  public void generatePropertySerializer(String namespace,
                                         String packageName,
                                         Property p,
                                         PrintWriter writer) {
    String name = capital(p.getName());
    if (p.getType() == Property.STRING) {
      writer.println("      writer.writeStartElement(_namespace,"+
                     quote(name)+");");
 
      writer.println("      writer.writeCharacters(obj.get"+name+"());");
      writer.println("      writer.writeEndElement();");
    }
    if (p.getType() == Property.INTEGER) {
      writer.println("      writer.writeStartElement(_namespace,"+
                     quote(name)+");");

      writer.println("      writer.writeCharacters(Integer.toString(obj.get"+name+"()));");
      writer.println("      writer.writeEndElement();");
    }
    if (p.getType() == Property.BEAN) {
      writer.println("      "+packageName+"."+p.getRefType()+"Codec.serialize(obj.get"+name+"()"+
                   ",writer);");
    }
 
  }

  public String quote(String val) {
    return "\""+val+"\"";
  }

  public void generateSerializer(String namespace,
                                 String packageName,
                                 BeanInfo info,
                                 PrintWriter writer)
  {
    String className = capital(info.getName());
    writer.println("   public static void serialize("+className+
                   " obj, XMLStreamWriter writer)");
    writer.println("      throws XMLStreamException");
    writer.println("   {");
    writer.println("      writer.setPrefix(\"a\",_namespace);");
    writer.println("      writer.writeStartElement(_namespace,"+
                   quote(className)+");");
    writer.println("      writer.writeNamespace(\"a\",_namespace);");
    Property[] properties = info.getProperties();
    for(int i=0; i < properties.length; i++) {
      generatePropertySerializer(namespace,packageName,properties[i], writer);
    }
    writer.println("      writer.writeEndElement();");
    writer.println("   }");
  }

  public void skipSpace(PrintWriter writer) {
    writer.println("      while(reader.isWhiteSpace() || !(reader.isStartElement() || reader.isEndElement() || reader.isCharacters())) reader.next();");
  }

  public void next(PrintWriter writer) {
    writer.println("      reader.next();");
  }

  public void generatePropertyDeserializer(String namespace,
                                         String packageName,
                                         Property p,
                                         PrintWriter writer) {
    String name = capital(p.getName());
    if (p.getType() == Property.STRING) {
      writer.println("      reader.require(XMLStreamReader.START_ELEMENT,"+
                     "_namespace,"+
                     quote(name)+");");
      next(writer);

      writer.println("      obj.set"+name+"("+
                     "reader.getText());");
      next(writer);
      writer.println("      reader.require(XMLStreamReader.END_ELEMENT,"+
                     "_namespace,"+
                     quote(name)+");");
      next(writer);

    }
    if (p.getType() == Property.INTEGER) {
      writer.println("      reader.require(XMLStreamReader.START_ELEMENT,"+
                     "_namespace,"+
                     quote(name)+");");
      next(writer);

      writer.println("      obj.set"+name+"("+
                     "(new Integer(reader.getText()).intValue()));");
      next(writer);
      writer.println("      reader.require(XMLStreamReader.END_ELEMENT,"+
                     "_namespace,"+
                     quote(name)+");");
      next(writer);

    }
    if (p.getType() == Property.BEAN) {
      writer.println("      obj.set"+name+"("+packageName+"."+p.getRefType()+"Codec.deserialize("
                     +"reader));");
    }

  }

  public void generateDeserializer(String namespace,
                                   String packageName,
                                   BeanInfo info,
                                   PrintWriter writer)
  {
    String className = capital(info.getName());
    writer.println("   public static "+className+" deserialize("+
                   " XMLStreamReader reader)");
    writer.println("      throws XMLStreamException");
    writer.println("   {");
    skipSpace(writer);
    writer.println("      "+className+" obj = new "+packageName+
                   "."+className+"();");
    writer.println("      reader.require(XMLStreamReader.START_ELEMENT,"+
                   "_namespace,"+
                   quote(className)+");");
    next(writer);
    Property[] properties = info.getProperties();
    for(int i=0; i < properties.length; i++) {
      generatePropertyDeserializer(namespace,packageName,properties[i], writer);
    }
    writer.println("      reader.require(XMLStreamReader.END_ELEMENT,"+
                   "_namespace,"+
                   quote(className)+");");
    next(writer);
    writer.println("      return obj;");
    writer.println("   }");
  }

  public void generateCodec(String namespace,
                            String packageName,
                            BeanInfo info)
    throws IOException
  {

    String className = capital(info.getName()+"Codec");
    PrintWriter writer = new PrintWriter(new FileWriter(getDestDir()+"/"+className+".java"));
    writer.println("//**-- "+className+".java--**"); 
    writer.println("package "+packageName+";");
    writer.println("import javax.xml.stream.XMLStreamWriter;");
    writer.println("import javax.xml.stream.XMLStreamReader;");
    writer.println("import javax.xml.stream.XMLStreamException;");
    writer.println("public class "+className+" {");
    writer.println("   private static final String _namespace=\""+namespace+"\";");
    writer.println("   public "+className+"(){}");
    generateSerializer(namespace,packageName,info,writer);
    generateDeserializer(namespace,packageName,info,writer);
    writer.println("}");
    writer.close();
  }

  public static void main(String args[]) 
    throws Exception 
  {
    Parser parser = new Parser(new java.io.FileReader(args[0]));
    BeanDescription description = parser.parse();
    
    StringWriter w = new StringWriter();
    BeanGen generator = new BeanGen();
    generator.setDestDir(args[1]);
    generator.generate(description);
  }
}



