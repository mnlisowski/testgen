package sample.fit;

public class ShippingEngine {
    public int shippingFee(ProductBundle bundle, CustomerProfile customer, ShippingSpeed speed) {
        if (bundle == null || customer == null || speed == null) {
            return -1;
        }
        if (!bundle.requiresShipping()) {
            return 0;
        }

        int fee = 10;
        if (bundle.isFragile()) {
            fee += 15;
        }
        if (bundle.getItemCount() > 5) {
            fee += 8;
        }
        if (customer.getTier() == CustomerTier.VIP) {
            fee -= 10;
        }
        switch (speed) {
            case ECONOMY:
                fee -= 2;
                break;
            case EXPRESS:
                fee += 15;
                break;
            case SAME_DAY:
                fee += 30;
                break;
            default:
                fee += 0;
                break;
        }
        if (bundle.getRegion() == Region.REMOTE) {
            fee += 50;
        }
        return Math.max(fee, 0);
    }

    public boolean canShip(Region region, boolean fragile, int itemCount) {
        if (region == null) {
            return false;
        }
        if (itemCount <= 0) {
            return false;
        }
        if (region == Region.REMOTE && fragile) {
            return false;
        }
        return itemCount < 100;
    }
}
