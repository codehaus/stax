package examples.order;

public class Customer {
  private int id;
  private String name;
  private String company;
  public Customer(int id,
                  String name,
                  String company) {
    this.id = id;
    this.name = name;
    this.company = company;
  }
  public String toString() {
    return "["+id+"]["+name+"]["+company+"]";
  }
}
