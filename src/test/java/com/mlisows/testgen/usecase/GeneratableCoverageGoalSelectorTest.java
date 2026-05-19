package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.BranchId;
import com.mlisows.testgen.domain.BranchKind;
import com.mlisows.testgen.domain.BranchType;
import com.mlisows.testgen.domain.BranchWeight;
import com.mlisows.testgen.domain.CoverageGoal;
import com.mlisows.testgen.domain.GenerationRequirement;
import com.mlisows.testgen.domain.MethodGenerationPlan;
import com.mlisows.testgen.domain.WeightedCoverageGoal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class GeneratableCoverageGoalSelectorTest {

    @Test
    void shouldSelectGoalWhenMethodRequirementsAreSupported() {
        WeightedCoverageGoal weightedGoal = weightedGoal("sample.Calculator", "calculate");
        MethodGenerationPlan plan = new MethodGenerationPlan(
                "sample.Calculator",
                "calculate",
                List.of(
                        GenerationRequirement.NO_ARG_CONSTRUCTOR,
                        GenerationRequirement.PRIMITIVE_ARGUMENT
                )
        );

        GeneratableCoverageGoalSelector selector = new GeneratableCoverageGoalSelector();

        List<WeightedCoverageGoal> selectedGoals = selector.selectGeneratableGoals(
                List.of(weightedGoal),
                List.of(plan)
        );

        assertEquals(1, selectedGoals.size());
        assertSame(weightedGoal, selectedGoals.get(0));
    }

    @Test
    void shouldSkipGoalWhenMethodRequiresObjectFixture() {
        WeightedCoverageGoal weightedGoal = weightedGoal("sample.OrderService", "process");
        MethodGenerationPlan plan = new MethodGenerationPlan(
                "sample.OrderService",
                "process",
                List.of(
                        GenerationRequirement.NO_ARG_CONSTRUCTOR,
                        GenerationRequirement.OBJECT_FIXTURE
                )
        );

        GeneratableCoverageGoalSelector selector = new GeneratableCoverageGoalSelector();

        List<WeightedCoverageGoal> selectedGoals = selector.selectGeneratableGoals(
                List.of(weightedGoal),
                List.of(plan)
        );

        assertEquals(0, selectedGoals.size());
    }

    private WeightedCoverageGoal weightedGoal(String className, String methodName) {
        CoverageGoal coverageGoal = new CoverageGoal(
                new BranchId(
                        className,
                        methodName,
                        6,
                        BranchKind.IF,
                        BranchType.TRUE,
                        ""
                ),
                "amount > 100"
        );

        return new WeightedCoverageGoal(coverageGoal, new BranchWeight(1.0));
    }

    @Test
    void shouldSkipGoalWhenNoMethodPlanExists() {
        WeightedCoverageGoal weightedGoal = weightedGoal("sample.Calculator", "calculate");

        GeneratableCoverageGoalSelector selector = new GeneratableCoverageGoalSelector();

        List<WeightedCoverageGoal> selectedGoals = selector.selectGeneratableGoals(
                List.of(weightedGoal),
                List.of()
        );

        assertEquals(0, selectedGoals.size());
    }

    @Test
    void shouldSelectOnlyGoalsForSupportedMethods() {
        WeightedCoverageGoal simpleGoal = weightedGoal("sample.Calculator", "calculate");
        WeightedCoverageGoal complexGoal = weightedGoal("sample.OrderService", "process");

        MethodGenerationPlan simplePlan = new MethodGenerationPlan(
                "sample.Calculator",
                "calculate",
                List.of(
                        GenerationRequirement.NO_ARG_CONSTRUCTOR,
                        GenerationRequirement.PRIMITIVE_ARGUMENT
                )
        );

        MethodGenerationPlan complexPlan = new MethodGenerationPlan(
                "sample.OrderService",
                "process",
                List.of(
                        GenerationRequirement.NO_ARG_CONSTRUCTOR,
                        GenerationRequirement.OBJECT_FIXTURE
                )
        );

        GeneratableCoverageGoalSelector selector = new GeneratableCoverageGoalSelector();

        List<WeightedCoverageGoal> selectedGoals = selector.selectGeneratableGoals(
                List.of(simpleGoal, complexGoal),
                List.of(simplePlan, complexPlan)
        );

        assertEquals(1, selectedGoals.size());
        assertSame(simpleGoal, selectedGoals.get(0));
    }


}
