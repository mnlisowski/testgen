package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.GeneratedArgument;
import com.mlisows.testgen.domain.GeneratedTestCase;
import org.junit.jupiter.api.Test;

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
}
