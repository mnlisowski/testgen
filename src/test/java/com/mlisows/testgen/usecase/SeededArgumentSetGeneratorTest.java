package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.GeneratedArgument;
import com.mlisows.testgen.domain.ParameterModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeededArgumentSetGeneratorTest {

    @Test
    void shouldGenerateArgumentSetsForSingleParameter() {
        SeededArgumentSetGenerator generator = new SeededArgumentSetGenerator(new SeedValueGenerator());

        List<List<GeneratedArgument>> argumentSets = generator.generateArgumentSets(List.of(
                new ParameterModel("amount", "int")
        ));

        assertEquals(5, argumentSets.size());
        assertEquals("-1", argumentSets.get(0).get(0).getValue());
        assertEquals("0", argumentSets.get(1).get(0).getValue());
        assertEquals("1", argumentSets.get(2).get(0).getValue());
        assertEquals("10", argumentSets.get(3).get(0).getValue());
        assertEquals("100", argumentSets.get(4).get(0).getValue());
    }

    @Test
    void shouldGenerateEmptyArgumentSetForMethodWithoutParameters() {
        SeededArgumentSetGenerator generator = new SeededArgumentSetGenerator(new SeedValueGenerator());

        List<List<GeneratedArgument>> argumentSets = generator.generateArgumentSets(List.of());

        assertEquals(List.of(List.of()), argumentSets);
    }

    @Test
    void shouldGenerateDefaultArgumentSetForMultipleParameters() {
        SeededArgumentSetGenerator generator = new SeededArgumentSetGenerator(new SeedValueGenerator());

        List<List<GeneratedArgument>> argumentSets = generator.generateArgumentSets(List.of(
                new ParameterModel("amount", "int"),
                new ParameterModel("active", "boolean")
        ));

        assertEquals(1, argumentSets.size());
        assertEquals("-1", argumentSets.get(0).get(0).getValue());
        assertEquals("false", argumentSets.get(0).get(1).getValue());
    }
}
