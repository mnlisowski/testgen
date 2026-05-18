package com.mlisows.testgen.domain;

import java.util.Objects;

public final class TypeInfo {
    private final String name;
    private final String fullyQualifiedName;
    private final TypeKind kind;

    public TypeInfo(String name, String fullyQualifiedName, TypeKind kind) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.fullyQualifiedName = Objects.requireNonNull(fullyQualifiedName, "fullyQualifiedName must not be null");
        this.kind = Objects.requireNonNull(kind, "kind must not be null");
    }

    public String getName() {
        return name;
    }

    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    public TypeKind getKind() {
        return kind;
    }
}
