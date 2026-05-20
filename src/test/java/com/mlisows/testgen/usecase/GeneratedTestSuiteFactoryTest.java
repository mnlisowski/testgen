package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.ConstructorModel;
import com.mlisows.testgen.domain.GeneratedTestSuite;
import com.mlisows.testgen.domain.GenerationRequirement;
import com.mlisows.testgen.domain.MethodGenerationPlan;
import com.mlisows.testgen.domain.MethodModel;
import com.mlisows.testgen.domain.ParameterModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeneratedTestSuiteFactoryTest {

    @Test
    void shouldCreateTestSuiteOnlyForSupportedMethods() {
        MethodModel supportedMethod = new MethodModel(
                "calculate",
                "int",
                true,
                List.of(new ParameterModel("amount", "int"))
        );

        MethodModel unsupportedMethod = new MethodModel(
                "process",
                "void",
                true,
                List.of(new ParameterModel("order", "Order"))
        );

        ClassStructure classStructure = new ClassStructure(
                "sample.Calculator",
                List.of(new ConstructorModel(true, List.of())),
                List.of(supportedMethod, unsupportedMethod)
        );

        List<MethodGenerationPlan> methodPlans = List.of(
                new MethodGenerationPlan(
                        "sample.Calculator",
                        "calculate",
                        List.of(
                                GenerationRequirement.NO_ARG_CONSTRUCTOR,
                                GenerationRequirement.PRIMITIVE_ARGUMENT
                        )
                ),
                new MethodGenerationPlan(
                        "sample.Calculator",
                        "process",
                        List.of(
                                GenerationRequirement.NO_ARG_CONSTRUCTOR,
                                GenerationRequirement.OBJECT_FIXTURE
                        )
                )
        );

        GeneratedTestSuiteFactory factory = new GeneratedTestSuiteFactory(
                new GeneratedTestCaseFactory(new SimpleArgumentGenerator()),
                new GeneratableCoverageGoalSelector()
        );

        GeneratedTestSuite testSuite = factory.create(classStructure, methodPlans);

        assertEquals("sample.Calculator", testSuite.getClassName());
        assertEquals(5, testSuite.getTestCases().size());
        assertEquals("calculate", testSuite.getTestCases().get(0).getMethodName());
        assertEquals("-1", testSuite.getTestCases().get(0).getMethodArguments().get(0).getValue());
        assertEquals("100", testSuite.getTestCases().get(4).getMethodArguments().get(0).getValue());

    }
}
