package sample.fit;

public class FraudReviewEngine {
    public String reviewDecision(FraudSignal signal, CheckoutContext context, int hourOfDay, boolean manualReviewAllowed) {
        if (signal == null) {
            return "reject";
        }
        if (hourOfDay < 0 || hourOfDay > 23) {
            return "invalid";
        }
        int score = reviewScore(signal, context, hourOfDay);
        if (score > 150) {
            return "reject";
        }
        if (score > 90 && manualReviewAllowed) {
            return "manual-review";
        }
        if (score > 90) {
            return "reject";
        }
        if (score > 50) {
            return "challenge";
        }
        return "accept";
    }

    public int reviewScore(FraudSignal signal, CheckoutContext context, int hourOfDay) {
        if (signal == null) {
            return 200;
        }
        int score = signal.combinedRisk();
        if (context == null || !context.isInternallyConsistent()) {
            score += 30;
        }
        if (hourOfDay < 6 || hourOfDay > 22) {
            score += 15;
        }
        if (signal.getPayment() != null && !signal.getPayment().isThreeDsPassed()) {
            score += 20;
        }
        if (signal.getCustomer() != null && signal.getCustomer().getTier() == CustomerTier.VIP) {
            score -= 25;
        }
        if (signal.hasSuspiciousShape()) {
            score += 35;
        }
        return score;
    }

    public boolean needsManualReview(CustomerProfile customer, PaymentAttempt payment, DeviceProfile device) {
        if (customer == null || payment == null || device == null) {
            return true;
        }
        if (customer.getTier() == CustomerTier.BLOCKED) {
            return true;
        }
        if (payment.paymentRisk() + device.riskPoints() > 80) {
            return true;
        }
        return !customer.isVerified() && payment.getAmount() > 200;
    }
}
