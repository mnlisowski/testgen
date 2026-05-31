package com.mlisows.testgen.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeneratedTestCaseTest {

    @Test
    void shouldStoreGeneratedTestCaseData() {
        GeneratedSetupObject setupObject = new GeneratedSetupObject(
                "PaymentService",
                "paymentService",
                List.of()
        );
        GeneratedArgument methodArgument = new GeneratedArgument("int", "100");

        GeneratedTestCase testCase = new GeneratedTestCase(
                "sample.PaymentService",
                "pay",
                "boolean",
                "shouldPay",
                List.of(setupObject),
                "paymentService",
                List.of(methodArgument)
        );

        assertEquals("sample.PaymentService", testCase.getClassName());
        assertEquals("pay", testCase.getMethodName());
        assertEquals("boolean", testCase.getReturnType());
        assertEquals("shouldPay", testCase.getTestName());
        assertEquals(List.of(setupObject), testCase.getSetupObjects());
        assertEquals("paymentService", testCase.getTargetVariableName());
        assertEquals(List.of(methodArgument), testCase.getMethodArguments());
    }
}
