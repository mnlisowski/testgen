package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.ConstructorModel;
import com.mlisows.testgen.domain.GenerationRequirement;
import com.mlisows.testgen.domain.MethodGenerationPlan;
import com.mlisows.testgen.domain.MethodModel;
import com.mlisows.testgen.domain.ParameterModel;
import org.junit.jupiter.api.Test;
import com.mlisows.testgen.domain.ProjectTypeIndex;
import com.mlisows.testgen.domain.TypeInfo;
import com.mlisows.testgen.domain.TypeKind;


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

    @Test
    void shouldRequireEnumArgumentWhenTypeIndexContainsEnum() {
        ClassStructure classStructure = new ClassStructure(
                "sample.DiscountCalculator",
                List.of(new ConstructorModel(true, List.of())),
                List.of(new MethodModel(
                        "calculate",
                        "double",
                        true,
                        List.of(new ParameterModel("customerType", "CustomerType"))
                ))
        );

        ProjectTypeIndex typeIndex = new ProjectTypeIndex(List.of(
                new TypeInfo("CustomerType", "sample.DiscountCalculator.CustomerType", TypeKind.ENUM)
        ));

        MethodGenerationPlanner planner = new MethodGenerationPlanner();

        List<MethodGenerationPlan> plans = planner.plan(classStructure, typeIndex);

        assertTrue(plans.get(0).getRequirements().contains(GenerationRequirement.ENUM_ARGUMENT));
    }

    @Test
    void shouldRequireInterfaceMockWhenTypeIndexContainsInterface() {
        ClassStructure classStructure = new ClassStructure(
                "sample.OrderService",
                List.of(new ConstructorModel(true, List.of())),
                List.of(new MethodModel(
                        "pay",
                        "boolean",
                        true,
                        List.of(new ParameterModel("gateway", "PaymentGateway"))
                ))
        );

        ProjectTypeIndex typeIndex = new ProjectTypeIndex(List.of(
                new TypeInfo("PaymentGateway", "sample.PaymentGateway", TypeKind.INTERFACE)
        ));

        MethodGenerationPlanner planner = new MethodGenerationPlanner();

        List<MethodGenerationPlan> plans = planner.plan(classStructure, typeIndex);

        assertTrue(plans.get(0).getRequirements().contains(GenerationRequirement.INTERFACE_MOCK));
    }


    @Test
    void shouldRequireObjectFixtureWhenTypeIndexContainsClass() {
        ClassStructure classStructure = new ClassStructure(
                "sample.OrderService",
                List.of(new ConstructorModel(true, List.of())),
                List.of(new MethodModel(
                        "process",
                        "void",
                        true,
                        List.of(new ParameterModel("order", "Order"))
                ))
        );

        ProjectTypeIndex typeIndex = new ProjectTypeIndex(List.of(
                new TypeInfo("Order", "sample.Order", TypeKind.CLASS)
        ));

        MethodGenerationPlanner planner = new MethodGenerationPlanner();

        List<MethodGenerationPlan> plans = planner.plan(classStructure, typeIndex);

        assertTrue(plans.get(0).getRequirements().contains(GenerationRequirement.OBJECT_FIXTURE));
    }

    @Test
    void shouldSkipNonPublicMethods() {
        ClassStructure classStructure = new ClassStructure(
                "sample.Calculator",
                List.of(new ConstructorModel(true, List.of())),
                List.of(
                        new MethodModel(
                                "calculate",
                                "int",
                                true,
                                List.of(new ParameterModel("amount", "int"))
                        ),
                        new MethodModel(
                                "helper",
                                "int",
                                false,
                                List.of(new ParameterModel("amount", "int"))
                        )
                )
        );

        MethodGenerationPlanner planner = new MethodGenerationPlanner();

        List<MethodGenerationPlan> plans = planner.plan(classStructure);

        assertEquals(1, plans.size());
        assertEquals("calculate", plans.get(0).getMethodName());
    }


}
