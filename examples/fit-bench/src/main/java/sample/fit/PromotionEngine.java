package sample.fit;

public class PromotionEngine {
    public int bonusDiscount(CustomerProfile customer, ProductBundle bundle, CouponInfo coupon, String campaign, int hourOfDay) {
        if (customer == null || bundle == null) {
            return 0;
        }
        if (hourOfDay < 0 || hourOfDay > 23) {
            return 0;
        }
        int discount = 0;
        if (coupon != null && coupon.isApplicable(bundle, customer)) {
            discount += coupon.discountFor(bundle.getTotalAmount());
        }
        if (isCampaign(campaign, "BLACK")) {
            discount += 15;
        }
        if (isCampaign(campaign, "VIP") && customer.getTier() == CustomerTier.VIP) {
            discount += 25;
        }
        if (hourOfDay >= 20 && hourOfDay <= 23) {
            discount += 5;
        }
        if (bundle.getTotalAmount() > 500 && customer.isVerified()) {
            discount += 20;
        }
        return Math.min(discount, bundle.getTotalAmount());
    }

    public String promotionBand(CustomerProfile customer, CouponInfo coupon, String campaign) {
        if (customer == null) {
            return "none";
        }
        if (customer.getTier() == CustomerTier.BLOCKED) {
            return "blocked";
        }
        if (coupon != null && coupon.getPercent() > 50) {
            return "heavy-coupon";
        }
        if (isCampaign(campaign, "BLACK")) {
            return customer.getTier() == CustomerTier.VIP ? "vip-seasonal" : "seasonal";
        }
        if (customer.getLoginStreak() > 30) {
            return "loyal";
        }
        return "standard";
    }

    private boolean isCampaign(String campaign, String expected) {
        if (campaign == null || expected == null) {
            return false;
        }
        if (campaign.isBlank()) {
            return false;
        }
        return campaign.equalsIgnoreCase(expected) || campaign.contains(expected);
    }
}
