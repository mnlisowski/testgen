package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.ConstructorModel;
import com.mlisows.testgen.domain.GeneratedSetupObject;
import com.mlisows.testgen.domain.GeneratedTestCase;
import com.mlisows.testgen.domain.MethodModel;
import com.mlisows.testgen.domain.ParameterModel;
import com.mlisows.testgen.domain.ProjectClassStructureIndex;
import com.mlisows.testgen.domain.ProjectTypeIndex;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeneratedTestCaseFactoryTest {

    @Test
    void shouldCreateGeneratedTestCaseForMethod() {
        MethodModel method = new MethodModel(
                "calculate",
                "int",
                true,
                List.of(new ParameterModel("amount", "int"))
        );

        ClassStructure classStructure = new ClassStructure(
                "sample.Calculator",
                List.of(new ConstructorModel(true, List.of())),
                List.of(method)
        );

        GeneratedTestCaseFactory factory = new GeneratedTestCaseFactory(new SimpleArgumentGenerator());

        GeneratedTestCase testCase = factory.create(classStructure, method);

        assertEquals("sample.Calculator", testCase.getClassName());
        assertEquals("calculate", testCase.getMethodName());
        assertEquals("int", testCase.getReturnType());
        assertEquals("shouldCallCalculate", testCase.getTestName());
        assertEquals("calculator", testCase.getTargetVariableName());
        assertEquals(1, testCase.getSetupObjects().size());

        GeneratedSetupObject setupObject = testCase.getSetupObjects().get(0);
        assertEquals("Calculator", setupObject.getType());
        assertEquals("calculator", setupObject.getVariableName());
        assertEquals(List.of(), setupObject.getArguments());

        assertEquals(1, testCase.getMethodArguments().size());
        assertEquals("int", testCase.getMethodArguments().get(0).getType());
        assertEquals("0", testCase.getMethodArguments().get(0).getValue());
    }

    @Test
    void shouldCreateSeededTestCasesForMethod() {
        MethodModel method = new MethodModel(
                "calculate",
                "int",
                true,
                List.of(new ParameterModel("amount", "int"))
        );

        ClassStructure classStructure = new ClassStructure(
                "sample.Calculator",
                List.of(new ConstructorModel(true, List.of())),
                List.of(method)
        );

        GeneratedTestCaseFactory factory = new GeneratedTestCaseFactory(new SimpleArgumentGenerator());

        List<GeneratedTestCase> testCases = factory.createAll(classStructure, method);

        assertEquals(5, testCases.size());
        assertEquals("shouldCallCalculate1", testCases.get(0).getTestName());
        assertEquals("-1", testCases.get(0).getMethodArguments().get(0).getValue());
        assertEquals("0", testCases.get(1).getMethodArguments().get(0).getValue());
        assertEquals("100", testCases.get(4).getMethodArguments().get(0).getValue());
    }

    @Test
    void shouldCreateTestCaseWithNestedObjectMethodArgument() {
        MethodModel calculateMethod = new MethodModel(
                "calculate",
                "int",
                true,
                List.of(new ParameterModel("order", "Order"))
        );

        ClassStructure discountService = new ClassStructure(
                "sample.DiscountService",
                List.of(new ConstructorModel(true, List.of())),
                List.of(calculateMethod)
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

        GeneratedTestCaseFactory factory = new GeneratedTestCaseFactory(new SimpleArgumentGenerator());

        List<GeneratedTestCase> testCases = factory.createAll(
                discountService,
                calculateMethod,
                new ProjectTypeIndex(List.of()),
                new ProjectClassStructureIndex(List.of(discountService, order, customer))
        );

        assertEquals(1, testCases.size());

        GeneratedTestCase testCase = testCases.get(0);

        assertEquals("sample.DiscountService", testCase.getClassName());
        assertEquals("calculate", testCase.getMethodName());
        assertEquals("shouldCallCalculate1", testCase.getTestName());
        assertEquals("discountService", testCase.getTargetVariableName());

        assertEquals(3, testCase.getSetupObjects().size());

        GeneratedSetupObject customerSetup = testCase.getSetupObjects().get(0);
        assertEquals("Customer", customerSetup.getType());
        assertEquals("customer", customerSetup.getVariableName());
        assertEquals("\"\"", customerSetup.getArguments().get(0).getValue());

        GeneratedSetupObject orderSetup = testCase.getSetupObjects().get(1);
        assertEquals("Order", orderSetup.getType());
        assertEquals("order", orderSetup.getVariableName());
        assertEquals("-1", orderSetup.getArguments().get(0).getValue());
        assertEquals("customer", orderSetup.getArguments().get(1).getValue());

        GeneratedSetupObject serviceSetup = testCase.getSetupObjects().get(2);
        assertEquals("DiscountService", serviceSetup.getType());
        assertEquals("discountService", serviceSetup.getVariableName());

        assertEquals(1, testCase.getMethodArguments().size());
        assertEquals("Order", testCase.getMethodArguments().get(0).getType());
        assertEquals("order", testCase.getMethodArguments().get(0).getValue());
    }



}
