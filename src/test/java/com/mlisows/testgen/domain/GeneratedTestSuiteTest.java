package com.mlisows.testgen.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeneratedTestSuiteTest {

    @Test
    void shouldStoreGeneratedTestSuiteData() {
        GeneratedTestCase testCase = new GeneratedTestCase(
                "sample.Calculator",
                "calculate",
                "shouldCallCalculate",
                List.of(),
                List.of(new GeneratedArgument("int", "0"))
        );

        GeneratedTestSuite testSuite = new GeneratedTestSuite(
                "sample.Calculator",
                List.of(testCase)
        );

        assertEquals("sample.Calculator", testSuite.getClassName());
        assertEquals(List.of(testCase), testSuite.getTestCases());
    }
}
