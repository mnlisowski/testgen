package com.mlisows.testgen.domain;

public final class BranchWeight {
    private final double value;

    public BranchWeight(double value) {
        if (value < 0.0) {
            throw new IllegalArgumentException("weight must not be negative");
        }

        this.value = value;
    }

    public double getValue() {
        return value;
    }
}
