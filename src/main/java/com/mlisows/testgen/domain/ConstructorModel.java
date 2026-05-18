package com.mlisows.testgen.domain;

import java.util.List;
import java.util.Objects;

public final class ConstructorModel {
    private final boolean publicConstructor;
    private final List<ParameterModel> parameters;

    public ConstructorModel(boolean publicConstructor, List<ParameterModel> parameters) {
        this.publicConstructor = publicConstructor;
        this.parameters = List.copyOf(Objects.requireNonNull(parameters, "parameters must not be null"));
    }

    public boolean isPublicConstructor() {
        return publicConstructor;
    }

    public List<ParameterModel> getParameters() {
        return parameters;
    }
}
