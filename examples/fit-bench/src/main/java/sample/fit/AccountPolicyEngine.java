package sample.fit;

public class AccountPolicyEngine {
    public boolean isBlocked(CustomerAccount account, PaymentAttempt payment) {
        if (account == null) {
            return true;
        }
        if (!account.isUsable()) {
            return true;
        }
        if (payment == null) {
            return true;
        }
        if (payment.getFailureCount() >= 3) {
            return true;
        }
        if (account.getChargebackCount() > 1 && payment.getAmount() > 100) {
            return true;
        }
        return account.getStatus() == AccountStatus.FRAUD_REVIEW && payment.paymentRisk() > 60;
    }

    public String accountDecision(CustomerAccount account, CheckoutContext context, DeviceProfile device) {
        if (account == null || context == null) {
            return "reject-missing";
        }
        if (isBlocked(account, context.getPayment())) {
            return "reject-account";
        }
        if (device == null) {
            return "review-device";
        }
        if (account.getProfile() != null && context.getCustomer() != null
                && account.getProfile().getTier() != context.getCustomer().getTier()) {
            return "review-profile";
        }
        if (device.riskPoints() > 80) {
            return "review-device";
        }
        if (account.servicePriority() > 20 && context.getCustomer().isVerified()) {
            return "priority";
        }
        return "allow";
    }

    public int accountPenalty(CustomerAccount account, int amount, boolean urgent) {
        if (account == null) {
            return 100;
        }
        if (amount < 0) {
            return 100;
        }
        int penalty = 0;
        if (!account.isUsable()) {
            penalty += 50;
        }
        if (urgent) {
            penalty += 5;
        }
        if (account.getChargebackCount() > 0) {
            penalty += account.getChargebackCount() * 20;
        }
        if (account.getOpenTicketCount() > 5) {
            penalty += 10;
        }
        switch (account.getStatus()) {
            case ACTIVE:
                penalty -= 5;
                break;
            case NEW:
                penalty += 10;
                break;
            case FRAUD_REVIEW:
                penalty += 40;
                break;
            default:
                penalty += 25;
                break;
        }
        return penalty;
    }
}
