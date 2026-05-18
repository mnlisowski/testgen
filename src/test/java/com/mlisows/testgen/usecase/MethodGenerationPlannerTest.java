package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.ConstructorModel;
import com.mlisows.testgen.domain.GenerationRequirement;
import com.mlisows.testgen.domain.MethodGenerationPlan;
import com.mlisows.testgen.domain.MethodModel;
import com.mlisows.testgen.domain.ParameterModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodGenerationPlannerTest {

    @Test
    void shouldPlanRequirementsForSimpleMethod() {
        ClassStructure classStructure = new ClassStructure(
                "sample.Calculator",
                List.of(new ConstructorModel(true, List.of())),
                List.of(new MethodModel(
                        "calculate",
                        "double",
                        true,
                        List.of(new ParameterModel("amount", "double"))
                ))
        );

        MethodGenerationPlanner planner = new MethodGenerationPlanner();

        List<MethodGenerationPlan> plans = planner.plan(classStructure);

        assertEquals(1, plans.size());
        assertEquals("calculate", plans.get(0).getMethodName());
        assertTrue(plans.get(0).getRequirements().contains(GenerationRequirement.NO_ARG_CONSTRUCTOR));
        assertTrue(plans.get(0).getRequirements().contains(GenerationRequirement.PRIMITIVE_ARGUMENT));
    }

    @Test
    void shouldRequireCollectionFixtureForListParameter() {
        ClassStructure classStructure = new ClassStructure(
                "sample.OrderService",
                List.of(new ConstructorModel(true, List.of())),
                List.of(new MethodModel(
                        "process",
                        "void",
                        true,
                        List.of(new ParameterModel("orders", "List<Order>"))
                ))
        );

        MethodGenerationPlanner planner = new MethodGenerationPlanner();

        List<MethodGenerationPlan> plans = planner.plan(classStructure);

        assertTrue(plans.get(0).getRequirements().contains(GenerationRequirement.COLLECTION_FIXTURE));
    }

    @Test
    void shouldRequireObjectFixtureWhenNoNoArgConstructorExists() {
        ClassStructure classStructure = new ClassStructure(
                "sample.OrderService",
                List.of(new ConstructorModel(true, List.of(new ParameterModel("repository", "OrderRepository")))),
                List.of(new MethodModel(
                        "calculate",
                        "double",
                        true,
                        List.of(new ParameterModel("amount", "double"))
                ))
        );

        MethodGenerationPlanner planner = new MethodGenerationPlanner();

        List<MethodGenerationPlan> plans = planner.plan(classStructure);

        assertTrue(plans.get(0).getRequirements().contains(GenerationRequirement.OBJECT_FIXTURE));
    }
}
