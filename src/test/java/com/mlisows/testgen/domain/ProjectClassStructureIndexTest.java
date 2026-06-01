package com.mlisows.testgen.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectClassStructureIndexTest {

    @Test
    void shouldFindClassStructureByFullyQualifiedName() {
        ClassStructure order = new ClassStructure(
                "sample.Order",
                List.of(),
                List.of()
        );

        ProjectClassStructureIndex index = new ProjectClassStructureIndex(List.of(order));

        assertEquals(order, index.findByName("sample.Order").orElseThrow());
    }

    @Test
    void shouldFindClassStructureBySimpleName() {
        ClassStructure order = new ClassStructure(
                "sample.Order",
                List.of(),
                List.of()
        );

        ProjectClassStructureIndex index = new ProjectClassStructureIndex(List.of(order));

        assertEquals(order, index.findByName("Order").orElseThrow());
    }

    @Test
    void shouldReturnEmptyForUnknownClass() {
        ProjectClassStructureIndex index = new ProjectClassStructureIndex(List.of());

        assertTrue(index.findByName("Order").isEmpty());
    }
}
