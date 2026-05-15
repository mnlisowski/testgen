package com.mlisows.testgen.domain;

import java.util.Objects;

public final class BranchId {
    private final String className;
    private final String methodName;
    private final int lineNumber;
    private final BranchType branchType;
    private final String discriminator;

    public BranchId(String className, String methodName, int lineNumber, BranchType branchType, String discriminator){
        this.className = Objects.requireNonNull(className, "className must not be null");
        this.methodName = Objects.requireNonNull(methodName, "methodName must not be null");
        this.branchType = Objects.requireNonNull(branchType, "branchType must not be null");

        if (lineNumber < 1) {
            throw new IllegalArgumentException("lineNumber must be positive");
        }

        this.lineNumber = lineNumber;
        this.discriminator = discriminator == null ? "" : discriminator;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public BranchType getBranchType() {
        return branchType;
    }

    public String getDiscriminator() {
        return discriminator;
    }

    public String asString() {
        String baseId = className + "." + methodName + ".L" + lineNumber + "." + branchType;

        if (discriminator.isEmpty()) {
            return baseId;
        }

        return baseId + "." + discriminator;
    }



}
