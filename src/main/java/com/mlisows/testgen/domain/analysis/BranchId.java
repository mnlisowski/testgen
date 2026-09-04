package com.mlisows.testgen.domain.analysis;

import java.util.Objects;

public final class BranchId {
    private final String className;
    private final String methodName;
    private final int lineNumber;
    private final BranchKind branchKind;
    private final BranchType branchType;
    private final String discriminator;

    public BranchId(String className, String methodName, int lineNumber, BranchKind branchKind, BranchType branchType, String discriminator){
        this.className = Objects.requireNonNull(className, "className must not be null");
        this.methodName = Objects.requireNonNull(methodName, "methodName must not be null");
        this.branchKind = Objects.requireNonNull(branchKind, "branchKind must not be null");
        this.branchType = Objects.requireNonNull(branchType, "branchType must not be null");

        if (lineNumber < 1) {
            throw new IllegalArgumentException("lineNumber must be positive");
        }

        this.lineNumber = lineNumber;
        this.discriminator = discriminator == null ? "" : discriminator;
    }

    public static BranchId parse(String value) {
        Objects.requireNonNull(value, "value must not be null");

        String[] parts = value.split("\\|", 6);

        if (parts.length < 5) {
            throw new IllegalArgumentException("Invalid branch id: " + value);
        }

        return new BranchId(
                parts[0],
                parts[1],
                parseLineNumber(parts[2], value),
                parseBranchKind(parts[3], value),
                parseBranchType(parts[4], value),
                parts.length == 6 ? parts[5] : ""
        );
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

    public BranchKind getBranchKind() {
        return branchKind;
    }

    public BranchType getBranchType() {
        return branchType;
    }

    public String getDiscriminator() {
        return discriminator;
    }

    public String methodSignature() {
        return className + "." + methodName;
    }

    public String asString() {
        String baseId = className + "|" + methodName + "|" + lineNumber + "|" + branchKind + "|" + branchType;

        if (discriminator.isEmpty()) {
            return baseId;
        }

        return baseId + "|" + discriminator;
    }

    private static int parseLineNumber(String value, String branchId) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid branch id: " + branchId, exception);
        }
    }

    private static BranchKind parseBranchKind(String value, String branchId) {
        try {
            return BranchKind.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid branch id: " + branchId, exception);
        }
    }

    private static BranchType parseBranchType(String value, String branchId) {
        try {
            return BranchType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid branch id: " + branchId, exception);
        }
    }
}
