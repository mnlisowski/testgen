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

    @Test
    void shouldCreateSeededTestCasesForMultipleSupportedMethods() {
        MethodModel calculateMethod = new MethodModel(
                "calculate",
                "int",
                true,
                List.of(new ParameterModel("amount", "int"))
        );

        MethodModel validateMethod = new MethodModel(
                "validate",
                "boolean",
                true,
                List.of(new ParameterModel("active", "boolean"))
        );

        ClassStructure classStructure = new ClassStructure(
                "sample.Calculator",
                List.of(new ConstructorModel(true, List.of())),
                List.of(calculateMethod, validateMethod)
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
                        "validate",
                        List.of(
                                GenerationRequirement.NO_ARG_CONSTRUCTOR,
                                GenerationRequirement.PRIMITIVE_ARGUMENT
                        )
                )
        );

        GeneratedTestSuiteFactory factory = new GeneratedTestSuiteFactory(
                new GeneratedTestCaseFactory(new SimpleArgumentGenerator()),
                new GeneratableCoverageGoalSelector()
        );

        GeneratedTestSuite testSuite = factory.create(classStructure, methodPlans);

        assertEquals(7, testSuite.getTestCases().size());
        assertEquals("shouldCallCalculate1", testSuite.getTestCases().get(0).getTestName());
        assertEquals("shouldCallCalculate5", testSuite.getTestCases().get(4).getTestName());
        assertEquals("shouldCallValidate1", testSuite.getTestCases().get(5).getTestName());
        assertEquals("shouldCallValidate2", testSuite.getTestCases().get(6).getTestName());
    }

}
