package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.ExecutionOutcome;
import com.mlisows.testgen.domain.GeneratedArgument;
import com.mlisows.testgen.domain.GeneratedSetupObject;
import com.mlisows.testgen.domain.TestCandidate;
import com.mlisows.testgen.domain.TestCandidateExecutionResult;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class JUnitCandidateTestWriter {

    public String write(String className, List<TestCandidateExecutionResult> selectedResults) {
        Objects.requireNonNull(className, "className must not be null");
        Objects.requireNonNull(selectedResults, "selectedResults must not be null");

        List<TestCandidateExecutionResult> returnedResults = selectedResults.stream()
                .filter(result -> result.getOutcome() == ExecutionOutcome.RETURNED)
                .filter(result -> result.getCandidate().getClassName().equals(className))
                .toList();

        StringBuilder builder = new StringBuilder();
        String packageName = packageName(className);

        if (!packageName.isEmpty()) {
            builder.append("package ").append(packageName).append(";\n\n");
        }

        builder.append("import org.junit.jupiter.api.Test;\n\n");
        builder.append("class ").append(simpleName(className)).append("Test {\n\n");

        int index = 1;
        for (TestCandidateExecutionResult result : returnedResults) {
            appendTestMethod(builder, result.getCandidate(), index);
            index++;
        }

        builder.append("}\n");
        return builder.toString();
    }

    private void appendTestMethod(StringBuilder builder, TestCandidate candidate, int index) {
        builder.append("    @Test\n");
        builder.append("    void shouldCall")
                .append(capitalize(candidate.getMethodName()))
                .append(index)
                .append("() {\n");

        for (GeneratedSetupObject setupObject : candidate.getSetupObjects()) {
            appendObjectCreation(builder, setupObject);
        }

        builder.append("\n");
        builder.append("        ");
        appendMethodCall(builder, candidate);
        builder.append("    }\n\n");
    }

    private void appendObjectCreation(StringBuilder builder, GeneratedSetupObject setupObject) {
        builder.append("        ")
                .append(setupObject.getType())
                .append(" ")
                .append(setupObject.getVariableName())
                .append(" = new ")
                .append(setupObject.getType())
                .append("(")
                .append(argumentValues(setupObject.getArguments()))
                .append(");\n");
    }

    private void appendMethodCall(StringBuilder builder, TestCandidate candidate) {
        if (!candidate.getReturnType().equals("void")) {
            builder.append(candidate.getReturnType()).append(" result = ");
        }

        builder.append(candidate.getTargetVariableName())
                .append(".")
                .append(candidate.getMethodName())
                .append("(")
                .append(argumentValues(candidate.getMethodArguments()))
                .append(");\n");
    }

    private String argumentValues(List<GeneratedArgument> arguments) {
        return arguments.stream()
                .map(GeneratedArgument::getValue)
                .collect(Collectors.joining(", "));
    }

    private String simpleName(String className) {
        int lastDotIndex = className.lastIndexOf('.');

        if (lastDotIndex == -1) {
            return className;
        }

        return className.substring(lastDotIndex + 1);
    }

    private String packageName(String className) {
        int lastDotIndex = className.lastIndexOf('.');

        if (lastDotIndex == -1) {
            return "";
        }

        return className.substring(0, lastDotIndex);
    }

    private String capitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }

        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }
}
