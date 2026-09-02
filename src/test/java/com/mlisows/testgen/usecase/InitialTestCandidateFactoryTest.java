package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.ConstructorModel;
import com.mlisows.testgen.domain.GeneratedSetupObject;
import com.mlisows.testgen.domain.MethodModel;
import com.mlisows.testgen.domain.ParameterModel;
import com.mlisows.testgen.domain.ProjectClassStructureIndex;
import com.mlisows.testgen.domain.ProjectTypeIndex;
import com.mlisows.testgen.domain.TestCandidate;
import com.mlisows.testgen.domain.TypeInfo;
import com.mlisows.testgen.domain.TypeKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InitialTestCandidateFactoryTest {

    @Test
    void shouldCreateInitialCandidateWithPrimitiveArgument() {
        MethodModel method = new MethodModel(
                "calculate",
                "int",
                true,
                List.of(new ParameterModel("amount", "int"))
        );

        ClassStructure calculator = new ClassStructure(
                "sample.Calculator",
                List.of(new ConstructorModel(true, List.of())),
                List.of(method)
        );

        InitialTestCandidateFactory factory = new InitialTestCandidateFactory();

        Optional<TestCandidate> candidate = factory.create(
                calculator,
                method,
                new ProjectTypeIndex(List.of()),
                new ProjectClassStructureIndex(List.of(calculator))
        );

        assertTrue(candidate.isPresent());
        assertEquals("sample.Calculator", candidate.get().getClassName());
        assertEquals("calculate", candidate.get().getMethodName());
        assertEquals("int", candidate.get().getReturnType());
        assertEquals("calculator", candidate.get().getTargetVariableName());

        assertEquals(1, candidate.get().getSetupObjects().size());
        assertEquals("Calculator", candidate.get().getSetupObjects().get(0).getType());
        assertEquals("calculator", candidate.get().getSetupObjects().get(0).getVariableName());

        assertEquals(1, candidate.get().getMethodArguments().size());
        assertEquals("int", candidate.get().getMethodArguments().get(0).getType());
        assertEquals("-1", candidate.get().getMethodArguments().get(0).getValue());
    }

    @Test
    void shouldCreateInitialCandidateWithEnumArgument() {
        MethodModel method = new MethodModel(
                "calculate",
                "int",
                true,
                List.of(new ParameterModel("type", "CustomerType"))
        );

        ClassStructure calculator = new ClassStructure(
                "sample.Calculator",
                List.of(new ConstructorModel(true, List.of())),
                List.of(method)
        );

        ProjectTypeIndex typeIndex = new ProjectTypeIndex(List.of(new TypeInfo(
                "CustomerType",
                "sample.CustomerType",
                TypeKind.ENUM,
                List.of("REGULAR", "GOLD")
        )));

        InitialTestCandidateFactory factory = new InitialTestCandidateFactory();

        Optional<TestCandidate> candidate = factory.create(
                calculator,
                method,
                typeIndex,
                new ProjectClassStructureIndex(List.of(calculator))
        );

        assertTrue(candidate.isPresent());
        assertEquals("CustomerType", candidate.get().getMethodArguments().get(0).getType());
        assertEquals("sample.CustomerType.REGULAR", candidate.get().getMethodArguments().get(0).getValue());
    }

    @Test
    void shouldCreateInitialCandidateWithNestedObjectArgument() {
        MethodModel method = new MethodModel(
                "calculate",
                "int",
                true,
                List.of(new ParameterModel("order", "Order"))
        );

        ClassStructure discountService = new ClassStructure(
                "sample.DiscountService",
                List.of(new ConstructorModel(true, List.of())),
                List.of(method)
        );

        ClassStructure customer = new ClassStructure(
                "sample.Customer",
                List.of(new ConstructorModel(
                        true,
                        List.of(new ParameterModel("type", "String"))
                )),
                List.of()
        );

        ClassStructure order = new ClassStructure(
                "sample.Order",
                List.of(new ConstructorModel(
                        true,
                        List.of(
                                new ParameterModel("total", "int"),
                                new ParameterModel("customer", "Customer")
                        )
                )),
                List.of()
        );

        InitialTestCandidateFactory factory = new InitialTestCandidateFactory();

        Optional<TestCandidate> candidate = factory.create(
                discountService,
                method,
                new ProjectTypeIndex(List.of()),
                new ProjectClassStructureIndex(List.of(discountService, order, customer))
        );

        assertTrue(candidate.isPresent());
        assertEquals("sample.DiscountService", candidate.get().getClassName());
        assertEquals("calculate", candidate.get().getMethodName());
        assertEquals("discountService", candidate.get().getTargetVariableName());

        assertEquals(3, candidate.get().getSetupObjects().size());

        GeneratedSetupObject customerSetup = candidate.get().getSetupObjects().get(0);
        assertEquals("Customer", customerSetup.getType());
        assertEquals("customer", customerSetup.getVariableName());
        assertEquals("\"\"", customerSetup.getArguments().get(0).getValue());

        GeneratedSetupObject orderSetup = candidate.get().getSetupObjects().get(1);
        assertEquals("Order", orderSetup.getType());
        assertEquals("order", orderSetup.getVariableName());
        assertEquals("-1", orderSetup.getArguments().get(0).getValue());
        assertEquals("customer", orderSetup.getArguments().get(1).getValue());

        GeneratedSetupObject serviceSetup = candidate.get().getSetupObjects().get(2);
        assertEquals("DiscountService", serviceSetup.getType());
        assertEquals("discountService", serviceSetup.getVariableName());

        assertEquals(1, candidate.get().getMethodArguments().size());
        assertEquals("Order", candidate.get().getMethodArguments().get(0).getType());
        assertEquals("order", candidate.get().getMethodArguments().get(0).getValue());
    }

    @Test
    void shouldReturnEmptyWhenArgumentCannotBeGenerated() {
        MethodModel method = new MethodModel(
                "calculate",
                "int",
                true,
                List.of(new ParameterModel("items", "List<String>"))
        );

        ClassStructure calculator = new ClassStructure(
                "sample.Calculator",
                List.of(new ConstructorModel(true, List.of())),
                List.of(method)
        );

        InitialTestCandidateFactory factory = new InitialTestCandidateFactory();

        Optional<TestCandidate> candidate = factory.create(
                calculator,
                method,
                new ProjectTypeIndex(List.of()),
                new ProjectClassStructureIndex(List.of(calculator))
        );

        assertTrue(candidate.isEmpty());
    }

    @Test
    void shouldCreateInitialCandidateWithTargetConstructorArgument() {
        MethodModel method = new MethodModel(
                "calculate",
                "int",
                true,
                List.of(new ParameterModel("amount", "int"))
        );

        ClassStructure calculator = new ClassStructure(
                "sample.Calculator",
                List.of(new ConstructorModel(true, List.of(new ParameterModel("rate", "int")))),
                List.of(method)
        );

        InitialTestCandidateFactory factory = new InitialTestCandidateFactory();

        Optional<TestCandidate> candidate = factory.create(
                calculator,
                method,
                new ProjectTypeIndex(List.of()),
                new ProjectClassStructureIndex(List.of(calculator))
        );

        assertTrue(candidate.isPresent());
        assertEquals("calculator", candidate.get().getTargetVariableName());
        assertEquals(1, candidate.get().getSetupObjects().size());
        assertEquals("Calculator", candidate.get().getSetupObjects().get(0).getType());
        assertEquals("calculator", candidate.get().getSetupObjects().get(0).getVariableName());
        assertEquals("-1", candidate.get().getSetupObjects().get(0).getArguments().get(0).getValue());
        assertEquals("-1", candidate.get().getMethodArguments().get(0).getValue());
    }
}
