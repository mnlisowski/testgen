package com.mlisows.testgen.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ProjectClassStructureIndex {
    private final Map<String, ClassStructure> classesByName;

    public ProjectClassStructureIndex(List<ClassStructure> classStructures) {
        Objects.requireNonNull(classStructures, "classStructures must not be null");

        this.classesByName = new HashMap<>();

        for (ClassStructure classStructure : classStructures) {
            classesByName.put(classStructure.getClassName(), classStructure);
            classesByName.put(simpleName(classStructure.getClassName()), classStructure);
        }
    }

    public Optional<ClassStructure> findByName(String name) {
        Objects.requireNonNull(name, "name must not be null");

        return Optional.ofNullable(classesByName.get(name));
    }

    private String simpleName(String className) {
        int lastDotIndex = className.lastIndexOf('.');

        if (lastDotIndex == -1) {
            return className;
        }

        return className.substring(lastDotIndex + 1);
    }
}
