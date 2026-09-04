package sample.fit;

public class DeliveryPromiseEngine {
    public String promise(AddressProfile address, ProductBundle bundle, ShippingSpeed speed, boolean holiday, int hourOfDay) {
        if (address == null || bundle == null || speed == null) {
            return "unknown";
        }
        if (hourOfDay < 0 || hourOfDay > 23) {
            return "invalid";
        }
        if (!bundle.requiresShipping()) {
            return "instant";
        }
        if (!address.isDeliverable(speed, holiday)) {
            return "unavailable";
        }
        if (holiday) {
            return speed == ShippingSpeed.ECONOMY ? "delayed" : "priority-delayed";
        }
        if (speed == ShippingSpeed.SAME_DAY && hourOfDay < 12) {
            return "today";
        }
        if (speed == ShippingSpeed.SAME_DAY) {
            return "tomorrow";
        }
        if (address.getRegion() == Region.REMOTE) {
            return "remote-window";
        }
        return speed == ShippingSpeed.EXPRESS ? "next-day" : "standard";
    }

    public int deliveryAdjustment(AddressProfile address, ShippingSpeed speed, boolean holiday) {
        if (address == null || speed == null) {
            return 0;
        }
        int adjustment = 0;
        if (holiday) {
            adjustment += 10;
        }
        if (address.isBusinessAddress()) {
            adjustment -= 3;
        }
        if (address.getDistanceKm() > 500) {
            adjustment += 20;
        }
        switch (speed) {
            case ECONOMY:
                adjustment -= 2;
                break;
            case EXPRESS:
                adjustment += 12;
                break;
            case SAME_DAY:
                adjustment += 30;
                break;
            default:
                adjustment += 0;
                break;
        }
        return adjustment;
    }
}
