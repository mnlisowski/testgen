    package sample.fit;

public class LoyaltyEngine {
    public int rewardPoints(CustomerProfile customer, ProductBundle bundle, CouponInfo coupon, int dayOfMonth) {
        if (customer == null || bundle == null) {
            return 0;
        }
        if (dayOfMonth < 1 || dayOfMonth > 31) {
            return 0;
        }

        int points = bundle.getTotalAmount() / 10;
        switch (customer.getTier()) {
            case SILVER:
                points *= 2;
                break;
            case GOLD:
                points *= 3;
                break;
            case VIP:
                points *= 5;
                break;
            case BLOCKED:
                return 0;
            default:
                points += 1;
                break;
        }
        if (coupon != null && coupon.isActive()) {
            points -= 5;
        }
        if (dayOfMonth == 1) {
            points += 10;
        }
        if (points < 0) {
            return 0;
        }
        return points;
    }

    public String badge(CustomerProfile customer, int points) {
        if (customer == null) {
            return "none";
        }
        if (points < 0) {
            return "invalid";
        }
        if (customer.getTier() == CustomerTier.VIP || points > 1000) {
            return "diamond";
        }
        if (points > 500) {
            return "gold";
        }
        if (points > 100) {
            return "silver";
        }
        return "basic";
    }
}
