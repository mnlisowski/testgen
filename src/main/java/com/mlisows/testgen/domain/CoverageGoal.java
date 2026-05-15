package com.mlisows.testgen.domain;

import java.util.Objects;


public class CoverageGoal {
    private final BranchId branchId;
    private final String condition;

    public CoverageGoal(BranchId branchId, String condition){
        this.branchId = Objects.requireNonNull(branchId, "branchId must not be null");
        this.condition = condition == null ? "" : condition;
    }

    public BranchId getBranchId() {
        return branchId;

    }

    public String getCondition() {
        return condition;
    }

}
