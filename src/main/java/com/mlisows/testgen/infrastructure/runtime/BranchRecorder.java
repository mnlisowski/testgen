package com.mlisows.testgen.infrastructure.runtime;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class BranchRecorder {
    private static final Map<String, Integer> branchHits = new LinkedHashMap<>();

    private BranchRecorder() {
    }

    public static synchronized void hit(String branchId) {
        Objects.requireNonNull(branchId, "branchId must not be null");

        branchHits.merge(branchId, 1, Integer::sum);
    }

    public static synchronized Map<String, Integer> snapshot() {
        return Map.copyOf(branchHits);
    }

    public static synchronized void reset() {
        branchHits.clear();
    }
}
