package sample.fit;

public class ProductBundle {
    private final int itemCount;
    private final int totalAmount;
    private final boolean digital;
    private final boolean fragile;
    private final Region region;

    public ProductBundle(int itemCount, int totalAmount, boolean digital, boolean fragile, Region region) {
        this.itemCount = itemCount;
        this.totalAmount = totalAmount;
        this.digital = digital;
        this.fragile = fragile;
        this.region = region;
    }

    public int getItemCount() {
        return itemCount;
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public boolean isDigital() {
        return digital;
    }

    public boolean isFragile() {
        return fragile;
    }

    public Region getRegion() {
        return region;
    }

    public boolean requiresShipping() {
        if (digital) {
            return false;
        }
        if (itemCount <= 0) {
            return false;
        }
        return region != Region.REMOTE;
    }

    public int packageComplexity() {
        int score = 0;
        if (itemCount < 0) {
            return -1;
        }
        if (itemCount == 0) {
            return 0;
        }
        for (int index = 0; index < itemCount; index++) {
            score += 2;
            if (fragile) {
                score += 3;
            }
            if (index > 10) {
                score++;
            }
        }
        if (totalAmount > 100) {
            score += 10;
        }
        if (totalAmount > 500) {
            score += 25;
        }
        return score;
    }

    public String amountBand() {
        if (totalAmount < 0) {
            return "invalid";
        }
        if (totalAmount == 0) {
            return "free";
        }
        if (totalAmount < 50) {
            return "small";
        }
        if (totalAmount < 200) {
            return "medium";
        }
        return "large";
    }
}
