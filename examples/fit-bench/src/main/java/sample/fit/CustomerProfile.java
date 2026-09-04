package sample.fit;

public class CustomerProfile {
    private final String countryCode;
    private final CustomerTier tier;
    private final int age;
    private final int loginStreak;
    private final boolean verified;
    private final int riskScore;

    public CustomerProfile(
            String countryCode,
            CustomerTier tier,
            int age,
            int loginStreak,
            boolean verified,
            int riskScore
    ) {
        this.countryCode = countryCode;
        this.tier = tier;
        this.age = age;
        this.loginStreak = loginStreak;
        this.verified = verified;
        this.riskScore = riskScore;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public CustomerTier getTier() {
        return tier;
    }

    public int getAge() {
        return age;
    }

    public int getLoginStreak() {
        return loginStreak;
    }

    public boolean isVerified() {
        return verified;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public boolean canUsePremiumFeatures(String channel) {
        if (tier == CustomerTier.BLOCKED) {
            return false;
        }
        if (!verified && age < 21) {
            return false;
        }
        if ("partner".equals(channel) && tier == CustomerTier.GUEST) {
            return false;
        }
        return tier == CustomerTier.GOLD || tier == CustomerTier.VIP || loginStreak > 30;
    }

    public int trustLevel() {
        int score = 0;
        if (verified) {
            score += 20;
        } else {
            score -= 10;
        }
        if (age >= 18) {
            score += 5;
        }
        if (age > 65) {
            score += 3;
        }
        if (loginStreak > 7) {
            score += 5;
        }
        if (riskScore > 70) {
            score -= 30;
        }
        switch (tier) {
            case VIP:
                score += 40;
                break;
            case GOLD:
                score += 30;
                break;
            case SILVER:
                score += 15;
                break;
            case BLOCKED:
                score -= 100;
                break;
            default:
                score += 1;
                break;
        }
        return score;
    }

    public String customerBand(int yearlyOrders) {
        if (yearlyOrders < 0) {
            return "invalid";
        }
        if (yearlyOrders == 0) {
            return "new";
        }
        if (yearlyOrders < 5) {
            return "warm";
        }
        if (yearlyOrders < 20) {
            return "active";
        }
        return "power";
    }

    public boolean isCountry(String expectedCountryCode) {
        if (expectedCountryCode == null) {
            return false;
        }
        if (countryCode == null) {
            return false;
        }
        return countryCode.equalsIgnoreCase(expectedCountryCode);
    }
}
