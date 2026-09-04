package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.generation.GeneratedArgument;
import com.mlisows.testgen.domain.generation.GeneratedSetupObject;
import com.mlisows.testgen.domain.generation.TestCandidate;
import com.mlisows.testgen.domain.execution.TestCandidateExecutionResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JUnitCandidateTestWriterTest {

    @Test
    void shouldWriteReturnedCandidateAsJUnitTest() {
        TestCandidate candidate = new TestCandidate(
                "sample.maven.DiscountService",
                "calculate",
                "int",
                List.of(
                        new GeneratedSetupObject(
                                "Customer",
                                "customer",
                                List.of(
                                        new GeneratedArgument("String", "\"PREMIUM\""),
                                        new GeneratedArgument("CustomerType", "CustomerType.VIP")
                                )
                        ),
                        new GeneratedSetupObject(
                                "Order",
                                "order",
                                List.of(
                                        new GeneratedArgument("int", "620"),
                                        new GeneratedArgument("Customer", "customer"),
                                        new GeneratedArgument("String", "\"PAID\"")
                                )
                        ),
                        new GeneratedSetupObject(
                                "DiscountService",
                                "discountService",
                                List.of(new GeneratedArgument("int", "500"))
                        )
                ),
                "discountService",
                List.of(
                        new GeneratedArgument("Order", "order"),
                        new GeneratedArgument("String", "\"BLACK_FRIDAY\"")
                )
        );

        TestCandidateExecutionResult result = TestCandidateExecutionResult.returned(
                candidate,
                List.of(),
                "63"
        );

        String code = new JUnitCandidateTestWriter().write(
                "sample.maven.DiscountService",
                List.of(result)
        );

        assertTrue(code.startsWith("package sample.maven;"));
        assertTrue(code.contains("import org.junit.jupiter.api.Test;"));
        assertTrue(code.contains("import static org.junit.jupiter.api.Assertions.assertEquals;"));
        assertTrue(code.contains("class DiscountServiceTest"));
        assertTrue(code.contains("void shouldCallCalculate1() throws Exception"));
        assertTrue(code.contains("Customer customer = new Customer(\"PREMIUM\", CustomerType.VIP);"));
        assertTrue(code.contains("Order order = new Order(620, customer, \"PAID\");"));
        assertTrue(code.contains("DiscountService discountService = new DiscountService(500);"));
        assertTrue(code.contains("int result = discountService.calculate(order, \"BLACK_FRIDAY\");"));
        assertTrue(code.contains("assertEquals(63, result);"));
    }

    @Test
    void shouldWriteExceptionCandidateAsAssertThrows() {
        TestCandidate candidate = new TestCandidate(
                "sample.Calculator",
                "calculate",
                "int",
                List.of(new GeneratedSetupObject("Calculator", "calculator", List.of())),
                "calculator",
                List.of(new GeneratedArgument("int", "-1"))
        );

        TestCandidateExecutionResult result = TestCandidateExecutionResult.threwException(
                candidate,
                List.of(),
                "java.lang.IllegalArgumentException",
                "amount must be positive"
        );

        String code = new JUnitCandidateTestWriter().write("sample.Calculator", List.of(result));

        assertTrue(code.contains("class CalculatorTest"));
        assertTrue(code.contains("import static org.junit.jupiter.api.Assertions.assertThrows;"));
        assertTrue(code.contains("void shouldCallCalculate1() throws Exception"));
        assertTrue(code.contains("Calculator calculator = new Calculator();"));
        assertTrue(code.contains("assertThrows(java.lang.IllegalArgumentException.class, () -> calculator.calculate(-1));"));
    }

    @Test
    void shouldEscapeStringReturnValueInEqualsAssertion() {
        TestCandidate candidate = new TestCandidate(
                "sample.Greeter",
                "greet",
                "String",
                List.of(new GeneratedSetupObject("Greeter", "greeter", List.of())),
                "greeter",
                List.of(new GeneratedArgument("String", "\"Marcin\""))
        );

        TestCandidateExecutionResult result = TestCandidateExecutionResult.returned(
                candidate,
                List.of(),
                "Hello \"Marcin\""
        );

        String code = new JUnitCandidateTestWriter().write("sample.Greeter", List.of(result));

        assertTrue(code.contains("String result = greeter.greet(\"Marcin\");"));
        assertTrue(code.contains("assertEquals(\"Hello \\\"Marcin\\\"\", result);"));
    }


    @Test
    void shouldCastNullArgumentToDeclaredType() {
        TestCandidate candidate = new TestCandidate(
                "sample.Codec",
                "decode",
                "String",
                List.of(new GeneratedSetupObject("Codec", "codec", List.of())),
                "codec",
                List.of(new GeneratedArgument("String", "null"))
        );

        TestCandidateExecutionResult result = TestCandidateExecutionResult.returned(
                candidate,
                List.of(),
                "decoded"
        );

        String code = new JUnitCandidateTestWriter().write("sample.Codec", List.of(result));

        assertTrue(code.contains("String result = codec.decode((String) null);"));
    }

    @Test
    void shouldAssertNullWhenMethodReturnsNull() {
        TestCandidate candidate = new TestCandidate(
                "sample.Greeter",
                "greet",
                "String",
                List.of(new GeneratedSetupObject("Greeter", "greeter", List.of())),
                "greeter",
                List.of(new GeneratedArgument("String", "null"))
        );

        TestCandidateExecutionResult result = TestCandidateExecutionResult.returned(
                candidate,
                List.of(),
                null
        );

        String code = new JUnitCandidateTestWriter().write("sample.Greeter", List.of(result));

        assertTrue(code.contains("import static org.junit.jupiter.api.Assertions.assertNull;"));
        assertTrue(code.contains("String result = greeter.greet((String) null);"));
        assertTrue(code.contains("assertNull(result);"));
    }

    @Test
    void shouldAssertNotNullForObjectReturnValue() {
        TestCandidate candidate = new TestCandidate(
                "sample.Base32",
                "builder",
                "Builder",
                List.of(new GeneratedSetupObject("Base32", "base32", List.of())),
                "base32",
                List.of()
        );

        TestCandidateExecutionResult result = TestCandidateExecutionResult.returned(
                candidate,
                List.of(),
                "sample.Base32$Builder@1"
        );

        String code = new JUnitCandidateTestWriter().write("sample.Base32", List.of(result));

        assertTrue(code.contains("import static org.junit.jupiter.api.Assertions.assertNotNull;"));
        assertTrue(code.contains("Object result = base32.builder();"));
        assertTrue(code.contains("assertNotNull(result);"));
    }

}
