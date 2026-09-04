package sample.fit;

public class AddressProfile {
    private final String countryCode;
    private final String postalCode;
    private final Region region;
    private final boolean businessAddress;
    private final int distanceKm;
    private final DeliveryWindow deliveryWindow;

    public AddressProfile(
            String countryCode,
            String postalCode,
            Region region,
            boolean businessAddress,
            int distanceKm,
            DeliveryWindow deliveryWindow
    ) {
        this.countryCode = countryCode;
        this.postalCode = postalCode;
        this.region = region;
        this.businessAddress = businessAddress;
        this.distanceKm = distanceKm;
        this.deliveryWindow = deliveryWindow;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public Region getRegion() {
        return region;
    }

    public boolean isBusinessAddress() {
        return businessAddress;
    }

    public int getDistanceKm() {
        return distanceKm;
    }

    public DeliveryWindow getDeliveryWindow() {
        return deliveryWindow;
    }

    public boolean isDeliverable(ShippingSpeed speed, boolean holiday) {
        if (countryCode == null || countryCode.isBlank()) {
            return false;
        }
        if (region == null || speed == null || deliveryWindow == null) {
            return false;
        }
        if (distanceKm < 0) {
            return false;
        }
        if (countryCode.equalsIgnoreCase("XX")) {
            return false;
        }
        if (holiday && speed == ShippingSpeed.SAME_DAY) {
            return false;
        }
        if (region == Region.REMOTE && speed == ShippingSpeed.SAME_DAY) {
            return false;
        }
        if (businessAddress && deliveryWindow == DeliveryWindow.NIGHT) {
            return false;
        }
        return distanceKm <= maxDistance(speed);
    }

    public String deliveryZone() {
        if (postalCode == null) {
            return "missing";
        }
        if (postalCode.isBlank()) {
            return "blank";
        }
        if (postalCode.startsWith("00")) {
            return "capital";
        }
        if (postalCode.startsWith("9")) {
            return "remote";
        }
        if (region == Region.EU) {
            return "eu";
        }
        if (region == Region.US) {
            return "us";
        }
        return "standard";
    }

    public int addressRiskPoints() {
        int points = 0;
        if (countryCode == null || countryCode.isBlank()) {
            points += 40;
        }
        if (postalCode == null || postalCode.length() < 3) {
            points += 15;
        }
        if (distanceKm > 500) {
            points += 20;
        }
        if (distanceKm > 2000) {
            points += 40;
        }
        if (region == Region.REMOTE) {
            points += 30;
        }
        if (businessAddress) {
            points -= 5;
        }
        return points;
    }

    private int maxDistance(ShippingSpeed speed) {
        switch (speed) {
            case ECONOMY:
                return 3000;
            case EXPRESS:
                return 1200;
            case SAME_DAY:
                return 80;
            default:
                return 500;
        }
    }
}
