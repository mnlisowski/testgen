package com.mlisows.testgen.domain;

import java.util.Objects;

public final class WeightedCoverageGoal {
    private final CoverageGoal coverageGoal;
    private final BranchWeight weight;

    public WeightedCoverageGoal(CoverageGoal coverageGoal, BranchWeight weight) {
        this.coverageGoal = Objects.requireNonNull(coverageGoal, "coverageGoal must not be null");
        this.weight = Objects.requireNonNull(weight, "weight must not be null");
    }

    public CoverageGoal getCoverageGoal() {
        return coverageGoal;
    }

    public BranchWeight getWeight() {
        return weight;
    }
}
