package com.mlisows.testgen.domain;

import java.util.List;
import java.util.Objects;

public final class GeneratedSetupObject {
    private final String type;
    private final String variableName;
    private final List<GeneratedArgument> arguments;

    public GeneratedSetupObject(String type, String variableName, List<GeneratedArgument> arguments) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.variableName = Objects.requireNonNull(variableName, "variableName must not be null");
        this.arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments must not be null"));
    }

    public String getType() {
        return type;
    }

    public String getVariableName() {
        return variableName;
    }

    public List<GeneratedArgument> getArguments() {
        return arguments;
    }
}
