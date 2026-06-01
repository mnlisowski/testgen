package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.GeneratedArgument;
import com.mlisows.testgen.domain.GeneratedSetupObject;

import java.util.List;
import java.util.Objects;

public final class SetupObjectResolution {
    private final List<GeneratedSetupObject> setupObjects;
    private final GeneratedArgument referenceArgument;

    public SetupObjectResolution(List<GeneratedSetupObject> setupObjects, GeneratedArgument referenceArgument) {
        this.setupObjects = List.copyOf(Objects.requireNonNull(setupObjects, "setupObjects must not be null"));
        this.referenceArgument = Objects.requireNonNull(referenceArgument, "referenceArgument must not be null");
    }

    public List<GeneratedSetupObject> getSetupObjects() {
        return setupObjects;
    }

    public GeneratedArgument getReferenceArgument() {
        return referenceArgument;
    }
}

