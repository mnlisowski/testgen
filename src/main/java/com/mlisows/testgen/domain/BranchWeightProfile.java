package com.mlisows.testgen.domain;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class BranchWeightProfile {
    private final Map<String, BranchWeight> weightsByBranchId;

    public BranchWeightProfile(Map<String, BranchWeight> weightsByBranchId) {
        this.weightsByBranchId = Map.copyOf(
                Objects.requireNonNull(weightsByBranchId, "weightsByBranchId must not be null")
        );
    }

    public Optional<BranchWeight> findWeight(BranchId branchId) {
        Objects.requireNonNull(branchId, "branchId must not be null");

        return Optional.ofNullable(weightsByBranchId.get(branchId.asString()));
    }
}
