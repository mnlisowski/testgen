package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.GeneratedArgument;
import com.mlisows.testgen.domain.GeneratedTestCase;
import com.mlisows.testgen.domain.GeneratedTestSuite;


import java.util.Objects;
import java.util.stream.Collectors;

public final class JUnitTestWriter {

    public String write(GeneratedTestCase testCase) {
        Objects.requireNonNull(testCase, "testCase must not be null");

        String simpleClassName = simpleName(testCase.getClassName());
        String testClassName = simpleClassName + "Test";
        String instanceName = decapitalize(simpleClassName);
        String constructorArguments = argumentValues(testCase.getConstructorArguments());
        String methodArguments = argumentValues(testCase.getMethodArguments());

        return """
                  import org.junit.jupiter.api.Test;

                  class %s {

                      @Test
                      void %s() {
                          %s %s = new %s(%s);

                          %s.%s(%s);
                      }
                  }
                  """.formatted(
                testClassName,
                testCase.getTestName(),
                simpleClassName,
                instanceName,
                simpleClassName,
                constructorArguments,
                instanceName,
                testCase.getMethodName(),
                methodArguments
        );
    }

    public String write(GeneratedTestSuite testSuite) {
        Objects.requireNonNull(testSuite, "testSuite must not be null");

        String simpleClassName = simpleName(testSuite.getClassName());
        String testClassName = simpleClassName + "Test";

        StringBuilder builder = new StringBuilder();

        builder.append("import org.junit.jupiter.api.Test;\n\n");
        builder.append("class ").append(testClassName).append(" {\n\n");

        for (GeneratedTestCase testCase : testSuite.getTestCases()) {
            appendTestMethod(builder, testCase);
        }

        builder.append("}\n");

        return builder.toString();
    }

    private void appendTestMethod(StringBuilder builder, GeneratedTestCase testCase) {
        String simpleClassName = simpleName(testCase.getClassName());
        String instanceName = decapitalize(simpleClassName);
        String constructorArguments = argumentValues(testCase.getConstructorArguments());
        String methodArguments = argumentValues(testCase.getMethodArguments());

        builder.append("    @Test\n");
        builder.append("    void ").append(testCase.getTestName()).append("() {\n");
        builder.append("        ")
                .append(simpleClassName)
                .append(" ")
                .append(instanceName)
                .append(" = new ")
                .append(simpleClassName)
                .append("(")
                .append(constructorArguments)
                .append(");\n\n");

        builder.append("        ")
                .append(instanceName)
                .append(".")
                .append(testCase.getMethodName())
                .append("(")
                .append(methodArguments)
                .append(");\n");

        builder.append("    }\n\n");
    }


    private String argumentValues(java.util.List<GeneratedArgument> arguments) {
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
}
