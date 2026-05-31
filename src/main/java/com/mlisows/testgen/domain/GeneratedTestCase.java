package com.mlisows.testgen.domain;

import java.util.List;
import java.util.Objects;

public final class GeneratedTestCase {
    private final String className;
    private final String methodName;
    private final String returnType;
    private final String testName;
    private final List<GeneratedSetupObject> setupObjects;
    private final String targetVariableName;
    private final List<GeneratedArgument> methodArguments;

    public GeneratedTestCase(
            String className,
            String methodName,
            String returnType,
            String testName,
            List<GeneratedSetupObject> setupObjects,
            String targetVariableName,
            List<GeneratedArgument> methodArguments
    ) {
        this.className = Objects.requireNonNull(className, "className must not be null");
        this.methodName = Objects.requireNonNull(methodName, "methodName must not be null");
        this.returnType = Objects.requireNonNull(returnType, "returnType must not be null");
        this.testName = Objects.requireNonNull(testName, "testName must not be null");
        this.setupObjects = List.copyOf(Objects.requireNonNull(setupObjects, "setupObjects must not be null"));
        this.targetVariableName = Objects.requireNonNull(targetVariableName, "targetVariableName must not be null");
        this.methodArguments = List.copyOf(Objects.requireNonNull(methodArguments, "methodArguments must not be null"));
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getReturnType() {
        return returnType;
    }

    public String getTestName() {
        return testName;
    }

    public String getTargetVariableName() {
        return targetVariableName;
    }

    public List<GeneratedArgument> getMethodArguments() {
        return methodArguments;
    }

    public List<GeneratedSetupObject> getSetupObjects() {
        return setupObjects;
    }
}
