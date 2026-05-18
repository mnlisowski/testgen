package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.GenerationRequirement;
import com.mlisows.testgen.domain.MethodGenerationPlan;
import com.mlisows.testgen.domain.WeightedCoverageGoal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class GeneratableCoverageGoalSelector {

    public List<WeightedCoverageGoal> selectGeneratableGoals(
            List<WeightedCoverageGoal> weightedGoals,
            List<MethodGenerationPlan> methodPlans
    ) {
        Objects.requireNonNull(weightedGoals, "weightedGoals must not be null");
        Objects.requireNonNull(methodPlans, "methodPlans must not be null");

        List<WeightedCoverageGoal> selectedGoals = new ArrayList<>();

        for (WeightedCoverageGoal weightedGoal : weightedGoals) {
            MethodGenerationPlan plan = findPlanForGoal(weightedGoal, methodPlans);

            if (plan != null && isSupportedByCurrentGenerator(plan)) {
                selectedGoals.add(weightedGoal);
            }
        }

        return selectedGoals;
    }

    private MethodGenerationPlan findPlanForGoal(WeightedCoverageGoal weightedGoal, List<MethodGenerationPlan> methodPlans) {
        String className = weightedGoal.getCoverageGoal().getBranchId().getClassName();
        String methodName = weightedGoal.getCoverageGoal().getBranchId().getMethodName();

        for (MethodGenerationPlan plan : methodPlans) {
            if (plan.getClassName().equals(className) && plan.getMethodName().equals(methodName)) {
                return plan;
            }
        }

        return null;
    }

    private boolean isSupportedByCurrentGenerator(MethodGenerationPlan plan) {
        for (GenerationRequirement requirement : plan.getRequirements()) {
            if (!isSupportedRequirement(requirement)) {
                return false;
            }
        }

        return true;
    }

    private boolean isSupportedRequirement(GenerationRequirement requirement) {
        return requirement == GenerationRequirement.NO_ARG_CONSTRUCTOR
                || requirement == GenerationRequirement.PRIMITIVE_ARGUMENT
                || requirement == GenerationRequirement.STRING_ARGUMENT;
    }
}
