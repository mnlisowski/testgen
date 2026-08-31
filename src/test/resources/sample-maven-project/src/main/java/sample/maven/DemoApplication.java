package sample.maven;

public final class DemoApplication {
    private DemoApplication() {
    }

    public static void main(String[] args) {
        DiscountService service = new DiscountService(500);
        Customer customer = new Customer("PREMIUM", CustomerType.VIP);
        Order order = new Order(620, customer, "PAID");

        service.calculate(order, "BLACK_FRIDAY");
        service.shippingFee(99);
    }
}
