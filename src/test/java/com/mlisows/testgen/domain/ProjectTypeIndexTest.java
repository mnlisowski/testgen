package com.mlisows.testgen.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectTypeIndexTest {

    @Test
    void shouldFindTypeBySimpleName() {
        ProjectTypeIndex index = new ProjectTypeIndex(List.of(
                new TypeInfo("PaymentGateway", "sample.PaymentGateway", TypeKind.INTERFACE)
        ));

        Optional<TypeInfo> type = index.findByName("PaymentGateway");

        assertTrue(type.isPresent());
        assertEquals(TypeKind.INTERFACE, type.get().getKind());
    }

    @Test
    void shouldFindTypeByFullyQualifiedName() {
        ProjectTypeIndex index = new ProjectTypeIndex(List.of(
                new TypeInfo("PaymentGateway", "sample.PaymentGateway", TypeKind.INTERFACE)
        ));

        Optional<TypeInfo> type = index.findByName("sample.PaymentGateway");

        assertTrue(type.isPresent());
        assertEquals("PaymentGateway", type.get().getName());
    }

    @Test
    void shouldReturnEmptyForUnknownType() {
        ProjectTypeIndex index = new ProjectTypeIndex(List.of());

        Optional<TypeInfo> type = index.findByName("UnknownType");

        assertTrue(type.isEmpty());
    }
}

