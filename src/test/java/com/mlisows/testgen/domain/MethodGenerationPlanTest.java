package com.mlisows.testgen.domain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MethodGenerationPlanTest {

    @Test
    void shouldCopyRequirements() {
        List<GenerationRequirement> requirements = new ArrayList<>();
        requirements.add(GenerationRequirement.NO_ARG_CONSTRUCTOR);

        MethodGenerationPlan plan = new MethodGenerationPlan(
                "sample.Calculator",
                "calculate",
                requirements
        );

        assertEquals(1, plan.getRequirements().size());
        assertEquals(GenerationRequirement.NO_ARG_CONSTRUCTOR, plan.getRequirements().get(0));
    }
}
