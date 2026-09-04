package sample.fit;

public class InventoryItem {
    private final String sku;
    private final InventoryState state;
    private final int stock;
    private final int reserved;
    private final boolean fragile;
    private final boolean digital;
    private final Region warehouseRegion;

    public InventoryItem(
            String sku,
            InventoryState state,
            int stock,
            int reserved,
            boolean fragile,
            boolean digital,
            Region warehouseRegion
    ) {
        this.sku = sku;
        this.state = state;
        this.stock = stock;
        this.reserved = reserved;
        this.fragile = fragile;
        this.digital = digital;
        this.warehouseRegion = warehouseRegion;
    }

    public String getSku() {
        return sku;
    }

    public InventoryState getState() {
        return state;
    }

    public int getStock() {
        return stock;
    }

    public int getReserved() {
        return reserved;
    }

    public boolean isFragile() {
        return fragile;
    }

    public boolean isDigital() {
        return digital;
    }

    public Region getWarehouseRegion() {
        return warehouseRegion;
    }

    public int availableUnits() {
        if (stock < 0 || reserved < 0) {
            return -1;
        }
        if (reserved > stock) {
            return 0;
        }
        return stock - reserved;
    }

    public boolean canReserve(int quantity, boolean allowPreorder) {
        if (quantity <= 0) {
            return false;
        }
        if (state == null) {
            return false;
        }
        if (digital) {
            return state != InventoryState.DISCONTINUED;
        }
        switch (state) {
            case AVAILABLE:
                return availableUnits() >= quantity;
            case LOW_STOCK:
                return availableUnits() >= quantity && quantity <= 3;
            case PREORDER:
                return allowPreorder;
            default:
                return false;
        }
    }

    public String stockBand() {
        int available = availableUnits();
        if (available < 0) {
            return "invalid";
        }
        if (state == InventoryState.DISCONTINUED) {
            return "closed";
        }
        if (available == 0) {
            return "empty";
        }
        if (available < 5) {
            return "low";
        }
        if (available < 50) {
            return "normal";
        }
        return "bulk";
    }

    public int handlingRisk(ProductBundle bundle) {
        int risk = 0;
        if (bundle == null) {
            return 50;
        }
        if (sku == null || sku.isBlank()) {
            risk += 10;
        }
        if (fragile || bundle.isFragile()) {
            risk += 25;
        }
        if (digital != bundle.isDigital()) {
            risk += 15;
        }
        if (warehouseRegion != null && bundle.getRegion() != null && warehouseRegion != bundle.getRegion()) {
            risk += 20;
        }
        if (bundle.getItemCount() > availableUnits()) {
            risk += 30;
        }
        return risk;
    }
}
