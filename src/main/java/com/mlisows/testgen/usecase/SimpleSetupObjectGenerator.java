package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.ConstructorModel;
import com.mlisows.testgen.domain.GeneratedArgument;
import com.mlisows.testgen.domain.GeneratedSetupObject;
import com.mlisows.testgen.domain.ParameterModel;
import com.mlisows.testgen.domain.ProjectClassStructureIndex;
import com.mlisows.testgen.domain.ProjectTypeIndex;
import com.mlisows.testgen.domain.TypeKind;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class SimpleSetupObjectGenerator {
    private static final int MAX_DEPTH = 5;

    private final SeedValueGenerator seedValueGenerator;

    public SimpleSetupObjectGenerator(SeedValueGenerator seedValueGenerator) {
        this.seedValueGenerator = Objects.requireNonNull(seedValueGenerator, "seedValueGenerator must not be null");
    }

    public Optional<GeneratedSetupObject> generate(
            ParameterModel parameter,
            ProjectClassStructureIndex classIndex,
            ProjectTypeIndex typeIndex
    ) {
        return generateSetup(parameter, classIndex, typeIndex)
                .map(resolution -> resolution.getSetupObjects().get(resolution.getSetupObjects().size() - 1));
    }

    public Optional<SetupObjectResolution> generateSetup(
            ParameterModel parameter,
            ProjectClassStructureIndex classIndex,
            ProjectTypeIndex typeIndex
    ) {
        Objects.requireNonNull(parameter, "parameter must not be null");
        Objects.requireNonNull(classIndex, "classIndex must not be null");
        Objects.requireNonNull(typeIndex, "typeIndex must not be null");

        return resolve(parameter, classIndex, typeIndex, 0, Set.of());
    }

    private Optional<SetupObjectResolution> resolve(
            ParameterModel parameter,
            ProjectClassStructureIndex classIndex,
            ProjectTypeIndex typeIndex,
            int depth,
            Set<String> resolvingClasses
    ) {
        if (depth > MAX_DEPTH) {
            return Optional.empty();
        }

        Optional<ClassStructure> classStructure = classIndex.findByName(parameter.getType());

        if (classStructure.isEmpty()) {
            return Optional.empty();
        }

        String className = classStructure.get().getClassName();

        if (resolvingClasses.contains(className)) {
            return Optional.empty();
        }

        Set<String> nextResolvingClasses = new HashSet<>(resolvingClasses);
        nextResolvingClasses.add(className);

        if (classStructure.get().getConstructors().isEmpty()) {
            return Optional.of(noArgConstructorResolution(parameter, className));
        }

        for (ConstructorModel constructor : classStructure.get().getConstructors()) {
            if (!constructor.isPublicConstructor()) {
                continue;
            }

            Optional<ConstructorResolution> constructorResolution = generateConstructorArguments(
                    constructor.getParameters(),
                    classIndex,
                    typeIndex,
                    depth,
                    nextResolvingClasses
            );

            if (constructorResolution.isPresent()) {
                String variableName = variableNameFor(parameter);
                List<GeneratedSetupObject> setupObjects = new ArrayList<>(
                        constructorResolution.get().setupObjects()
                );

                setupObjects.add(new GeneratedSetupObject(
                        simpleName(className),
                        variableName,
                        constructorResolution.get().arguments(),
                        constructor.getParameters()
                ));

                return Optional.of(new SetupObjectResolution(
                        setupObjects,
                        new GeneratedArgument(parameter.getType(), variableName)
                ));
            }
        }

        return Optional.empty();
    }

    private SetupObjectResolution noArgConstructorResolution(ParameterModel parameter, String className) {
        String variableName = variableNameFor(parameter);
        GeneratedSetupObject setupObject = new GeneratedSetupObject(
                simpleName(className),
                variableName,
                List.of()
        );

        return new SetupObjectResolution(
                List.of(setupObject),
                new GeneratedArgument(parameter.getType(), variableName)
        );
    }

    private Optional<ConstructorResolution> generateConstructorArguments(
            List<ParameterModel> parameters,
            ProjectClassStructureIndex classIndex,
            ProjectTypeIndex typeIndex,
            int depth,
            Set<String> resolvingClasses
    ) {
        List<GeneratedSetupObject> setupObjects = new ArrayList<>();
        List<GeneratedArgument> arguments = new ArrayList<>();

        for (ParameterModel parameter : parameters) {
            Optional<GeneratedArgument> simpleArgument = generateSimpleArgument(parameter, typeIndex);

            if (simpleArgument.isPresent()) {
                arguments.add(simpleArgument.get());
                continue;
            }

            Optional<SetupObjectResolution> nestedSetup = resolve(
                    parameter,
                    classIndex,
                    typeIndex,
                    depth + 1,
                    resolvingClasses
            );

            if (nestedSetup.isEmpty()) {
                return Optional.empty();
            }

            setupObjects.addAll(nestedSetup.get().getSetupObjects());
            arguments.add(nestedSetup.get().getReferenceArgument());
        }

        return Optional.of(new ConstructorResolution(setupObjects, arguments));
    }

    private Optional<GeneratedArgument> generateSimpleArgument(ParameterModel parameter, ProjectTypeIndex typeIndex) {
        List<String> seedValues = seedValueGenerator.seedValuesFor(parameter.getType());

        if (!seedValues.isEmpty()) {
            return Optional.of(new GeneratedArgument(parameter.getType(), seedValues.get(0)));
        }

        return typeIndex.findByName(parameter.getType())
                .filter(typeInfo -> typeInfo.getKind() == TypeKind.ENUM)
                .flatMap(typeInfo -> typeInfo.getEnumConstants().stream()
                        .findFirst()
                        .map(enumConstant -> new GeneratedArgument(
                                parameter.getType(),
                                typeInfo.getFullyQualifiedName() + "." + enumConstant
                        )));
    }

    private String variableNameFor(ParameterModel parameter) {
        if (!parameter.getName().isBlank()) {
            return parameter.getName();
        }

        return decapitalize(simpleName(parameter.getType()));
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

    private record ConstructorResolution(
            List<GeneratedSetupObject> setupObjects,
            List<GeneratedArgument> arguments
    ) {
    }
}
