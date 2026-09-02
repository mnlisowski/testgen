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
    private static final List<String> EQUALS_ASSERTION_RETURN_TYPES = List.of(
            "byte", "short", "int", "long", "float", "double", "boolean", "char",
            "Byte", "Short", "Integer", "Long", "Float", "Double", "Boolean", "Character", "String",
            "java.lang.Byte", "java.lang.Short", "java.lang.Integer", "java.lang.Long",
            "java.lang.Float", "java.lang.Double", "java.lang.Boolean", "java.lang.Character", "java.lang.String"
    );

    public String write(String className, List<TestCandidateExecutionResult> selectedResults) {
        Objects.requireNonNull(className, "className must not be null");
        Objects.requireNonNull(selectedResults, "selectedResults must not be null");

        List<TestCandidateExecutionResult> writableResults = selectedResults.stream()
                .filter(result -> result.getCandidate().getClassName().equals(className))
                .filter(this::isWritableResult)
                .toList();

        StringBuilder builder = new StringBuilder();
        String packageName = packageName(className);

        if (!packageName.isEmpty()) {
            builder.append("package ").append(packageName).append(";\n\n");
        }

        builder.append("import org.junit.jupiter.api.Test;\n");
        builder.append("\n");
        builder.append("import static org.junit.jupiter.api.Assertions.assertEquals;\n");
        builder.append("import static org.junit.jupiter.api.Assertions.assertNotNull;\n");
        builder.append("import static org.junit.jupiter.api.Assertions.assertNull;\n");
        builder.append("import static org.junit.jupiter.api.Assertions.assertThrows;\n\n");
        builder.append("class ").append(simpleName(className)).append("Test {\n\n");

        int index = 1;
        for (TestCandidateExecutionResult result : writableResults) {
            appendTestMethod(builder, result, index);
            index++;
        }

        builder.append("}\n");
        return builder.toString();
    }

    private boolean isWritableResult(TestCandidateExecutionResult result) {
        return result.getOutcome() == ExecutionOutcome.RETURNED
                || result.getOutcome() == ExecutionOutcome.THREW_EXCEPTION;
    }

    private void appendTestMethod(StringBuilder builder, TestCandidateExecutionResult result, int index) {
        TestCandidate candidate = result.getCandidate();

        builder.append("    @Test\n");
        builder.append("    void shouldCall")
                .append(capitalize(candidate.getMethodName()))
                .append(index)
                .append("() throws Exception {\n");

        for (GeneratedSetupObject setupObject : candidate.getSetupObjects()) {
            appendObjectCreation(builder, setupObject);
        }

        builder.append("\n");

        if (result.getOutcome() == ExecutionOutcome.THREW_EXCEPTION) {
            appendExceptionAssertion(builder, result);
        } else {
            appendReturnedCall(builder, result);
        }

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

    private void appendReturnedCall(StringBuilder builder, TestCandidateExecutionResult result) {
        TestCandidate candidate = result.getCandidate();

        builder.append("        ");

        if (!candidate.getReturnType().equals("void")) {
            builder.append(resultVariableType(candidate.getReturnType())).append(" result = ");
        }

        appendMethodCall(builder, candidate);
        builder.append(";\n");

        if (!candidate.getReturnType().equals("void") && isNullReturn(result)) {
            builder.append("\n");
            builder.append("        assertNull(result);\n");
        } else if (canAssertEquals(candidate.getReturnType())) {
            builder.append("\n");
            builder.append("        assertEquals(")
                    .append(expectedReturnValue(result))
                    .append(", result);\n");
        } else if (canAssertNotNull(candidate.getReturnType())) {
            builder.append("\n");
            builder.append("        assertNotNull(result);\n");
        }
    }

    private void appendExceptionAssertion(StringBuilder builder, TestCandidateExecutionResult result) {
        TestCandidate candidate = result.getCandidate();

        builder.append("        assertThrows(")
                .append(result.getExceptionType().orElseThrow())
                .append(".class, () -> ");
        appendMethodCall(builder, candidate);
        builder.append(");\n");
    }

    private void appendMethodCall(StringBuilder builder, TestCandidate candidate) {
        builder.append(candidate.getTargetVariableName())
                .append(".")
                .append(candidate.getMethodName())
                .append("(")
                .append(argumentValues(candidate.getMethodArguments()))
                .append(")");
    }

    private String argumentValues(List<GeneratedArgument> arguments) {
        return arguments.stream()
                .map(this::argumentValue)
                .collect(Collectors.joining(", "));
    }

    private String argumentValue(GeneratedArgument argument) {
        if (argument.getValue().equals("null")) {
            return "(" + argument.getType() + ") null";
        }

        return argument.getValue();
    }

    private String resultVariableType(String returnType) {
        if (canAssertEquals(returnType)) {
            return returnType;
        }

        return "Object";
    }

    private boolean canAssertEquals(String returnType) {
        return EQUALS_ASSERTION_RETURN_TYPES.contains(returnType);
    }

    private boolean canAssertNotNull(String returnType) {
        return !returnType.equals("void");
    }

    private boolean isNullReturn(TestCandidateExecutionResult result) {
        return result.getReturnValue().isEmpty();
    }

    private String expectedReturnValue(TestCandidateExecutionResult result) {
        String returnType = result.getCandidate().getReturnType();
        String returnValue = result.getReturnValue().orElseThrow();

        if (returnType.equals("String") || returnType.equals("java.lang.String")) {
            return "\"" + escapeJavaString(returnValue) + "\"";
        }

        if (returnType.equals("char")
                || returnType.equals("Character")
                || returnType.equals("java.lang.Character")) {
            return "'" + escapeJavaCharacter(returnValue) + "'";
        }

        return returnValue;
    }

    private String escapeJavaString(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String escapeJavaCharacter(String value) {
        return escapeJavaString(value)
                .replace("'", "\\'");
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
