package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.GeneratedArgument;
import com.mlisows.testgen.domain.GeneratedTestCase;
import com.mlisows.testgen.domain.MethodModel;
import com.mlisows.testgen.domain.GeneratedSetupObject;

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

        String targetVariableName = decapitalize(simpleName(classStructure.getClassName()));

        List<GeneratedSetupObject> setupObjects = List.of(new GeneratedSetupObject(
                simpleName(classStructure.getClassName()),
                targetVariableName,
                List.of()
        ));

        return new GeneratedTestCase(
                classStructure.getClassName(),
                method.getName(),
                method.getReturnType(),
                "shouldCall" + capitalize(method.getName()),
                setupObjects,
                targetVariableName,
                methodArguments
        );


    }

    public List<GeneratedTestCase> createAll(ClassStructure classStructure, MethodModel method) {
        Objects.requireNonNull(classStructure, "classStructure must not be null");
        Objects.requireNonNull(method, "method must not be null");

        List<List<GeneratedArgument>> argumentSets = argumentSetGenerator.generateArgumentSets(method.getParameters());
        List<GeneratedTestCase> testCases = new ArrayList<>();
        String targetVariableName = decapitalize(simpleName(classStructure.getClassName()));

        List<GeneratedSetupObject> setupObjects = List.of(new GeneratedSetupObject(
                simpleName(classStructure.getClassName()),
                targetVariableName,
                List.of()
        ));

        int index = 1;
        for (List<GeneratedArgument> methodArguments : argumentSets) {
            testCases.add(new GeneratedTestCase(
                    classStructure.getClassName(),
                    method.getName(),
                    method.getReturnType(),
                    "shouldCall" + capitalize(method.getName()) + index,
                    setupObjects,
                    targetVariableName,
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


