package sample.fit;

public class RiskEngine {
    public int riskScore(CheckoutContext context) {
        if (context == null) {
            return 100;
        }
        if (context.getCustomer() == null || context.getPayment() == null || context.getBundle() == null) {
            return 90;
        }

        int risk = context.getCustomer().getRiskScore();
        risk += context.getPayment().paymentRisk();

        if (!context.getCustomer().isVerified()) {
            risk += 20;
        }
        if (context.getBundle().getTotalAmount() > 500) {
            risk += 15;
        }
        if (context.getBundle().getRegion() == Region.REMOTE) {
            risk += 25;
        }
        if (context.hasCampaign("BLACK") && context.getPayment().getFailureCount() > 0) {
            risk += 10;
        }
        if (risk < 0) {
            return 0;
        }
        if (risk > 100) {
            return 100;
        }
        return risk;
    }

    public boolean shouldBlock(PaymentAttempt payment, CustomerProfile customer, int manualScore) {
        if (payment == null || customer == null) {
            return true;
        }
        if (manualScore > 90) {
            return true;
        }
        if (customer.getTier() == CustomerTier.BLOCKED) {
            return true;
        }
        if (payment.getFailureCount() >= 3) {
            return true;
        }
        return payment.paymentRisk() + customer.getRiskScore() > 120;
    }
}
