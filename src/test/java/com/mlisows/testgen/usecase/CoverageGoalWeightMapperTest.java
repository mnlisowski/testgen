package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.BranchId;
import com.mlisows.testgen.domain.BranchKind;
import com.mlisows.testgen.domain.BranchType;
import com.mlisows.testgen.domain.CoverageGoal;
import com.mlisows.testgen.domain.WeightedCoverageGoal;
import org.junit.jupiter.api.Test;
import com.mlisows.testgen.domain.BranchWeight;
import com.mlisows.testgen.domain.BranchWeightProfile;
import java.util.Map;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CoverageGoalWeightMapperTest {

    @Test
    void shouldAssignDefaultWeightToCoverageGoals() {
        CoverageGoal coverageGoal = new CoverageGoal(
                new BranchId(
                        "sample.SimpleDiscountCalculator",
                        "calculate",
                        6,
                        BranchKind.IF,
                        BranchType.TRUE,
                        ""
                ),
                "amount > 100"
        );

        CoverageGoalWeightMapper mapper = new CoverageGoalWeightMapper();

        List<WeightedCoverageGoal> weightedGoals = mapper.assignDefaultWeights(List.of(coverageGoal));

        assertEquals(1, weightedGoals.size());
        assertSame(coverageGoal, weightedGoals.get(0).getCoverageGoal());
        assertEquals(1.0, weightedGoals.get(0).getWeight().getValue());
    }

    @Test
    void shouldAssignWeightFromBranchWeightProfile() {
        BranchId branchId = new BranchId(
                "sample.SimpleDiscountCalculator",
                "calculate",
                6,
                BranchKind.IF,
                BranchType.TRUE,
                ""
        );

        CoverageGoal coverageGoal = new CoverageGoal(branchId, "amount > 100");

        BranchWeightProfile profile = new BranchWeightProfile(Map.of(
                branchId.asString(), new BranchWeight(10.0)
        ));

        CoverageGoalWeightMapper mapper = new CoverageGoalWeightMapper();

        List<WeightedCoverageGoal> weightedGoals = mapper.assignWeights(List.of(coverageGoal), profile);

        assertEquals(1, weightedGoals.size());
        assertSame(coverageGoal, weightedGoals.get(0).getCoverageGoal());
        assertEquals(10.0, weightedGoals.get(0).getWeight().getValue());
    }

}
