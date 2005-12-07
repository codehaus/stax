package com.bea.xml.stream.test;

import junit.framework.*;
import org.apache.log4j.Logger;
import java.lang.reflect.*;


/**
 * BaseTestCase for all the Stax Tests
 *
 */
public class BaseTestCase extends TestCase {

  protected Logger logger;
  protected GlobalCounter globalCounter;

  public BaseTestCase() {
    logger = Logger.getLogger("JSR173");
    globalCounter = GlobalCounter.getInstance();
  }

  public BaseTestCase(String fName) { 
    super(fName);
    logger = Logger.getLogger("JSR173");    
  }

  public static void main (String[] args) throws Exception{
    junit.textui.TestRunner.run (suite());
  }

  protected void setUp() {
  }

  public static Test suite() {
    return new TestSuite(SimpleReaderTest.class);
  }
    
  protected void runTest() throws Throwable {
    Method[] runMethods= null;
    try {
      // use getMethod to get all public inherited
      // methods. getDeclaredMethods returns all
      // methods of this class but excludes the
      // inherited ones.
      runMethods = getClass().getDeclaredMethods();
    } catch (Exception e) {
      fail("No Methods declared in this test suite");
    }
    
    for( int i=0; i < runMethods.length; i++) {
      if (!runMethods[i].getName().startsWith("test")) {
        continue;
      }
    
      try {
        runMethods[i].invoke(this, new Class[0]);
      }
      catch (InvocationTargetException e) {
        e.fillInStackTrace();
        throw e.getTargetException();
      }
      catch (IllegalAccessException e) {
        e.fillInStackTrace();
        throw e;
      }
    }
  }
}


