package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.GeneratedArgument;
import com.mlisows.testgen.domain.GeneratedSetupObject;
import com.mlisows.testgen.domain.GeneratedTestCase;
import com.mlisows.testgen.domain.GeneratedTestSuite;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JUnitTestWriterTest {

    @Test
    void shouldWriteSmokeTestCode() {
        GeneratedTestCase testCase = calculatorTestCase(
                "calculate",
                "void",
                "shouldCallCalculate",
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
        GeneratedTestCase firstTestCase = calculatorTestCase(
                "add",
                "void",
                "shouldCallAdd",
                List.of(new GeneratedArgument("int", "0"), new GeneratedArgument("int", "0"))
        );

        GeneratedTestCase secondTestCase = calculatorTestCase(
                "subtract",
                "void",
                "shouldCallSubtract",
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

    @Test
    void shouldWritePackageDeclarationForPackagedClass() {
        GeneratedTestCase testCase = calculatorTestCase(
                "calculate",
                "void",
                "shouldCallCalculate",
                List.of(new GeneratedArgument("int", "0"))
        );

        JUnitTestWriter writer = new JUnitTestWriter();

        String code = writer.write(testCase);

        assertTrue(code.startsWith("package sample;\n\n"));
        assertTrue(code.contains("import org.junit.jupiter.api.Test;"));
        assertTrue(code.contains("class CalculatorTest"));
    }

    @Test
    void shouldNotWritePackageDeclarationForDefaultPackageClass() {
        GeneratedTestCase testCase = new GeneratedTestCase(
                "Calculator",
                "calculate",
                "void",
                "shouldCallCalculate",
                List.of(new GeneratedSetupObject(
                        "Calculator",
                        "calculator",
                        List.of()
                )),
                "calculator",
                List.of(new GeneratedArgument("int", "0"))
        );

        JUnitTestWriter writer = new JUnitTestWriter();

        String code = writer.write(testCase);

        assertTrue(code.startsWith("import org.junit.jupiter.api.Test;"));
    }

    @Test
    void shouldAssignMethodResultWhenMethodReturnsValue() {
        GeneratedTestCase testCase = calculatorTestCase(
                "calculate",
                "int",
                "shouldCallCalculate",
                List.of(new GeneratedArgument("int", "100"))
        );

        JUnitTestWriter writer = new JUnitTestWriter();

        String code = writer.write(testCase);

        assertTrue(code.contains("int result = calculator.calculate(100);"));
    }

    @Test
    void shouldNotAssignMethodResultWhenMethodReturnsVoid() {
        GeneratedTestCase testCase = calculatorTestCase(
                "process",
                "void",
                "shouldCallProcess",
                List.of(new GeneratedArgument("int", "100"))
        );

        JUnitTestWriter writer = new JUnitTestWriter();

        String code = writer.write(testCase);

        assertTrue(code.contains("calculator.process(100);"));
        assertFalse(code.contains("void result ="));
    }

    @Test
    void shouldWriteSetupObjectsBeforeMethodCall() {
        GeneratedTestCase testCase = new GeneratedTestCase(
                "sample.DiscountService",
                "calculate",
                "int",
                "shouldCallCalculate",
                List.of(
                        new GeneratedSetupObject(
                                "Order",
                                "order",
                                List.of(new GeneratedArgument("int", "620"))
                        ),
                        new GeneratedSetupObject(
                                "DiscountService",
                                "discountService",
                                List.of()
                        )
                ),
                "discountService",
                List.of(new GeneratedArgument("Order", "order"))
        );

        JUnitTestWriter writer = new JUnitTestWriter();

        String code = writer.write(testCase);

        assertTrue(code.contains("Order order = new Order(620);"));
        assertTrue(code.contains("DiscountService discountService = new DiscountService();"));
        assertTrue(code.contains("int result = discountService.calculate(order);"));
    }

    private GeneratedTestCase calculatorTestCase(
            String methodName,
            String returnType,
            String testName,
            List<GeneratedArgument> methodArguments
    ) {
        return new GeneratedTestCase(
                "sample.Calculator",
                methodName,
                returnType,
                testName,
                List.of(new GeneratedSetupObject(
                        "Calculator",
                        "calculator",
                        List.of()
                )),
                "calculator",
                methodArguments
        );
    }

    @Test
    void shouldWriteNestedSetupObjectsBeforeMethodCall() {
        GeneratedTestCase testCase = new GeneratedTestCase(
                "sample.DiscountService",
                "calculate",
                "int",
                "shouldCallCalculate",
                List.of(
                        new GeneratedSetupObject(
                                "Customer",
                                "customer",
                                List.of(new GeneratedArgument("String", "\"\""))
                        ),
                        new GeneratedSetupObject(
                                "Order",
                                "order",
                                List.of(
                                        new GeneratedArgument("int", "-1"),
                                        new GeneratedArgument("Customer", "customer")
                                )
                        ),
                        new GeneratedSetupObject(
                                "DiscountService",
                                "discountService",
                                List.of()
                        )
                ),
                "discountService",
                List.of(new GeneratedArgument("Order", "order"))
        );

        JUnitTestWriter writer = new JUnitTestWriter();

        String code = writer.write(testCase);

        assertTrue(code.contains("Customer customer = new Customer(\"\");"));
        assertTrue(code.contains("Order order = new Order(-1, customer);"));
        assertTrue(code.contains("DiscountService discountService = new DiscountService();"));
        assertTrue(code.contains("int result = discountService.calculate(order);"));
    }

}
