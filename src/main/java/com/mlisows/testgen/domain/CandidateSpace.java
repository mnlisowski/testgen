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

    public enum CandidateValueTier {
        EXACT,
        RELATED,
        FALLBACK
    }

    public static final class CandidateValueOption {
        private final GeneratedArgument argument;
        private final CandidateValueTier tier;
        private final String source;

        public CandidateValueOption(GeneratedArgument argument, CandidateValueTier tier, String source) {
            this.argument = Objects.requireNonNull(argument, "argument must not be null");
            this.tier = Objects.requireNonNull(tier, "tier must not be null");
            this.source = Objects.requireNonNull(source, "source must not be null");
        }

        public GeneratedArgument getArgument() {
            return argument;
        }

        public CandidateValueTier getTier() {
            return tier;
        }

        public String getSource() {
            return source;
        }
    }

    public static final class CandidateValueSlot {
        private final CandidateValueSlotKind kind;
        private final String ownerId;
        private final String argumentName;
        private final String type;
        private final int argumentIndex;

        public CandidateValueSlot(
                CandidateValueSlotKind kind,
                String ownerId,
                String argumentName,
                String type,
                int argumentIndex
        ) {
            this.kind = Objects.requireNonNull(kind, "kind must not be null");
            this.ownerId = Objects.requireNonNull(ownerId, "ownerId must not be null");
            this.argumentName = Objects.requireNonNull(argumentName, "argumentName must not be null");
            this.type = Objects.requireNonNull(type, "type must not be null");

            if (argumentIndex < 0) {
                throw new IllegalArgumentException("argumentIndex must not be negative");
            }

            this.argumentIndex = argumentIndex;
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

        public int getArgumentIndex() {
            return argumentIndex;
        }

        public String id() {
            return ownerId + "." + argumentName;
        }
    }

    public static final class CandidateValuePool {
        private final CandidateValueSlot slot;
        private final List<CandidateValueOption> options;

        public CandidateValuePool(CandidateValueSlot slot, List<CandidateValueOption> options) {
            this.slot = Objects.requireNonNull(slot, "slot must not be null");
            this.options = List.copyOf(Objects.requireNonNull(options, "options must not be null"));
        }

        public CandidateValueSlot getSlot() {
            return slot;
        }

        public List<CandidateValueOption> getOptions() {
            return options;
        }

        public List<GeneratedArgument> getValues() {
            return options.stream()
                    .map(CandidateValueOption::getArgument)
                    .toList();
        }
    }
}
