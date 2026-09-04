package sample.fit;

public final class DemoMain {
    private DemoMain() {
    }

    public static void main(String[] args) {
        CheckoutDecisionEngine checkout = new CheckoutDecisionEngine();

        CustomerProfile vipCustomer = new CustomerProfile("PL", CustomerTier.VIP, 34, 45, true, 8);
        ProductBundle regularBundle = new ProductBundle(3, 240, false, true, Region.EU);
        CouponInfo vipCoupon = new CouponInfo("VIP25", 25, 100, true);
        PaymentAttempt cardPayment = new PaymentAttempt(PaymentMethod.CARD, 240, true, 0);
        CheckoutContext regularContext = new CheckoutContext(
                vipCustomer,
                regularBundle,
                vipCoupon,
                cardPayment,
                ShippingSpeed.EXPRESS,
                "BLACK-FRIDAY"
        );

        checkout.decide(regularContext, true);
        checkout.fastPath(CustomerTier.VIP, PaymentMethod.CARD, 120, true);

        InventoryItem availableItem = new InventoryItem(
                "SKU-123",
                InventoryState.AVAILABLE,
                50,
                4,
                true,
                false,
                Region.EU
        );
        AddressProfile cityAddress = new AddressProfile("PL", "00-001", Region.EU, false, 18, DeliveryWindow.MORNING);
        DeviceProfile trustedDevice = new DeviceProfile("device-1", DeviceTrust.TRUSTED, false, 0, 400, "web");
        CustomerAccount activeAccount = new CustomerAccount(
                "ACC-1",
                AccountStatus.ACTIVE,
                vipCustomer,
                0,
                1,
                true
        );

        checkout.decideAdvanced(
                regularContext,
                availableItem,
                cityAddress,
                trustedDevice,
                activeAccount,
                true,
                10,
                false
        );

        CustomerProfile blockedCustomer = new CustomerProfile("XX", CustomerTier.BLOCKED, 17, 0, false, 95);
        ProductBundle invalidBundle = new ProductBundle(1, 900, false, false, Region.REMOTE);
        PaymentAttempt riskyPayment = new PaymentAttempt(PaymentMethod.CRYPTO, 900, false, 4);
        CheckoutContext riskyContext = new CheckoutContext(
                blockedCustomer,
                invalidBundle,
                new CouponInfo("BLACK90", 90, 50, true),
                riskyPayment,
                ShippingSpeed.SAME_DAY,
                "api-mobile"
        );

        checkout.decide(riskyContext, false);
        checkout.fastPath(CustomerTier.BLOCKED, PaymentMethod.CRYPTO, 900, false);
    }
}
