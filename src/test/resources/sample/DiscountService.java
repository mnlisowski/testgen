package sample;

public class DiscountService {

    public int calculate(Order order) {
        if (order.total() > 100) {
            return 20;
        }

        if ("PREMIUM".equals(order.customer().type())) {
            return 10;
        }

        return 0;
    }
}
