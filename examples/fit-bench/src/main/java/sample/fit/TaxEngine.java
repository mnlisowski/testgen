package sample.fit;

public class TaxEngine {
    public int calculateTax(CustomerProfile customer, ProductBundle bundle, String vatNumber, boolean businessPurchase) {
        if (customer == null || bundle == null) {
            return -1;
        }
        if (bundle.getTotalAmount() < 0) {
            return -2;
        }
        int rate = baseRate(customer.getCountryCode(), bundle.getRegion());
        if (businessPurchase && isValidVat(vatNumber)) {
            rate -= 5;
        }
        if (customer.getTier() == CustomerTier.VIP && bundle.isDigital()) {
            rate -= 2;
        }
        if (rate < 0) {
            rate = 0;
        }
        return bundle.getTotalAmount() * rate / 100;
    }

    public int baseRate(String countryCode, Region region) {
        if (countryCode == null || countryCode.isBlank()) {
            return 23;
        }
        if (region == Region.US) {
            return 8;
        }
        if (region == Region.REMOTE) {
            return 30;
        }
        switch (countryCode) {
            case "PL":
                return 23;
            case "DE":
                return 19;
            case "FR":
                return 20;
            case "GB":
                return 20;
            default:
                return region == Region.EU ? 21 : 15;
        }
    }

    public boolean isValidVat(String vatNumber) {
        if (vatNumber == null) {
            return false;
        }
        if (vatNumber.length() < 4) {
            return false;
        }
        if (vatNumber.startsWith("PL") || vatNumber.startsWith("DE")) {
            return vatNumber.length() >= 6;
        }
        return vatNumber.contains("-") || vatNumber.length() > 8;
    }
}
