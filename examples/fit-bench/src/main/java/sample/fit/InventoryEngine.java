package sample.fit;

public class InventoryEngine {
    public String reserveDecision(InventoryItem item, ProductBundle bundle, int requestedQuantity, boolean allowBackorder) {
        if (item == null || bundle == null) {
            return "missing";
        }
        if (requestedQuantity <= 0) {
            return "invalid";
        }
        if (bundle.isDigital() && item.isDigital()) {
            return "digital";
        }
        if (!item.canReserve(requestedQuantity, allowBackorder)) {
            return allowBackorder ? "backorder" : "reject";
        }
        int risk = reservationRisk(item, bundle, requestedQuantity, allowBackorder);
        if (risk > 80) {
            return "review";
        }
        if (risk > 40) {
            return "slow";
        }
        return "reserve";
    }

    public int reservationRisk(InventoryItem item, ProductBundle bundle, int requestedQuantity, boolean allowBackorder) {
        if (item == null || bundle == null) {
            return 100;
        }
        int risk = item.handlingRisk(bundle);
        int checks = requestedQuantity;
        if (checks < 0) {
            return 100;
        }
        if (checks > 20) {
            checks = 20;
            risk += 15;
        }
        for (int index = 0; index < checks; index++) {
            if (item.isFragile()) {
                risk += 2;
            }
            if (index > item.availableUnits()) {
                risk += 5;
            }
        }
        if (allowBackorder && item.getState() == InventoryState.PREORDER) {
            risk -= 10;
        }
        if (item.getState() == InventoryState.DISCONTINUED) {
            risk += 80;
        }
        return risk;
    }

    public String warehousePath(Region customerRegion, Region warehouseRegion, boolean express) {
        if (customerRegion == null || warehouseRegion == null) {
            return "unknown";
        }
        if (customerRegion == warehouseRegion && express) {
            return "direct-express";
        }
        if (customerRegion == warehouseRegion) {
            return "direct";
        }
        if (customerRegion == Region.REMOTE || warehouseRegion == Region.REMOTE) {
            return express ? "remote-priority" : "remote";
        }
        return "cross-region";
    }
}
