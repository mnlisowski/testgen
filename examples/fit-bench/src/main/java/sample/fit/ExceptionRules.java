package sample.fit;

public class ExceptionRules {
    public int requirePositiveAmount(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (amount == 0) {
            throw new IllegalStateException("amount must not be zero");
        }
        if (amount > 1000) {
            return 1000;
        }
        return amount;
    }

    public String requireKnownCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("code must not be null");
        }
        if (code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        if (code.startsWith("VIP")) {
            return "vip";
        }
        if (code.contains("-")) {
            return "compound";
        }
        if (code.endsWith("10")) {
            return "discount";
        }
        return "standard";
    }

    public int paymentLimit(PaymentMethod method, int amount, boolean verified) {
        if (method == null) {
            throw new IllegalArgumentException("method must not be null");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
        if (!verified && amount > 100) {
            throw new IllegalStateException("unverified payment is too large");
        }
        switch (method) {
            case CARD:
                return amount + 10;
            case TRANSFER:
                return amount + 5;
            case CRYPTO:
                throw new IllegalArgumentException("crypto is not accepted");
            default:
                return amount;
        }
    }

    public String validateCheckout(CheckoutContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (!context.isInternallyConsistent()) {
            throw new IllegalStateException("checkout context is inconsistent");
        }
        if (context.getCustomer().getTier() == CustomerTier.BLOCKED) {
            throw new IllegalArgumentException("blocked customer");
        }
        if (context.getPayment().paymentRisk() > 80) {
            return "review";
        }
        return "accepted";
    }
}
