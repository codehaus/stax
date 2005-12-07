package com.bea.xml.stream.test;


/**
 * @author Copyright (c) 2003 by BEA WebXpress. All Rights Reserved.
 */

public class GlobalCounter {

  public int count;
  public static GlobalCounter counter;

  protected GlobalCounter() {
    count = 0;
  }
  
  public static GlobalCounter getInstance() {
    if(counter == null) {
      counter = new GlobalCounter();
      return counter;
    } 
    else {
      return counter;
    }
  }
  
  public void increment() {
    count++;
  }

  public int getCount() {
    return count;
  }

}
