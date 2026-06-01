package sample;

public class Order {
    private final int total;
    private final Customer customer;

    public Order(int total, Customer customer) {
        this.total = total;
        this.customer = customer;
    }

    public int total() {
        return total;
    }

    public Customer customer() {
        return customer;
    }
}
