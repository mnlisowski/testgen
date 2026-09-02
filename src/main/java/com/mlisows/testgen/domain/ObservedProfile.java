package com.mlisows.testgen.domain;

import java.util.List;
import java.util.Objects;

public final class ObservedProfile {
    private final List<ArgumentValueHint> argumentValueHints;
    private final List<ObservedInvocation> observedInvocations;

    public ObservedProfile(
            List<ArgumentValueHint> argumentValueHints,
            List<ObservedInvocation> observedInvocations
    ) {
        this.argumentValueHints = List.copyOf(Objects.requireNonNull(
                argumentValueHints,
                "argumentValueHints must not be null"
        ));
        this.observedInvocations = List.copyOf(Objects.requireNonNull(
                observedInvocations,
                "observedInvocations must not be null"
        ));
    }

    public List<ArgumentValueHint> getArgumentValueHints() {
        return argumentValueHints;
    }

    public List<ObservedInvocation> getObservedInvocations() {
        return observedInvocations;
    }
}
