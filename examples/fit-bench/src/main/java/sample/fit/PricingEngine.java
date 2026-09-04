package sample.fit;

public class PricingEngine {
    public int finalPrice(CheckoutContext context) {
        if (context == null) {
            return -1;
        }
        if (!context.isInternallyConsistent()) {
            return -2;
        }

        ProductBundle bundle = context.getBundle();
        CustomerProfile customer = context.getCustomer();
        CouponInfo coupon = context.getCoupon();

        int price = bundle.getTotalAmount();
        if (price < 0) {
            return -3;
        }
        if (coupon != null && coupon.isApplicable(bundle, customer)) {
            price -= coupon.discountFor(price);
        }
        if (customer.getTier() == CustomerTier.VIP) {
            price -= 20;
        }
        if (bundle.isDigital()) {
            price -= 5;
        }
        if (context.hasCampaign("BLACK")) {
            price -= 15;
        }
        if (price < 0) {
            return 0;
        }
        return price;
    }

    public int surcharge(int amount, PaymentMethod method, boolean weekend) {
        int surcharge = 0;
        if (amount < 0) {
            return -1;
        }
        if (weekend) {
            surcharge += 5;
        }
        if (amount > 1000) {
            surcharge += 20;
        }
        switch (method) {
            case CARD:
                surcharge += 2;
                break;
            case CRYPTO:
                surcharge += 25;
                break;
            case CASH:
                surcharge += 0;
                break;
            default:
                surcharge += 1;
                break;
        }
        return surcharge;
    }
}
