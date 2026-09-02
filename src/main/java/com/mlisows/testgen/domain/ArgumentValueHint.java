package com.mlisows.testgen.domain;

import java.util.Objects;

public final class ArgumentValueHint {
    private final String ownerId;
    private final String argumentName;
    private final String type;
    private final String value;
    private final String source;

    public ArgumentValueHint(
            String ownerId,
            String argumentName,
            String type,
            String value,
            String source
    ) {
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId must not be null");
        this.argumentName = Objects.requireNonNull(argumentName, "argumentName must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.value = Objects.requireNonNull(value, "value must not be null");
        this.source = Objects.requireNonNull(source, "source must not be null");
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getArgumentName() {
        return argumentName;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public String getSource() {
        return source;
    }

    public String slotId() {
        return ownerId + "." + argumentName;
    }
}
