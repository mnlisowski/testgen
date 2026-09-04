package sample.fit;

public class CheckoutDecisionEngine {
    private final PricingEngine pricingEngine;
    private final ShippingEngine shippingEngine;
    private final RiskEngine riskEngine;
    private final InventoryEngine inventoryEngine;
    private final FraudReviewEngine fraudReviewEngine;
    private final DeliveryPromiseEngine deliveryPromiseEngine;
    private final TaxEngine taxEngine;
    private final PromotionEngine promotionEngine;
    private final AccountPolicyEngine accountPolicyEngine;

    public CheckoutDecisionEngine() {
        this.pricingEngine = new PricingEngine();
        this.shippingEngine = new ShippingEngine();
        this.riskEngine = new RiskEngine();
        this.inventoryEngine = new InventoryEngine();
        this.fraudReviewEngine = new FraudReviewEngine();
        this.deliveryPromiseEngine = new DeliveryPromiseEngine();
        this.taxEngine = new TaxEngine();
        this.promotionEngine = new PromotionEngine();
        this.accountPolicyEngine = new AccountPolicyEngine();
    }

    public String decide(CheckoutContext context, boolean manualReviewAllowed) {
        if (context == null) {
            return "reject";
        }
        if (!context.isInternallyConsistent()) {
            return "invalid";
        }

        int risk = riskEngine.riskScore(context);
        if (risk > 85) {
            return "reject";
        }
        if (risk > 60 && manualReviewAllowed) {
            return "review";
        }
        if (risk > 60) {
            return "reject";
        }

        int price = pricingEngine.finalPrice(context);
        if (price < 0) {
            return "invalid";
        }

        int shipping = shippingEngine.shippingFee(
                context.getBundle(),
                context.getCustomer(),
                context.getShippingSpeed()
        );
        if (shipping < 0) {
            return "invalid";
        }
        if (price + shipping == 0) {
            return "free";
        }
        return "accept";
    }

    public String decideAdvanced(
            CheckoutContext context,
            InventoryItem item,
            AddressProfile address,
            DeviceProfile device,
            CustomerAccount account,
            boolean manualReviewAllowed,
            int hourOfDay,
            boolean holiday
    ) {
        if (context == null) {
            return "reject-missing-context";
        }
        if (!context.isInternallyConsistent()) {
            return "reject-invalid-context";
        }
        if (accountPolicyEngine.isBlocked(account, context.getPayment())) {
            return "reject-account";
        }

        String accountDecision = accountPolicyEngine.accountDecision(account, context, device);
        if (accountDecision.startsWith("reject")) {
            return accountDecision;
        }

        String inventoryDecision = inventoryEngine.reserveDecision(
                item,
                context.getBundle(),
                context.getBundle().getItemCount(),
                manualReviewAllowed
        );
        if (inventoryDecision.equals("missing") || inventoryDecision.equals("invalid")) {
            return "reject-inventory";
        }
        if (inventoryDecision.equals("reject") && !manualReviewAllowed) {
            return "reject-stock";
        }

        FraudSignal signal = new FraudSignal(
                context.getCustomer(),
                context.getPayment(),
                device,
                address,
                context.getCustomer().getCountryCode(),
                device != null && device.isVpnDetected()
        );
        String fraudDecision = fraudReviewEngine.reviewDecision(signal, context, hourOfDay, manualReviewAllowed);
        if (fraudDecision.equals("reject")) {
            return "reject-fraud";
        }
        if (fraudDecision.equals("manual-review")) {
            return "manual-review";
        }
        if (fraudDecision.equals("challenge") && !manualReviewAllowed) {
            return "reject-challenge";
        }

        String deliveryPromise = deliveryPromiseEngine.promise(
                address,
                context.getBundle(),
                context.getShippingSpeed(),
                holiday,
                hourOfDay
        );
        if (deliveryPromise.equals("unavailable") || deliveryPromise.equals("unknown")) {
            return "reject-delivery";
        }

        int price = pricingEngine.finalPrice(context);
        int tax = taxEngine.calculateTax(context.getCustomer(), context.getBundle(), "PL-123456", false);
        int shipping = shippingEngine.shippingFee(context.getBundle(), context.getCustomer(), context.getShippingSpeed());
        int discount = promotionEngine.bonusDiscount(
                context.getCustomer(),
                context.getBundle(),
                context.getCoupon(),
                context.getCampaignCode(),
                hourOfDay
        );
        int finalAmount = price + tax + shipping - discount;

        if (finalAmount < 0) {
            return "free";
        }
        if (finalAmount > 2000) {
            return manualReviewAllowed ? "manual-review" : "reject-amount";
        }
        if (inventoryDecision.equals("slow") || deliveryPromise.contains("delayed")) {
            return "accept-delayed";
        }
        if (accountDecision.equals("priority") && fraudDecision.equals("accept")) {
            return "accept-priority";
        }
        return fraudDecision.equals("challenge") ? "accept-with-challenge" : "accept";
    }

    public boolean fastPath(CustomerTier tier, PaymentMethod method, int amount, boolean trustedDevice) {
        if (!trustedDevice) {
            return false;
        }
        if (amount > 300) {
            return false;
        }
        if (method == PaymentMethod.CRYPTO) {
            return false;
        }
        return tier == CustomerTier.GOLD || tier == CustomerTier.VIP;
    }
}
