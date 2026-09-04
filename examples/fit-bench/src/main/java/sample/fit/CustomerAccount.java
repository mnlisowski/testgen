package sample.fit;

public class CustomerAccount {
    private final String accountId;
    private final AccountStatus status;
    private final CustomerProfile profile;
    private final int chargebackCount;
    private final int openTicketCount;
    private final boolean marketingOptIn;

    public CustomerAccount(
            String accountId,
            AccountStatus status,
            CustomerProfile profile,
            int chargebackCount,
            int openTicketCount,
            boolean marketingOptIn
    ) {
        this.accountId = accountId;
        this.status = status;
        this.profile = profile;
        this.chargebackCount = chargebackCount;
        this.openTicketCount = openTicketCount;
        this.marketingOptIn = marketingOptIn;
    }

    public String getAccountId() {
        return accountId;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public CustomerProfile getProfile() {
        return profile;
    }

    public int getChargebackCount() {
        return chargebackCount;
    }

    public int getOpenTicketCount() {
        return openTicketCount;
    }

    public boolean isMarketingOptIn() {
        return marketingOptIn;
    }

    public boolean isUsable() {
        if (status == null || profile == null) {
            return false;
        }
        if (accountId == null || accountId.isBlank()) {
            return false;
        }
        if (chargebackCount < 0 || openTicketCount < 0) {
            return false;
        }
        if (status == AccountStatus.CLOSED || status == AccountStatus.SUSPENDED) {
            return false;
        }
        if (profile.getTier() == CustomerTier.BLOCKED) {
            return false;
        }
        return chargebackCount < 3;
    }

    public String accountBand() {
        if (status == null) {
            return "missing";
        }
        if (status == AccountStatus.NEW) {
            return marketingOptIn ? "new-marketing" : "new";
        }
        if (status == AccountStatus.FRAUD_REVIEW) {
            return "review";
        }
        if (status == AccountStatus.SUSPENDED || status == AccountStatus.CLOSED) {
            return "blocked";
        }
        if (profile != null && profile.getTier() == CustomerTier.VIP) {
            return "vip";
        }
        return openTicketCount > 0 ? "support" : "active";
    }

    public int servicePriority() {
        int priority = 0;
        if (!isUsable()) {
            return -1;
        }
        if (marketingOptIn) {
            priority += 2;
        }
        if (openTicketCount > 3) {
            priority += 10;
        }
        if (chargebackCount > 0) {
            priority -= 5;
        }
        if (profile != null) {
            priority += profile.trustLevel() / 10;
        }
        return priority;
    }
}
