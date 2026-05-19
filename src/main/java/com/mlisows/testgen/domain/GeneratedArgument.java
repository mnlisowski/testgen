package com.mlisows.testgen.domain;

import java.util.Objects;

public final class GeneratedArgument {
    private final String type;
    private final String value;

    public GeneratedArgument(String type, String value) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }
}
