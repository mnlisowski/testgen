package sample.fit;

public class DeviceProfile {
    private final String deviceId;
    private final DeviceTrust trust;
    private final boolean vpnDetected;
    private final int failedLogins;
    private final int accountAgeDays;
    private final String channel;

    public DeviceProfile(
            String deviceId,
            DeviceTrust trust,
            boolean vpnDetected,
            int failedLogins,
            int accountAgeDays,
            String channel
    ) {
        this.deviceId = deviceId;
        this.trust = trust;
        this.vpnDetected = vpnDetected;
        this.failedLogins = failedLogins;
        this.accountAgeDays = accountAgeDays;
        this.channel = channel;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public DeviceTrust getTrust() {
        return trust;
    }

    public boolean isVpnDetected() {
        return vpnDetected;
    }

    public int getFailedLogins() {
        return failedLogins;
    }

    public int getAccountAgeDays() {
        return accountAgeDays;
    }

    public String getChannel() {
        return channel;
    }

    public int riskPoints() {
        int points = 0;
        if (deviceId == null || deviceId.isBlank()) {
            points += 25;
        }
        if (failedLogins < 0) {
            points += 20;
        }
        if (failedLogins > 2) {
            points += 35;
        }
        if (accountAgeDays < 1) {
            points += 15;
        }
        if (vpnDetected) {
            points += 30;
        }
        switch (trust) {
            case TRUSTED:
                points -= 25;
                break;
            case KNOWN:
                points -= 10;
                break;
            case BLOCKED:
                points += 100;
                break;
            default:
                points += 10;
                break;
        }
        if (isChannel("partner") && trust == DeviceTrust.UNKNOWN) {
            points += 20;
        }
        return points;
    }

    public boolean supportsFastCheckout(PaymentMethod method, int amount) {
        if (method == null || trust == null) {
            return false;
        }
        if (trust == DeviceTrust.BLOCKED) {
            return false;
        }
        if (vpnDetected && amount > 100) {
            return false;
        }
        if (method == PaymentMethod.CRYPTO) {
            return false;
        }
        if (failedLogins > 0 && amount > 250) {
            return false;
        }
        return trust == DeviceTrust.TRUSTED || accountAgeDays > 30;
    }

    public boolean isChannel(String expectedChannel) {
        if (channel == null || expectedChannel == null) {
            return false;
        }
        return channel.equalsIgnoreCase(expectedChannel);
    }

    public String channelKind() {
        if (channel == null) {
            return "missing";
        }
        if (channel.isBlank()) {
            return "blank";
        }
        if (channel.startsWith("api")) {
            return "api";
        }
        if (channel.contains("mobile")) {
            return "mobile";
        }
        if (channel.equalsIgnoreCase("partner")) {
            return "partner";
        }
        return "web";
    }
}
