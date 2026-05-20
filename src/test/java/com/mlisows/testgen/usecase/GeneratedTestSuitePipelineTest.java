package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.ConstructorModel;
import com.mlisows.testgen.domain.GeneratedTestSuite;
import com.mlisows.testgen.domain.MethodGenerationPlan;
import com.mlisows.testgen.domain.MethodModel;
import com.mlisows.testgen.domain.ParameterModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratedTestSuitePipelineTest {

    @Test
    void shouldGenerateSmokeTestSuiteForSupportedMethods() {
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

        MethodGenerationPlanner planner = new MethodGenerationPlanner();
        List<MethodGenerationPlan> methodPlans = planner.plan(classStructure);

        GeneratedTestSuiteFactory suiteFactory = new GeneratedTestSuiteFactory(
                new GeneratedTestCaseFactory(new SimpleArgumentGenerator()),
                new GeneratableCoverageGoalSelector()
        );

        JUnitTestWriter writer = new JUnitTestWriter();

        GeneratedTestSuite testSuite = suiteFactory.create(classStructure, methodPlans);
        String code = writer.write(testSuite);

        assertTrue(code.contains("class CalculatorTest"));
        assertTrue(code.contains("void shouldCallCalculate()"));
        assertTrue(code.contains("calculator.calculate(0);"));
    }
}
