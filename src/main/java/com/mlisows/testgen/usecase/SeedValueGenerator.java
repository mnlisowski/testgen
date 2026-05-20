package com.mlisows.testgen.usecase;

import java.util.List;
import java.util.Objects;

public final class SeedValueGenerator {

    public List<String> seedValuesFor(String type) {
        Objects.requireNonNull(type, "type must not be null");

        if (type.equals("int") || type.equals("long")) {
            return List.of("-1", "0", "1", "10", "100");
        }

        if (type.equals("double")) {
            return List.of("-1.0", "0.0", "1.0", "10.0", "100.0");
        }

        if (type.equals("float")) {
            return List.of("-1.0f", "0.0f", "1.0f", "10.0f", "100.0f");
        }

        if (type.equals("boolean")) {
            return List.of("false", "true");
        }

        if (type.equals("String") || type.equals("java.lang.String")) {
            return List.of("\"\"", "\"test\"", "\"a\"");
        }

        return List.of();
    }
}
