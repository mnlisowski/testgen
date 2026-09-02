package com.mlisows.testgen.domain;

import java.util.List;
import java.util.Objects;

public final class ObservedInvocation {
    private final String ownerId;
    private final List<GeneratedArgument> arguments;

    public ObservedInvocation(String ownerId, List<GeneratedArgument> arguments) {
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId must not be null");
        this.arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments must not be null"));
    }

    public String getOwnerId() {
        return ownerId;
    }

    public List<GeneratedArgument> getArguments() {
        return arguments;
    }
}
