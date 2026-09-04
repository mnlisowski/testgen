package sample.fit;

public class PaymentAttempt {
    private final PaymentMethod method;
    private final int amount;
    private final boolean threeDsPassed;
    private final int failureCount;

    public PaymentAttempt(PaymentMethod method, int amount, boolean threeDsPassed, int failureCount) {
        this.method = method;
        this.amount = amount;
        this.threeDsPassed = threeDsPassed;
        this.failureCount = failureCount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public int getAmount() {
        return amount;
    }

    public boolean isThreeDsPassed() {
        return threeDsPassed;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public boolean isRetryAllowed() {
        if (failureCount < 0) {
            return false;
        }
        if (failureCount >= 3) {
            return false;
        }
        return method != PaymentMethod.CASH;
    }

    public int paymentRisk() {
        int risk = 0;
        if (amount > 1000) {
            risk += 30;
        }
        if (amount < 0) {
            risk += 100;
        }
        if (!threeDsPassed && method == PaymentMethod.CARD) {
            risk += 40;
        }
        if (failureCount > 0) {
            risk += failureCount * 15;
        }
        switch (method) {
            case CRYPTO:
                risk += 35;
                break;
            case WALLET:
                risk += 10;
                break;
            case TRANSFER:
                risk -= 5;
                break;
            case CASH:
                risk += 5;
                break;
            default:
                risk += 0;
                break;
        }
        return risk;
    }
}
