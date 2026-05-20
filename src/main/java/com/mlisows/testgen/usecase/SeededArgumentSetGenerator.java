package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.GeneratedArgument;
import com.mlisows.testgen.domain.ParameterModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SeededArgumentSetGenerator {
    private final SeedValueGenerator seedValueGenerator;

    public SeededArgumentSetGenerator(SeedValueGenerator seedValueGenerator) {
        this.seedValueGenerator = Objects.requireNonNull(seedValueGenerator, "seedValueGenerator must not be null");
    }

    public List<List<GeneratedArgument>> generateArgumentSets(List<ParameterModel> parameters) {
        Objects.requireNonNull(parameters, "parameters must not be null");

        if (parameters.isEmpty()) {
            return List.of(List.of());
        }

        if (parameters.size() == 1) {
            return generateSingleParameterSets(parameters.get(0));
        }

        return List.of(generateDefaultArgumentSet(parameters));
    }

    private List<List<GeneratedArgument>> generateSingleParameterSets(ParameterModel parameter) {
        List<String> seedValues = seedValueGenerator.seedValuesFor(parameter.getType());
        List<List<GeneratedArgument>> argumentSets = new ArrayList<>();

        for (String seedValue : seedValues) {
            argumentSets.add(List.of(new GeneratedArgument(parameter.getType(), seedValue)));
        }

        return argumentSets;
    }

    private List<GeneratedArgument> generateDefaultArgumentSet(List<ParameterModel> parameters) {
        List<GeneratedArgument> arguments = new ArrayList<>();

        for (ParameterModel parameter : parameters) {
            List<String> seedValues = seedValueGenerator.seedValuesFor(parameter.getType());

            if (seedValues.isEmpty()) {
                return List.of();
            }

            arguments.add(new GeneratedArgument(parameter.getType(), seedValues.get(0)));
        }

        return arguments;
    }
}
