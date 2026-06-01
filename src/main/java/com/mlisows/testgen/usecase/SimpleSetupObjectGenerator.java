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
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class SimpleSetupObjectGenerator {
    private final SeedValueGenerator seedValueGenerator;

    public SimpleSetupObjectGenerator(SeedValueGenerator seedValueGenerator) {
        this.seedValueGenerator = Objects.requireNonNull(seedValueGenerator, "seedValueGenerator must not be null");
    }

    public Optional<GeneratedSetupObject> generate(
            ParameterModel parameter,
            ProjectClassStructureIndex classIndex,
            ProjectTypeIndex typeIndex
    ) {
        Objects.requireNonNull(parameter, "parameter must not be null");
        Objects.requireNonNull(classIndex, "classIndex must not be null");
        Objects.requireNonNull(typeIndex, "typeIndex must not be null");

        Optional<ClassStructure> classStructure = classIndex.findByName(parameter.getType());

        if (classStructure.isEmpty()) {
            return Optional.empty();
        }

        for (ConstructorModel constructor : classStructure.get().getConstructors()) {
            if (!constructor.isPublicConstructor()) {
                continue;
            }

            Optional<List<GeneratedArgument>> arguments = generateConstructorArguments(
                    constructor.getParameters(),
                    typeIndex
            );

            if (arguments.isPresent()) {
                return Optional.of(new GeneratedSetupObject(
                        simpleName(classStructure.get().getClassName()),
                        decapitalize(simpleName(parameter.getType())),
                        arguments.get()
                ));
            }
        }

        return Optional.empty();
    }

    private Optional<List<GeneratedArgument>> generateConstructorArguments(
            List<ParameterModel> parameters,
            ProjectTypeIndex typeIndex
    ) {
        List<GeneratedArgument> arguments = new ArrayList<>();

        for (ParameterModel parameter : parameters) {
            Optional<GeneratedArgument> argument = generateArgument(parameter, typeIndex);

            if (argument.isEmpty()) {
                return Optional.empty();
            }

            arguments.add(argument.get());
        }

        return Optional.of(arguments);
    }

    private Optional<GeneratedArgument> generateArgument(ParameterModel parameter, ProjectTypeIndex typeIndex) {
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

