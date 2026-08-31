package sample.maven;

public final class Order {
    private final int total;
    private final Customer customer;
    private final String status;

    public Order(int total, Customer customer, String status) {
        this.total = total;
        this.customer = customer;
        this.status = status;
    }

    public int getTotal() {
        return total;
    }

    public Customer getCustomer() {
        return customer;
    }

    public String getStatus() {
        return status;
    }
}
