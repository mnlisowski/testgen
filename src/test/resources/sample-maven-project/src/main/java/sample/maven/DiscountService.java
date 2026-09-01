package sample.maven;

public final class DiscountService {
    private final int vipThreshold;

    public DiscountService() {
        this(500);
    }

    public DiscountService(int vipThreshold) {
        this.vipThreshold = vipThreshold;
    }

    public int calculate(Order order, String couponCode) {
        if (order == null) {
            return 0;
        }

        int discount = 0;

        if (order.getTotal() > vipThreshold) {
            discount += 20;
        } else {
            discount += 5;
        }

        if (order.getCustomer().getType() == CustomerType.VIP) {
            discount += 10;
        }

        if ("BLACK_FRIDAY".equals(couponCode)) {
            discount += 30;
        }

        switch (order.getStatus()) {
            case "PAID" -> discount += 3;
            case "CANCELLED" -> discount = 0;
            default -> discount += 1;
        }

        return Math.min(discount, order.getTotal());
    }

    public int shippingFee(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }

        if (amount > 100) {
            return 0;
        }

        return 10;
    }

    int localRate() {
        return vipThreshold;
    }
}
