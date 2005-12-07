package com.bea.xml.stream.test;

import com.bea.xml.stream.util.ElementTypeNames;
import com.bea.xml.stream.XMLStreamPlayer;
import com.bea.xml.stream.XMLStreamRecorder;
import com.bea.xml.stream.filters.TypeFilter;
import com.bea.xml.stream.filters.NameFilter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.io.File;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.StreamFilter;
import javax.xml.stream.events.*;

import javax.xml.stream.XMLStreamException;

public class Recorder {
  public String resType;
  public String resName;
  public String filter;
  public QName name;
  private StringBuffer fSuffix;
  private String nSuffix;
  
  public Recorder() { }

  public Recorder(String[] params) throws Exception{
    if(params[0] == null || params[1] == null)
      throw new Exception("You must provide resourceType and resourceName");
    this.resType = params[0];
    this.resName = params[1];
    if(params.length == 3){
      StringTokenizer st = new StringTokenizer(params[2], "=");
      String typeorname = st.nextToken();
      String value = st.nextToken();
      if (typeorname.equals("filter")){
        this.filter = new String(value);
      } else {
        StringTokenizer nst = new StringTokenizer(value, "*");
        this.name = new QName(nst.nextToken(), nst.nextToken());
      }
    }
  }


  private StreamFilter resolveFilter() throws Exception {
    int cnt = 0;
    StringTokenizer st = new StringTokenizer(filter, "*");
    fSuffix = new StringBuffer(st.countTokens());
    TypeFilter f = new com.bea.xml.stream.filters.TypeFilter();
    while(st.hasMoreTokens()) {
      String eventName = st.nextToken();
      System.out.println("eName of filter" + ++cnt + " is: " + eventName);
      int eventType = ElementTypeNames.getEventType(eventName);
      System.out.println("its event val is: " + eventType);
      f.addType(eventType);
      fSuffix.append("_");
      fSuffix.append(eventName);
    }
    return (StreamFilter)f;
  }

    private StreamFilter resolveName() throws Exception {
    int cnt = 0;
    nSuffix = "_" + name.getNamespaceURI() + "_" 
                  + name.getLocalPart();
    System.out.println("namespace of filter" + " is: " + nSuffix);
                      
    NameFilter f = new com.bea.xml.stream.filters.NameFilter(name);
    return (StreamFilter)f;
  }

  private void recordStream(String fName) throws Exception {
    XMLInputFactory xmlif = XMLInputFactory.newInstance();
    XMLOutputFactory xmlof = XMLOutputFactory.newInstance();
    XMLStreamReader xmlr;
    XMLStreamRecorder r;
    String fPrefix = "";
    String outFile = null;

    StringTokenizer st = new StringTokenizer(resName, ".");
    if(st.hasMoreTokens())
      fPrefix = st.nextToken();

    if(filter != null) {
      StreamFilter f =  resolveFilter();
      xmlr = xmlif.createFilteredReader( xmlif.createXMLStreamReader(new java.io.FileReader(fName)), f);
      outFile = fPrefix + "_filtered" + fSuffix + ".stream";
      System.out.println("Resultant master file is: " + outFile);
    } else if (name != null){
      StreamFilter f =  resolveName();
      xmlr = xmlif.createFilteredReader( xmlif.createXMLStreamReader(new java.io.FileReader(fName)), f);
      outFile = fPrefix + "_filtered" + nSuffix + ".stream";
      System.out.println("Resultant master file is: " + outFile);
    }
    else {
      xmlr = xmlif.createXMLStreamReader(new java.io.FileReader(fName));
      outFile = fPrefix + ".stream";
    }
    r = new XMLStreamRecorder(
          new java.io.OutputStreamWriter(
            new java.io.FileOutputStream(outFile)));
    while (xmlr.hasNext()) {
      r.write(xmlr);
      xmlr.next();
    }
    r.write(xmlr);
    r.flush();
  }
    
  public void startRecording() throws Exception {
    if("-file".equals(resType)) {
      recordStream(resName);
    }
    else if("-dir".equals(resType)) {
      File d = new File(resName);
      if(!d.isDirectory())
        return;
      File[] f = d.listFiles();
      for(int i = 0; i < f.length; i++) {
        recordStream(f[i].getName());
      }
    }
    else {
      System.out.println("Input not properly specified");
    }    
  }

  public static void main(String args[]) throws Exception {

    if("-help".equals(args[0])) {
      System.out.println(" Usage : ");
      System.out.println(" java com.bea.xml.stream.test.Recorder [$option] [$resource] [filter=$filter | namespace=$namespace]");
      System.out.println(" $option can be [-file|-dir]");
      System.out.println(" $resource is either the file name or a directory name");
      System.out.println(" $filter is a set of Events seperated by \"*\" ");
      System.out.println(" $namespace is the namespace to be filtered on ");
      System.exit(0);
    }

    Recorder rec = new Recorder(args);
    rec.startRecording();

  }

}
