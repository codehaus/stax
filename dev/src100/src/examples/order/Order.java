package examples.order;

public class Order {
  private int idRef;
  private String part;
  private String date;

  public Order(int idRef, String part, String shipDate){
    this.idRef = idRef;
    this.part = part;
    this.date = shipDate;
  }

  public String toString() {
    return "["+idRef+"]["+part+"]["+date+"]";
  }
}
