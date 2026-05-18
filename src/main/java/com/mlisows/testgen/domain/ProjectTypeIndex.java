package com.mlisows.testgen.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ProjectTypeIndex {
    private final Map<String, TypeInfo> typesByName;

    public ProjectTypeIndex(List<TypeInfo> types) {
        Objects.requireNonNull(types, "types must not be null");

        this.typesByName = new HashMap<>();

        for (TypeInfo type : types) {
            typesByName.put(type.getName(), type);
            typesByName.put(type.getFullyQualifiedName(), type);
        }
    }

    public Optional<TypeInfo> findByName(String name) {
        Objects.requireNonNull(name, "name must not be null");

        return Optional.ofNullable(typesByName.get(name));
    }
}
