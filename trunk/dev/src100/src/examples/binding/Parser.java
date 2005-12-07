package examples.binding;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.io.IOException;

public class Parser {
  private Reader reader;
  private char current;
  private boolean EOF=false;
  private int line = 1;

  public Parser(Reader r)
    throws IOException
  {
    this.reader = r;
    accept();
  }

  public boolean reachedEOF() {
    return EOF;
  }

  public char current() {
    return current;
  }

  public boolean isSpace() {
    return current == ' ' || current == '\r'
      || current == '\t' || current == '\n';
  }

  public void skipSpace() 
      throws IOException
  {
    while (isSpace() && !reachedEOF()) accept();
  }

  public String error() {
    return "\nParse error at line:"+line+" char:"+current();
  }

  public void accept() 
    throws IOException
  {
    int c = reader.read();
    if (c == -1) EOF = true;
    current = (char) c;
    if (c == '\n') line ++;
  }

  public void accept(char c) 
    throws IOException
  {
    if (current() == c)
      accept();
    else
      throw new IOException("Unable to match character["+c+"]"+error());
  }

  public void accept(String s) 
    throws IOException
  {
    int i=0;
    while (i < s.length()) {
      if (s.charAt(i) == current()) { accept(); i++; }
      else 
        throw new IOException("Unable to match string["+s+"]"+error());
    }
  }

  public String readString() 
    throws IOException
  {
    StringBuffer b = new StringBuffer();
    while (current() != ' ' &&
           current() != '\n' &&
           current() != '\r' &&
           current() != '\t' &&
           current() != ';' &&
           !reachedEOF()) {
      b.append(current());
      accept();
    }
    return b.toString();
  }

  public BeanDescription parse() 
    throws IOException
  {
    BeanDescription desc = new BeanDescription();
    skipSpace();
    accept("Package:");
    skipSpace();
    String packageName = readString();
    //System.out.println("package:"+packageName);
    skipSpace();
    accept("Namespace:");
    skipSpace();
    String namespace = readString();
    //System.out.println("namespace:"+namespace);
    skipSpace();
    desc.setPackageName(packageName);
    desc.setNamespaceName(namespace);
    while(!reachedEOF()) {
      desc.addBean(parseBeanInfo());
    }
    return desc;
  }

  public BeanInfo parseBeanInfo() 
    throws IOException
  {
    BeanInfo info = new BeanInfo();
    skipSpace();
    String name = readString();
    info.setName(name);
    //System.out.println("Reading:"+name);
    skipSpace();
    accept('{');
    while(current()!='}') {
      skipSpace();
      info.addProperty(parseProperty());
      skipSpace();
    }
    accept('}');
    skipSpace();
    return info;
  }

  public Property parseProperty() 
    throws IOException
  {
    skipSpace();
    String type = readString();
    skipSpace();
    String prop = readString();
    skipSpace();
    accept(';');
    if ("Integer".equals(type)) {
      return new Property(prop,Property.INTEGER);
    } else if ("String".equals(type)) {
      return new Property(prop,Property.STRING);
    } else {
      return new Property(prop,Property.BEAN,type);
    }
  }

  public static void main(String args[]) 
    throws Exception 
  {
    Parser parser = new Parser(new java.io.FileReader(args[0]));
    BeanDescription description = parser.parse();
    System.out.println(description);
  }
}

