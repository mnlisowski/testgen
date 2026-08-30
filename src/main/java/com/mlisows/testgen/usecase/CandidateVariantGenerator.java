package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.CandidateSpace;
import com.mlisows.testgen.domain.CandidateSpace.CandidateValuePool;
import com.mlisows.testgen.domain.CandidateSpace.CandidateValueSlot;
import com.mlisows.testgen.domain.CandidateSpace.CandidateValueSlotKind;
import com.mlisows.testgen.domain.GeneratedArgument;
import com.mlisows.testgen.domain.GeneratedSetupObject;
import com.mlisows.testgen.domain.TestCandidate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class CandidateVariantGenerator {
    private static final int DEFAULT_MAX_VALUES_PER_SLOT = 3;
    private static final int DEFAULT_MAX_CANDIDATES = 25;

    private final int maxValuesPerSlot;
    private final int maxCandidates;

    public CandidateVariantGenerator() {
        this(DEFAULT_MAX_VALUES_PER_SLOT, DEFAULT_MAX_CANDIDATES);
    }

    public CandidateVariantGenerator(int maxValuesPerSlot, int maxCandidates) {
        if (maxValuesPerSlot < 1) {
            throw new IllegalArgumentException("maxValuesPerSlot must be positive");
        }

        if (maxCandidates < 1) {
            throw new IllegalArgumentException("maxCandidates must be positive");
        }

        this.maxValuesPerSlot = maxValuesPerSlot;
        this.maxCandidates = maxCandidates;
    }

    public List<TestCandidate> generateVariants(CandidateSpace candidateSpace) {
        Objects.requireNonNull(candidateSpace, "candidateSpace must not be null");

        TestCandidate baseCandidate = candidateSpace.getBaseCandidate();
        List<TestCandidate> candidates = new ArrayList<>();
        candidates.add(baseCandidate);

        for (CandidateValuePool pool : candidateSpace.getValuePools()) {
            int generatedValuesForSlot = 0;

            for (GeneratedArgument value : pool.getValues()) {
                if (candidates.size() >= maxCandidates) {
                    return List.copyOf(candidates);
                }

                if (generatedValuesForSlot >= maxValuesPerSlot) {
                    break;
                }

                if (isBaseValue(baseCandidate, pool.getSlot(), value)) {
                    continue;
                }

                Optional<TestCandidate> variant = variantFor(baseCandidate, pool.getSlot(), value);

                if (variant.isPresent()) {
                    candidates.add(variant.get());
                    generatedValuesForSlot++;
                }
            }
        }

        return List.copyOf(candidates);
    }

    private Optional<TestCandidate> variantFor(
            TestCandidate baseCandidate,
            CandidateValueSlot slot,
            GeneratedArgument value
    ) {
        if (slot.getKind() == CandidateValueSlotKind.METHOD_ARGUMENT) {
            return withMethodArgument(baseCandidate, slot.getArgumentIndex(), value);
        }

        if (slot.getKind() == CandidateValueSlotKind.SETUP_OBJECT_ARGUMENT) {
            return withSetupObjectArgument(baseCandidate, slot, value);
        }

        return Optional.empty();
    }

    private Optional<TestCandidate> withMethodArgument(
            TestCandidate baseCandidate,
            int argumentIndex,
            GeneratedArgument value
    ) {
        if (argumentIndex >= baseCandidate.getMethodArguments().size()) {
            return Optional.empty();
        }

        List<GeneratedArgument> methodArguments = new ArrayList<>(baseCandidate.getMethodArguments());
        methodArguments.set(argumentIndex, value);

        return Optional.of(copyCandidate(
                baseCandidate,
                baseCandidate.getSetupObjects(),
                methodArguments
        ));
    }

    private Optional<TestCandidate> withSetupObjectArgument(
            TestCandidate baseCandidate,
            CandidateValueSlot slot,
            GeneratedArgument value
    ) {
        List<GeneratedSetupObject> setupObjects = new ArrayList<>(baseCandidate.getSetupObjects());

        for (int index = 0; index < setupObjects.size(); index++) {
            GeneratedSetupObject setupObject = setupObjects.get(index);

            if (!matchesSetupObject(setupObject, slot.getOwnerId())) {
                continue;
            }

            if (slot.getArgumentIndex() >= setupObject.getArguments().size()) {
                return Optional.empty();
            }

            List<GeneratedArgument> arguments = new ArrayList<>(setupObject.getArguments());
            arguments.set(slot.getArgumentIndex(), value);

            setupObjects.set(index, new GeneratedSetupObject(
                    setupObject.getType(),
                    setupObject.getVariableName(),
                    arguments,
                    setupObject.getConstructorParameters()
            ));

            return Optional.of(copyCandidate(
                    baseCandidate,
                    setupObjects,
                    baseCandidate.getMethodArguments()
            ));
        }

        return Optional.empty();
    }

    private boolean isBaseValue(
            TestCandidate baseCandidate,
            CandidateValueSlot slot,
            GeneratedArgument value
    ) {
        Optional<GeneratedArgument> baseValue = baseValueFor(baseCandidate, slot);

        return baseValue
                .map(argument -> argument.getType().equals(value.getType())
                        && argument.getValue().equals(value.getValue()))
                .orElse(false);
    }

    private Optional<GeneratedArgument> baseValueFor(TestCandidate baseCandidate, CandidateValueSlot slot) {
        if (slot.getKind() == CandidateValueSlotKind.METHOD_ARGUMENT) {
            if (slot.getArgumentIndex() >= baseCandidate.getMethodArguments().size()) {
                return Optional.empty();
            }

            return Optional.of(baseCandidate.getMethodArguments().get(slot.getArgumentIndex()));
        }

        for (GeneratedSetupObject setupObject : baseCandidate.getSetupObjects()) {
            if (!matchesSetupObject(setupObject, slot.getOwnerId())) {
                continue;
            }

            if (slot.getArgumentIndex() >= setupObject.getArguments().size()) {
                return Optional.empty();
            }

            return Optional.of(setupObject.getArguments().get(slot.getArgumentIndex()));
        }

        return Optional.empty();
    }

    private boolean matchesSetupObject(GeneratedSetupObject setupObject, String ownerId) {
        String ownerType = ownerId.replace(".<init>", "");

        return setupObject.getType().equals(ownerType)
                || setupObject.getType().equals(simpleName(ownerType));
    }

    private TestCandidate copyCandidate(
            TestCandidate baseCandidate,
            List<GeneratedSetupObject> setupObjects,
            List<GeneratedArgument> methodArguments
    ) {
        return new TestCandidate(
                baseCandidate.getClassName(),
                baseCandidate.getMethodName(),
                baseCandidate.getReturnType(),
                setupObjects,
                baseCandidate.getTargetVariableName(),
                methodArguments
        );
    }

    private String simpleName(String className) {
        int lastDotIndex = className.lastIndexOf('.');

        if (lastDotIndex == -1) {
            return className;
        }

        return className.substring(lastDotIndex + 1);
    }
}
