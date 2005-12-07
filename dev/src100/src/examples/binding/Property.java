package examples.binding;

public class Property {
  public static final int STRING=1;
  public static final int INTEGER=2;
  public static final int BEAN=3;

  private String name;
  private int type;
  private String refType;
  public Property(String name,
                  int type) {
    this.name = name;
    this.type = type;
  }
  public Property(String name,
                  int type,
                  String refType) {
    this.name = name;
    this.type = type;
    this.refType = refType;
  }

  public String getName() { return name; }
  public int getType() { return type; }
  public String getRefType() { return refType; }
  public String toString() {
    if (type == STRING) {
      return "String "+name;
    } else if (type == INTEGER) {
      return "Integer "+name;
    } else {
      return refType+" "+name;
    }
  }
}
