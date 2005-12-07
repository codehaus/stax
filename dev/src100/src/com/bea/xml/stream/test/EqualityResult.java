package com.bea.xml.stream.test;
public class EqualityResult {
  boolean value;
  String message;

  public EqualityResult(boolean equal,
                        String message) {
    this.value = equal;
    this.message = message;
  }

  public boolean getValue() {
    return value;
  }

  public String getMessage() {
    return message;
  }

  public String toString() {
    if (value)
      return "EQUAL with Message:["+message+"]";
    else
      return "NOT EQUAL with Message:["+message+"]";
  }
}
