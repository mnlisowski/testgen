package sample.fit;

public class CouponInfo {
    private final String code;
    private final int percent;
    private final int minAmount;
    private final boolean active;

    public CouponInfo(String code, int percent, int minAmount, boolean active) {
        this.code = code;
        this.percent = percent;
        this.minAmount = minAmount;
        this.active = active;
    }

    public String getCode() {
        return code;
    }

    public int getPercent() {
        return percent;
    }

    public int getMinAmount() {
        return minAmount;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isApplicable(ProductBundle bundle, CustomerProfile customer) {
        if (!active) {
            return false;
        }
        if (bundle == null || customer == null) {
            return false;
        }
        if (code == null || code.isBlank()) {
            return false;
        }
        if (percent <= 0 || percent > 90) {
            return false;
        }
        if (bundle.getTotalAmount() < minAmount) {
            return false;
        }
        if (code.startsWith("VIP") && customer.getTier() != CustomerTier.VIP) {
            return false;
        }
        return true;
    }

    public int discountFor(int amount) {
        if (!active) {
            return 0;
        }
        if (amount < minAmount) {
            return 0;
        }
        if (percent < 0) {
            return 0;
        }
        if (percent > 75) {
            return amount / 2;
        }
        return amount * percent / 100;
    }

    public String codeKind() {
        if (code == null) {
            return "missing";
        }
        if (code.isBlank()) {
            return "blank";
        }
        if (code.startsWith("VIP")) {
            return "vip";
        }
        if (code.contains("BLACK")) {
            return "seasonal";
        }
        if (code.endsWith("10")) {
            return "small";
        }
        return "standard";
    }
}
