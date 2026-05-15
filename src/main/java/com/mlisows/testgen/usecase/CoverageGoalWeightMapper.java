package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.BranchWeight;
import com.mlisows.testgen.domain.CoverageGoal;
import com.mlisows.testgen.domain.WeightedCoverageGoal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CoverageGoalWeightMapper {

    public List<WeightedCoverageGoal> assignDefaultWeights(List<CoverageGoal> coverageGoals) {
        Objects.requireNonNull(coverageGoals, "coverageGoals must not be null");

        List<WeightedCoverageGoal> weightedGoals = new ArrayList<>();

        for (CoverageGoal coverageGoal : coverageGoals) {
            weightedGoals.add(new WeightedCoverageGoal(
                    coverageGoal,
                    new BranchWeight(1.0)
            ));
        }

        return weightedGoals;
    }
}
