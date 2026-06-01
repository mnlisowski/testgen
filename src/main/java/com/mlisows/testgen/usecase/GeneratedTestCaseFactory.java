package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.GeneratedArgument;
import com.mlisows.testgen.domain.GeneratedSetupObject;
import com.mlisows.testgen.domain.GeneratedTestCase;
import com.mlisows.testgen.domain.MethodModel;
import com.mlisows.testgen.domain.ParameterModel;
import com.mlisows.testgen.domain.ProjectClassStructureIndex;
import com.mlisows.testgen.domain.ProjectTypeIndex;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class GeneratedTestCaseFactory {
    private final SimpleArgumentGenerator argumentGenerator;
    private final SeededArgumentSetGenerator argumentSetGenerator;
    private final SimpleSetupObjectGenerator setupObjectGenerator;

    public GeneratedTestCaseFactory(
            SimpleArgumentGenerator argumentGenerator,
            SeededArgumentSetGenerator argumentSetGenerator,
            SimpleSetupObjectGenerator setupObjectGenerator
    ) {
        this.argumentGenerator = Objects.requireNonNull(argumentGenerator, "argumentGenerator must not be null");
        this.argumentSetGenerator = Objects.requireNonNull(argumentSetGenerator, "argumentSetGenerator must not be null");
        this.setupObjectGenerator = Objects.requireNonNull(setupObjectGenerator, "setupObjectGenerator must not be null");
    }

    public GeneratedTestCaseFactory(
            SimpleArgumentGenerator argumentGenerator,
            SeededArgumentSetGenerator argumentSetGenerator
    ) {
        this(
                argumentGenerator,
                argumentSetGenerator,
                new SimpleSetupObjectGenerator(new SeedValueGenerator())
        );
    }

    public GeneratedTestCaseFactory(SimpleArgumentGenerator argumentGenerator) {
        this(
                argumentGenerator,
                new SeededArgumentSetGenerator(new SeedValueGenerator()),
                new SimpleSetupObjectGenerator(new SeedValueGenerator())
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
        return createAll(classStructure, method, new ProjectTypeIndex(List.of()));
    }

    public List<GeneratedTestCase> createAll(
            ClassStructure classStructure,
            MethodModel method,
            ProjectTypeIndex typeIndex
    ) {
        return createAll(
                classStructure,
                method,
                typeIndex,
                new ProjectClassStructureIndex(List.of(classStructure))
        );
    }

    public List<GeneratedTestCase> createAll(
            ClassStructure classStructure,
            MethodModel method,
            ProjectTypeIndex typeIndex,
            ProjectClassStructureIndex classIndex
    ) {
        Objects.requireNonNull(classStructure, "classStructure must not be null");
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(typeIndex, "typeIndex must not be null");
        Objects.requireNonNull(classIndex, "classIndex must not be null");

        Optional<ObjectAwareArguments> objectAwareArguments = generateObjectAwareArguments(
                method.getParameters(),
                classIndex,
                typeIndex
        );

        if (objectAwareArguments.isEmpty()) {
            return List.of();
        }

        if (objectAwareArguments.get().hasSetupObjects()) {
            return List.of(createTestCase(
                    classStructure,
                    method,
                    "shouldCall" + capitalize(method.getName()) + "1",
                    objectAwareArguments.get().setupObjects(),
                    objectAwareArguments.get().methodArguments()
            ));
        }

        List<List<GeneratedArgument>> argumentSets = argumentSetGenerator.generateArgumentSets(
                method.getParameters(),
                typeIndex
        );

        List<GeneratedTestCase> testCases = new ArrayList<>();
        int index = 1;

        for (List<GeneratedArgument> methodArguments : argumentSets) {
            testCases.add(createTestCase(
                    classStructure,
                    method,
                    "shouldCall" + capitalize(method.getName()) + index,
                    List.of(),
                    methodArguments
            ));
            index++;
        }

        return testCases;
    }

    private Optional<ObjectAwareArguments> generateObjectAwareArguments(
            List<ParameterModel> parameters,
            ProjectClassStructureIndex classIndex,
            ProjectTypeIndex typeIndex
    ) {
        List<GeneratedSetupObject> setupObjects = new ArrayList<>();
        List<GeneratedArgument> methodArguments = new ArrayList<>();

        for (ParameterModel parameter : parameters) {
            Optional<SetupObjectResolution> setupResolution = setupObjectGenerator.generateSetup(
                    parameter,
                    classIndex,
                    typeIndex
            );

            if (setupResolution.isPresent()) {
                setupObjects.addAll(setupResolution.get().getSetupObjects());
                methodArguments.add(setupResolution.get().getReferenceArgument());
                continue;
            }

            List<List<GeneratedArgument>> argumentSets = argumentSetGenerator.generateArgumentSets(
                    List.of(parameter),
                    typeIndex
            );

            if (argumentSets.isEmpty() || argumentSets.get(0).isEmpty()) {
                return Optional.empty();
            }

            methodArguments.add(argumentSets.get(0).get(0));
        }

        return Optional.of(new ObjectAwareArguments(setupObjects, methodArguments));
    }

    private GeneratedTestCase createTestCase(
            ClassStructure classStructure,
            MethodModel method,
            String testName,
            List<GeneratedSetupObject> methodSetupObjects,
            List<GeneratedArgument> methodArguments
    ) {
        String targetVariableName = decapitalize(simpleName(classStructure.getClassName()));

        List<GeneratedSetupObject> setupObjects = new ArrayList<>(methodSetupObjects);
        setupObjects.add(new GeneratedSetupObject(
                simpleName(classStructure.getClassName()),
                targetVariableName,
                List.of()
        ));

        return new GeneratedTestCase(
                classStructure.getClassName(),
                method.getName(),
                method.getReturnType(),
                testName,
                setupObjects,
                targetVariableName,
                methodArguments
        );
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

    private record ObjectAwareArguments(
            List<GeneratedSetupObject> setupObjects,
            List<GeneratedArgument> methodArguments
    ) {
        boolean hasSetupObjects() {
            return !setupObjects.isEmpty();
        }
    }
}
