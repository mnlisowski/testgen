package com.mlisows.testgen.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeneratedTestCaseTest {

    @Test
    void shouldStoreGeneratedTestCaseData() {
        GeneratedArgument constructorArgument = new GeneratedArgument("PaymentGateway", "paymentGateway");
        GeneratedArgument methodArgument = new GeneratedArgument("int", "100");

        GeneratedTestCase testCase = new GeneratedTestCase(
                "sample.PaymentService",
                "pay",
                "shouldPay",
                List.of(constructorArgument),
                List.of(methodArgument)
        );

        assertEquals("sample.PaymentService", testCase.getClassName());
        assertEquals("pay", testCase.getMethodName());
        assertEquals("shouldPay", testCase.getTestName());
        assertEquals(List.of(constructorArgument), testCase.getConstructorArguments());
        assertEquals(List.of(methodArgument), testCase.getMethodArguments());
    }


}
