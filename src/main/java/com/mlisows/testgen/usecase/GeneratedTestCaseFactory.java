package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.GeneratedArgument;
import com.mlisows.testgen.domain.GeneratedTestCase;
import com.mlisows.testgen.domain.MethodModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class GeneratedTestCaseFactory {
    private final SimpleArgumentGenerator argumentGenerator;
    private final SeededArgumentSetGenerator argumentSetGenerator;


    public GeneratedTestCaseFactory(
            SimpleArgumentGenerator argumentGenerator,
            SeededArgumentSetGenerator argumentSetGenerator
    ) {
        this.argumentGenerator = Objects.requireNonNull(argumentGenerator, "argumentGenerator must not be null");
        this.argumentSetGenerator = Objects.requireNonNull(argumentSetGenerator, "argumentSetGenerator must not be null");
    }

    public GeneratedTestCaseFactory(SimpleArgumentGenerator argumentGenerator) {
        this(
                argumentGenerator,
                new SeededArgumentSetGenerator(new SeedValueGenerator())
        );
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

    public List<GeneratedTestCase> createAll(ClassStructure classStructure, MethodModel method) {
        Objects.requireNonNull(classStructure, "classStructure must not be null");
        Objects.requireNonNull(method, "method must not be null");

        List<List<GeneratedArgument>> argumentSets = argumentSetGenerator.generateArgumentSets(method.getParameters());
        List<GeneratedTestCase> testCases = new ArrayList<>();

        int index = 1;
        for (List<GeneratedArgument> methodArguments : argumentSets) {
            testCases.add(new GeneratedTestCase(
                    classStructure.getClassName(),
                    method.getName(),
                    "shouldCall" + capitalize(method.getName()) + index,
                    List.of(),
                    methodArguments
            ));
            index++;
        }

        return testCases;
    }

    private String capitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }

        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }
}


