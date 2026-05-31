package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.GeneratedArgument;
import com.mlisows.testgen.domain.GeneratedTestCase;
import com.mlisows.testgen.domain.GeneratedTestSuite;
import com.mlisows.testgen.domain.GeneratedSetupObject;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class JUnitTestWriter {

    public String write(GeneratedTestCase testCase) {
        Objects.requireNonNull(testCase, "testCase must not be null");

        return write(new GeneratedTestSuite(
                testCase.getClassName(),
                List.of(testCase)
        ));
    }


    public String write(GeneratedTestSuite testSuite) {
        Objects.requireNonNull(testSuite, "testSuite must not be null");

        String simpleClassName = simpleName(testSuite.getClassName());
        String testClassName = simpleClassName + "Test";
        String packageName = packageName(testSuite.getClassName());

        StringBuilder builder = new StringBuilder();

        if (!packageName.isEmpty()) {
            builder.append("package ")
                    .append(packageName)
                    .append(";\n\n");
        }

        builder.append("import org.junit.jupiter.api.Test;\n\n");
        builder.append("class ").append(testClassName).append(" {\n\n");

        for (GeneratedTestCase testCase : testSuite.getTestCases()) {
            appendTestMethod(builder, testCase);
        }

        builder.append("}\n");

        return builder.toString();
    }

    private void appendTestMethod(StringBuilder builder, GeneratedTestCase testCase) {
        String methodArguments = argumentValues(testCase.getMethodArguments());

        builder.append("    @Test\n");
        builder.append("    void ").append(testCase.getTestName()).append("() {\n");

        for (GeneratedSetupObject setupObject : testCase.getSetupObjects()) {
            appendObjectCreation(builder, setupObject);
        }

        builder.append("\n");
        builder.append("        ");

        if (!testCase.getReturnType().equals("void")) {
            builder.append(testCase.getReturnType())
                    .append(" result = ");
        }

        builder.append(testCase.getTargetVariableName())
                .append(".")
                .append(testCase.getMethodName())
                .append("(")
                .append(methodArguments)
                .append(");\n");

        builder.append("    }\n\n");

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

    private String decapitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }

        return value.substring(0, 1).toLowerCase() + value.substring(1);
    }

    private String packageName(String className) {
        int lastDotIndex = className.lastIndexOf('.');

        if (lastDotIndex == -1) {
            return "";
        }

        return className.substring(0, lastDotIndex);
    }

    private void appendObjectCreation(StringBuilder builder, GeneratedSetupObject objectCreation) {
        builder.append("        ")
                .append(objectCreation.getType())
                .append(" ")
                .append(objectCreation.getVariableName())
                .append(" = new ")
                .append(objectCreation.getType())
                .append("(")
                .append(argumentValues(objectCreation.getArguments()))
                .append(");\n");
    }



}
