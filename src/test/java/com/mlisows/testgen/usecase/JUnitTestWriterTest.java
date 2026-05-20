package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.GeneratedArgument;
import com.mlisows.testgen.domain.GeneratedTestCase;
import org.junit.jupiter.api.Test;
import com.mlisows.testgen.domain.GeneratedTestSuite;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JUnitTestWriterTest {

    @Test
    void shouldWriteSmokeTestCode() {
        GeneratedTestCase testCase = new GeneratedTestCase(
                "sample.Calculator",
                "calculate",
                "shouldCallCalculate",
                List.of(),
                List.of(new GeneratedArgument("int", "0"))
        );

        JUnitTestWriter writer = new JUnitTestWriter();

        String code = writer.write(testCase);

        assertTrue(code.contains("import org.junit.jupiter.api.Test;"));
        assertTrue(code.contains("class CalculatorTest"));
        assertTrue(code.contains("@Test"));
        assertTrue(code.contains("void shouldCallCalculate()"));
        assertTrue(code.contains("Calculator calculator = new Calculator();"));
        assertTrue(code.contains("calculator.calculate(0);"));
    }

    @Test
    void shouldWriteTestSuite() {
        GeneratedTestCase firstTestCase = new GeneratedTestCase(
                "sample.Calculator",
                "add",
                "shouldCallAdd",
                List.of(),
                List.of(new GeneratedArgument("int", "0"), new GeneratedArgument("int", "0"))
        );

        GeneratedTestCase secondTestCase = new GeneratedTestCase(
                "sample.Calculator",
                "subtract",
                "shouldCallSubtract",
                List.of(),
                List.of(new GeneratedArgument("int", "0"), new GeneratedArgument("int", "0"))
        );

        GeneratedTestSuite testSuite = new GeneratedTestSuite(
                "sample.Calculator",
                List.of(firstTestCase, secondTestCase)
        );

        JUnitTestWriter writer = new JUnitTestWriter();

        String code = writer.write(testSuite);

        assertTrue(code.contains("class CalculatorTest"));
        assertTrue(code.contains("void shouldCallAdd()"));
        assertTrue(code.contains("calculator.add(0, 0);"));
        assertTrue(code.contains("void shouldCallSubtract()"));
        assertTrue(code.contains("calculator.subtract(0, 0);"));
    }

}
