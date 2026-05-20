package com.mlisows.testgen.usecase;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeedValueGeneratorTest {

    @Test
    void shouldGenerateSeedValuesForNumericTypes() {
        SeedValueGenerator generator = new SeedValueGenerator();

        assertEquals(List.of("-1", "0", "1", "10", "100"), generator.seedValuesFor("int"));
        assertEquals(List.of("-1", "0", "1", "10", "100"), generator.seedValuesFor("long"));
        assertEquals(List.of("-1.0", "0.0", "1.0", "10.0", "100.0"), generator.seedValuesFor("double"));
        assertEquals(List.of("-1.0f", "0.0f", "1.0f", "10.0f", "100.0f"), generator.seedValuesFor("float"));
    }

    @Test
    void shouldGenerateSeedValuesForBooleanType() {
        SeedValueGenerator generator = new SeedValueGenerator();

        assertEquals(List.of("false", "true"), generator.seedValuesFor("boolean"));
    }

    @Test
    void shouldGenerateSeedValuesForStringType() {
        SeedValueGenerator generator = new SeedValueGenerator();

        assertEquals(List.of("\"\"", "\"test\"", "\"a\""), generator.seedValuesFor("String"));
        assertEquals(List.of("\"\"", "\"test\"", "\"a\""), generator.seedValuesFor("java.lang.String"));
    }

    @Test
    void shouldReturnEmptyListForUnsupportedType() {
        SeedValueGenerator generator = new SeedValueGenerator();

        assertEquals(List.of(), generator.seedValuesFor("Order"));
    }
}
