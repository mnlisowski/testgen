package com.mlisows.testgen.domain;

import java.util.List;
import java.util.Objects;

public final class GeneratedTestSuite {
    private final String className;
    private final List<GeneratedTestCase> testCases;

    public GeneratedTestSuite(String className, List<GeneratedTestCase> testCases) {
        this.className = Objects.requireNonNull(className, "className must not be null");
        this.testCases = List.copyOf(Objects.requireNonNull(testCases, "testCases must not be null"));
    }

    public String getClassName() {
        return className;
    }

    public List<GeneratedTestCase> getTestCases() {
        return testCases;
    }
}
