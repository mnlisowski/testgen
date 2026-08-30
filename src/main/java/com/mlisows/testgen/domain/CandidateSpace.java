package com.mlisows.testgen.domain;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class CandidateSpace {
    private final TestCandidate baseCandidate;
    private final List<CandidateValuePool> valuePools;

    public CandidateSpace(TestCandidate baseCandidate, List<CandidateValuePool> valuePools) {
        this.baseCandidate = Objects.requireNonNull(baseCandidate, "baseCandidate must not be null");
        this.valuePools = List.copyOf(Objects.requireNonNull(valuePools, "valuePools must not be null"));
    }

    public TestCandidate getBaseCandidate() {
        return baseCandidate;
    }

    public List<CandidateValuePool> getValuePools() {
        return valuePools;
    }

    public Optional<CandidateValuePool> findPoolBySlotId(String slotId) {
        Objects.requireNonNull(slotId, "slotId must not be null");

        return valuePools.stream()
                .filter(pool -> pool.getSlot().id().equals(slotId))
                .findFirst();
    }

    public enum CandidateValueSlotKind {
        METHOD_ARGUMENT,
        SETUP_OBJECT_ARGUMENT
    }

    public static final class CandidateValueSlot {
        private final CandidateValueSlotKind kind;
        private final String ownerId;
        private final String argumentName;
        private final String type;

        public CandidateValueSlot(
                CandidateValueSlotKind kind,
                String ownerId,
                String argumentName,
                String type
        ) {
            this.kind = Objects.requireNonNull(kind, "kind must not be null");
            this.ownerId = Objects.requireNonNull(ownerId, "ownerId must not be null");
            this.argumentName = Objects.requireNonNull(argumentName, "argumentName must not be null");
            this.type = Objects.requireNonNull(type, "type must not be null");
        }

        public CandidateValueSlotKind getKind() {
            return kind;
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

        public String id() {
            return ownerId + "." + argumentName;
        }
    }

    public static final class CandidateValuePool {
        private final CandidateValueSlot slot;
        private final List<GeneratedArgument> values;

        public CandidateValuePool(CandidateValueSlot slot, List<GeneratedArgument> values) {
            this.slot = Objects.requireNonNull(slot, "slot must not be null");
            this.values = List.copyOf(Objects.requireNonNull(values, "values must not be null"));
        }

        public CandidateValueSlot getSlot() {
            return slot;
        }

        public List<GeneratedArgument> getValues() {
            return values;
        }
    }
}
