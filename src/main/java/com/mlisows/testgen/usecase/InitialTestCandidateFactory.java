package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.ConstructorModel;
import com.mlisows.testgen.domain.GeneratedArgument;
import com.mlisows.testgen.domain.GeneratedSetupObject;
import com.mlisows.testgen.domain.MethodModel;
import com.mlisows.testgen.domain.ParameterModel;
import com.mlisows.testgen.domain.ProjectClassStructureIndex;
import com.mlisows.testgen.domain.ProjectTypeIndex;
import com.mlisows.testgen.domain.TestCandidate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class InitialTestCandidateFactory {
    private final SeededArgumentSetGenerator argumentSetGenerator;
    private final SimpleSetupObjectGenerator setupObjectGenerator;

    public InitialTestCandidateFactory(
            SeededArgumentSetGenerator argumentSetGenerator,
            SimpleSetupObjectGenerator setupObjectGenerator
    ) {
        this.argumentSetGenerator = Objects.requireNonNull(argumentSetGenerator, "argumentSetGenerator must not be null");
        this.setupObjectGenerator = Objects.requireNonNull(setupObjectGenerator, "setupObjectGenerator must not be null");
    }

    public InitialTestCandidateFactory() {
        this(
                new SeededArgumentSetGenerator(new SeedValueGenerator()),
                new SimpleSetupObjectGenerator(new SeedValueGenerator())
        );
    }

    public Optional<TestCandidate> create(
            ClassStructure classStructure,
            MethodModel method,
            ProjectTypeIndex typeIndex,
            ProjectClassStructureIndex classIndex
    ) {
        Objects.requireNonNull(classStructure, "classStructure must not be null");
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(typeIndex, "typeIndex must not be null");
        Objects.requireNonNull(classIndex, "classIndex must not be null");

        if (!hasPublicNoArgConstructor(classStructure)) {
            return Optional.empty();
        }

        Optional<MethodArguments> methodArguments = generateMethodArguments(
                method.getParameters(),
                classIndex,
                typeIndex
        );

        if (methodArguments.isEmpty()) {
            return Optional.empty();
        }

        String targetVariableName = decapitalize(simpleName(classStructure.getClassName()));
        List<GeneratedSetupObject> setupObjects = new ArrayList<>(methodArguments.get().setupObjects());
        setupObjects.add(new GeneratedSetupObject(
                simpleName(classStructure.getClassName()),
                targetVariableName,
                List.of()
        ));

        return Optional.of(new TestCandidate(
                classStructure.getClassName(),
                method.getName(),
                method.getReturnType(),
                setupObjects,
                targetVariableName,
                methodArguments.get().methodArguments()
        ));
    }

    private Optional<MethodArguments> generateMethodArguments(
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

            Optional<GeneratedArgument> simpleArgument = generateSimpleArgument(parameter, typeIndex);

            if (simpleArgument.isEmpty()) {
                return Optional.empty();
            }

            methodArguments.add(simpleArgument.get());
        }

        return Optional.of(new MethodArguments(setupObjects, methodArguments));
    }

    private Optional<GeneratedArgument> generateSimpleArgument(ParameterModel parameter, ProjectTypeIndex typeIndex) {
        List<List<GeneratedArgument>> argumentSets = argumentSetGenerator.generateArgumentSets(
                List.of(parameter),
                typeIndex
        );

        if (argumentSets.isEmpty() || argumentSets.get(0).isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(argumentSets.get(0).get(0));
    }

    private boolean hasPublicNoArgConstructor(ClassStructure classStructure) {
        if (classStructure.getConstructors().isEmpty()) {
            return true;
        }

        for (ConstructorModel constructor : classStructure.getConstructors()) {
            if (constructor.isPublicConstructor() && constructor.getParameters().isEmpty()) {
                return true;
            }
        }

        return false;
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

    private record MethodArguments(
            List<GeneratedSetupObject> setupObjects,
            List<GeneratedArgument> methodArguments
    ) {
    }
}
