package com.mlisows.testgen.domain;

import java.util.List;
import java.util.Objects;

public final class ClassStructure {
    private final String className;
    private final List<ConstructorModel> constructors;
    private final List<MethodModel> methods;

    public ClassStructure(String className, List<ConstructorModel> constructors, List<MethodModel> methods) {
        this.className = Objects.requireNonNull(className, "className must not be null");
        this.constructors = List.copyOf(Objects.requireNonNull(constructors, "constructors must not be null"));
        this.methods = List.copyOf(Objects.requireNonNull(methods, "methods must not be null"));
    }

    public String getClassName() {
        return className;
    }

    public List<ConstructorModel> getConstructors() {
        return constructors;
    }

    public List<MethodModel> getMethods() {
        return methods;
    }
}
