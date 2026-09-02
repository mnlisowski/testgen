package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.GenerationRequirement;
import com.mlisows.testgen.domain.MethodGenerationPlan;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodGenerationPlanSupportTest {

    @Test
    void shouldSupportCurrentGeneratorRequirements() {
        MethodGenerationPlan plan = new MethodGenerationPlan(
                "sample.OrderService",
                "process",
                List.of(
                        GenerationRequirement.NO_ARG_CONSTRUCTOR,
                        GenerationRequirement.PRIMITIVE_ARGUMENT,
                        GenerationRequirement.STRING_ARGUMENT,
                        GenerationRequirement.ENUM_ARGUMENT,
                        GenerationRequirement.OBJECT_FIXTURE
                )
        );

        MethodGenerationPlanSupport support = new MethodGenerationPlanSupport();

        assertTrue(support.isSupported(plan));
    }

    @Test
    void shouldRejectUnsupportedRequirements() {
        MethodGenerationPlan plan = new MethodGenerationPlan(
                "sample.OrderService",
                "process",
                List.of(
                        GenerationRequirement.NO_ARG_CONSTRUCTOR,
                        GenerationRequirement.COLLECTION_FIXTURE
                )
        );

        MethodGenerationPlanSupport support = new MethodGenerationPlanSupport();

        assertFalse(support.isSupported(plan));
    }
}
