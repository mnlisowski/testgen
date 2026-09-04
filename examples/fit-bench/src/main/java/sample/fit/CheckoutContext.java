package sample.fit;

public class CheckoutContext {
    private final CustomerProfile customer;
    private final ProductBundle bundle;
    private final CouponInfo coupon;
    private final PaymentAttempt payment;
    private final ShippingSpeed shippingSpeed;
    private final String campaignCode;

    public CheckoutContext(
            CustomerProfile customer,
            ProductBundle bundle,
            CouponInfo coupon,
            PaymentAttempt payment,
            ShippingSpeed shippingSpeed,
            String campaignCode
    ) {
        this.customer = customer;
        this.bundle = bundle;
        this.coupon = coupon;
        this.payment = payment;
        this.shippingSpeed = shippingSpeed;
        this.campaignCode = campaignCode;
    }

    public CustomerProfile getCustomer() {
        return customer;
    }

    public ProductBundle getBundle() {
        return bundle;
    }

    public CouponInfo getCoupon() {
        return coupon;
    }

    public PaymentAttempt getPayment() {
        return payment;
    }

    public ShippingSpeed getShippingSpeed() {
        return shippingSpeed;
    }

    public String getCampaignCode() {
        return campaignCode;
    }

    public boolean hasCampaign(String expectedCode) {
        if (campaignCode == null || expectedCode == null) {
            return false;
        }
        if (campaignCode.isBlank()) {
            return false;
        }
        return campaignCode.equalsIgnoreCase(expectedCode);
    }

    public boolean isInternallyConsistent() {
        if (customer == null || bundle == null || payment == null) {
            return false;
        }
        if (payment.getAmount() != bundle.getTotalAmount()) {
            return false;
        }
        if (bundle.isDigital() && shippingSpeed == ShippingSpeed.SAME_DAY) {
            return false;
        }
        return true;
    }
}
