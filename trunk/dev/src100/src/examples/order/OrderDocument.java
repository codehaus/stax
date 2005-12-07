package examples.order;

import java.util.*;

public class OrderDocument {
  public ArrayList orders = new ArrayList();
  public ArrayList customers = new ArrayList();
  public OrderDocument(){}
  public void addOrder(Order o) {
    orders.add(o);
  }

  public void addCustomer(Customer c) {
    customers.add(c);
  }

  public String toString() {
    StringBuffer b = new StringBuffer();
    b.append("Order[\n");
    Iterator i = customers.iterator();
    while(i.hasNext()) b.append("\t"+i.next()+"\n");
    i = orders.iterator();
    while(i.hasNext()) b.append("\t"+i.next()+"\n");
    b.append("]Order\n");
    return b.toString();
  }
}
