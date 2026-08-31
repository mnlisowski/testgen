package sample.maven;

public final class Customer {
    private final String segment;
    private final CustomerType type;

    public Customer(String segment, CustomerType type) {
        this.segment = segment;
        this.type = type;
    }

    public String getSegment() {
        return segment;
    }

    public CustomerType getType() {
        return type;
    }
}
