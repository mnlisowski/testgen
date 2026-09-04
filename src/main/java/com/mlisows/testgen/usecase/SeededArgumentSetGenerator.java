package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.generation.GeneratedArgument;
import com.mlisows.testgen.domain.structure.ParameterModel;
import com.mlisows.testgen.domain.structure.ProjectTypeIndex;
import com.mlisows.testgen.domain.structure.TypeKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SeededArgumentSetGenerator {
    private final SeedValueGenerator seedValueGenerator;

    public SeededArgumentSetGenerator(SeedValueGenerator seedValueGenerator) {
        this.seedValueGenerator = Objects.requireNonNull(seedValueGenerator, "seedValueGenerator must not be null");
    }

    public List<List<GeneratedArgument>> generateArgumentSets(List<ParameterModel> parameters) {
        return generateArgumentSets(parameters, new ProjectTypeIndex(List.of()));
    }

    public List<List<GeneratedArgument>> generateArgumentSets(
            List<ParameterModel> parameters,
            ProjectTypeIndex typeIndex
    ) {
        Objects.requireNonNull(parameters, "parameters must not be null");
        Objects.requireNonNull(typeIndex, "typeIndex must not be null");

        if (parameters.isEmpty()) {
            return List.of(List.of());
        }

        if (parameters.size() == 1) {
            return generateSingleParameterSets(parameters.get(0), typeIndex);
        }

        return List.of(generateDefaultArgumentSet(parameters, typeIndex));
    }

    private List<List<GeneratedArgument>> generateSingleParameterSets(
            ParameterModel parameter,
            ProjectTypeIndex typeIndex
    ) {
        List<String> seedValues = seedValuesFor(parameter.getType(), typeIndex);
        List<List<GeneratedArgument>> argumentSets = new ArrayList<>();

        for (String seedValue : seedValues) {
            argumentSets.add(List.of(new GeneratedArgument(parameter.getType(), seedValue)));
        }

        return argumentSets;
    }

    private List<GeneratedArgument> generateDefaultArgumentSet(
            List<ParameterModel> parameters,
            ProjectTypeIndex typeIndex
    ) {
        List<GeneratedArgument> arguments = new ArrayList<>();

        for (ParameterModel parameter : parameters) {
            List<String> seedValues = seedValuesFor(parameter.getType(), typeIndex);

            if (seedValues.isEmpty()) {
                return List.of();
            }

            arguments.add(new GeneratedArgument(parameter.getType(), seedValues.get(0)));
        }

        return arguments;
    }

    private List<String> seedValuesFor(String type, ProjectTypeIndex typeIndex) {
        List<String> seedValues = seedValueGenerator.seedValuesFor(type);

        if (!seedValues.isEmpty()) {
            return seedValues;
        }

        return typeIndex.findByName(type)
                .filter(typeInfo -> typeInfo.getKind() == TypeKind.ENUM)
                .map(typeInfo -> typeInfo.getEnumConstants().stream()
                        .map(enumConstant -> typeInfo.getFullyQualifiedName() + "." + enumConstant)
                        .toList())
                .orElse(List.of());
    }
}
