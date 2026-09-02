package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.GenerationRequirement;
import com.mlisows.testgen.domain.MethodGenerationPlan;

import java.util.Objects;

public final class MethodGenerationPlanSupport {

    public boolean isSupported(MethodGenerationPlan plan) {
        Objects.requireNonNull(plan, "plan must not be null");

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
                || requirement == GenerationRequirement.STRING_ARGUMENT
                || requirement == GenerationRequirement.ENUM_ARGUMENT
                || requirement == GenerationRequirement.OBJECT_FIXTURE;
    }
}
