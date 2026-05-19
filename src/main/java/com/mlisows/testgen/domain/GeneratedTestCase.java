package com.mlisows.testgen.domain;

import java.util.List;
import java.util.Objects;

public final class GeneratedTestCase {
    private final String className;
    private final String methodName;
    private final String testName;
    private final List<GeneratedArgument> constructorArguments;
    private final List<GeneratedArgument> methodArguments;

    public GeneratedTestCase(
            String className,
            String methodName,
            String testName,
            List<GeneratedArgument> constructorArguments,
            List<GeneratedArgument> methodArguments
    ) {
        this.className = Objects.requireNonNull(className, "className must not be null");
        this.methodName = Objects.requireNonNull(methodName, "methodName must not be null");
        this.testName = Objects.requireNonNull(testName, "testName must not be null");
        this.constructorArguments = List.copyOf(Objects.requireNonNull(constructorArguments, "constructorArguments must not be null"));
        this.methodArguments = List.copyOf(Objects.requireNonNull(methodArguments, "methodArguments must not be null"));
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getTestName() {
        return testName;
    }

    public List<GeneratedArgument> getConstructorArguments() {
        return constructorArguments;
    }

    public List<GeneratedArgument> getMethodArguments() {
        return methodArguments;
    }
}
