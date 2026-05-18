package com.mlisows.testgen.domain;

import java.util.Objects;

public final class ParameterModel {
    private final String name;
    private final String type;

    public ParameterModel(String name, String type) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
}
