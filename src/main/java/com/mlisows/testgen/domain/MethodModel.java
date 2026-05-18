package com.mlisows.testgen.domain;

import java.util.List;
import java.util.Objects;

public final class MethodModel {
    private final String name;
    private final String returnType;
    private final boolean publicMethod;
    private final List<ParameterModel> parameters;

    public MethodModel(String name, String returnType, boolean publicMethod, List<ParameterModel> parameters) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.returnType = Objects.requireNonNull(returnType, "returnType must not be null");
        this.publicMethod = publicMethod;
        this.parameters = List.copyOf(Objects.requireNonNull(parameters, "parameters must not be null"));
    }

    public String getName() {
        return name;
    }

    public String getReturnType() {
        return returnType;
    }

    public boolean isPublicMethod() {
        return publicMethod;
    }

    public List<ParameterModel> getParameters() {
        return parameters;
    }
}

