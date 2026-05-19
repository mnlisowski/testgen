package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.ConstructorModel;
import com.mlisows.testgen.domain.GeneratedTestCase;
import com.mlisows.testgen.domain.MethodModel;
import com.mlisows.testgen.domain.ParameterModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratedSmokeTestPipelineTest {

    @Test
    void shouldGenerateSmokeTestCodeForSimpleMethod() {
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
        JUnitTestWriter writer = new JUnitTestWriter();

        GeneratedTestCase testCase = factory.create(classStructure, method);
        String code = writer.write(testCase);


        assertTrue(code.contains("class CalculatorTest"));
        assertTrue(code.contains("void shouldCallCalculate()"));
        assertTrue(code.contains("Calculator calculator = new Calculator();"));
        assertTrue(code.contains("calculator.calculate(0);"));
    }
}

