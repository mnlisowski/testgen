package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.GeneratedArgument;
import com.mlisows.testgen.domain.GeneratedTestCase;
import com.mlisows.testgen.domain.MethodModel;

import java.util.List;
import java.util.Objects;

public final class GeneratedTestCaseFactory {
    private final SimpleArgumentGenerator argumentGenerator;

    public GeneratedTestCaseFactory(SimpleArgumentGenerator argumentGenerator) {
        this.argumentGenerator = Objects.requireNonNull(argumentGenerator, "argumentGenerator must not be null");
    }

    public GeneratedTestCase create(ClassStructure classStructure, MethodModel method) {
        Objects.requireNonNull(classStructure, "classStructure must not be null");
        Objects.requireNonNull(method, "method must not be null");

        List<GeneratedArgument> methodArguments = argumentGenerator.generateArguments(method.getParameters());

        return new GeneratedTestCase(
                classStructure.getClassName(),
                method.getName(),
                "shouldCall" + capitalize(method.getName()),
                List.of(),
                methodArguments
        );
    }

    private String capitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }

        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }
}


