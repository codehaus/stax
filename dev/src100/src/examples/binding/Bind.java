package test;

import javax.xml.stream.*;

public class Bind {
  public static void main(String args[]) throws Exception {
    Employee e = new Employee();
    Job j = new Job();
    j.setTitle("Engineer");
    j.setYearsEmployed(2);
    e.setName("Chris Fry");
    e.setJob(j);

    System.out.println("Created the Employee Bean");
    
    XMLOutputFactory f = XMLOutputFactory.newInstance();
    java.io.OutputStream out = new java.io.FileOutputStream("emp.xml");
    XMLStreamWriter w = f.createXMLStreamWriter(out);
    EmployeeCodec.serialize(e,w);
    w.close();
    System.out.println("Serialized the Employee Bean to 'emp.xml'");

    XMLInputFactory i = XMLInputFactory.newInstance();
    java.io.InputStream in = new java.io.FileInputStream("emp.xml");
    XMLStreamReader r = i.createXMLStreamReader(in);
    Employee e2 = EmployeeCodec.deserialize(r);
    
    System.out.println("Deserialized the Employee Bean from 'emp.xml'");
    System.out.println("Emp: "+e2.getName()+" "+
                       e2.getJob().getTitle()+" "+
                       e2.getJob().getYearsEmployed());
    System.out.println("Successful execution");
  }
}
