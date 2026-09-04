package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.generation.CandidateSpace;
import com.mlisows.testgen.domain.generation.CandidateSpace.CandidateValueOption;
import com.mlisows.testgen.domain.generation.CandidateSpace.CandidateValuePool;
import com.mlisows.testgen.domain.generation.CandidateSpace.CandidateValueSlot;
import com.mlisows.testgen.domain.generation.CandidateSpace.CandidateValueSlotKind;
import com.mlisows.testgen.domain.generation.CandidateSpace.CandidateValueTier;
import com.mlisows.testgen.domain.generation.GeneratedArgument;
import com.mlisows.testgen.domain.generation.GeneratedSetupObject;
import com.mlisows.testgen.domain.generation.TestCandidate;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

public final class CandidateVariantGenerator {
    private static final int DEFAULT_INITIAL_CANDIDATE_COUNT = 10;
    private static final int DEFAULT_MAX_VALUES_PER_SLOT = 3;
    private static final int DEFAULT_MAX_CANDIDATES = 25;

    private final int initialCandidateCount;
    private final int maxValuesPerSlot;
    private final int maxCandidates;
    private final Random random;

    public CandidateVariantGenerator() {
        this(DEFAULT_INITIAL_CANDIDATE_COUNT, DEFAULT_MAX_VALUES_PER_SLOT, DEFAULT_MAX_CANDIDATES);
    }

    public CandidateVariantGenerator(int maxValuesPerSlot, int maxCandidates) {
        this(DEFAULT_INITIAL_CANDIDATE_COUNT, maxValuesPerSlot, maxCandidates);
    }

    public CandidateVariantGenerator(int initialCandidateCount, int maxValuesPerSlot, int maxCandidates) {
        if (initialCandidateCount < 0) {
            throw new IllegalArgumentException("initialCandidateCount must not be negative");
        }

        if (maxValuesPerSlot < 1) {
            throw new IllegalArgumentException("maxValuesPerSlot must be positive");
        }

        if (maxCandidates < 1) {
            throw new IllegalArgumentException("maxCandidates must be positive");
        }

        this.initialCandidateCount = initialCandidateCount;
        this.maxValuesPerSlot = maxValuesPerSlot;
        this.maxCandidates = maxCandidates;
        this.random = new Random(0);
    }

    public List<TestCandidate> generateVariants(CandidateSpace space) {
        Objects.requireNonNull(space, "space must not be null");

        UniqueCandidateList candidates = new UniqueCandidateList(maxCandidates);

        candidates.add(space.getBaseCandidate());
        addRandomInitialCandidates(space, candidates);
        addSingleSlotMutations(space, candidates);
        addTwoSlotMutations(space, candidates);

        return candidates.toList();
    }

    private void addRandomInitialCandidates(CandidateSpace space, UniqueCandidateList candidates) {
        for (int index = 0; index < initialCandidateCount; index++) {
            if (candidates.isFull()) {
                return;
            }

            candidates.add(buildRandomInitialCandidate(space));
        }
    }

    private TestCandidate buildRandomInitialCandidate(CandidateSpace space) {
        TestCandidate candidate = space.getBaseCandidate();

        for (CandidateValuePool pool : space.getValuePools()) {
            Optional<GeneratedArgument> value = bestRandomValue(pool);

            if (value.isEmpty()) {
                continue;
            }

            candidate = replaceSlotValue(candidate, pool.getSlot(), value.get())
                    .orElse(candidate);
        }

        return candidate;
    }

    private Optional<GeneratedArgument> bestRandomValue(CandidateValuePool pool) {
        Optional<GeneratedArgument> exact = randomValue(pool, CandidateValueTier.EXACT, false);
        if (exact.isPresent()) {
            return exact;
        }

        Optional<GeneratedArgument> related = randomValue(pool, CandidateValueTier.RELATED, false);
        if (related.isPresent()) {
            return related;
        }

        Optional<GeneratedArgument> fallback = randomValue(pool, CandidateValueTier.FALLBACK, false);
        if (fallback.isPresent()) {
            return fallback;
        }

        return randomValue(pool, CandidateValueTier.NULL, true);
    }

    private Optional<GeneratedArgument> randomValue(
            CandidateValuePool pool,
            CandidateValueTier tier,
            boolean includeNull
    ) {
        List<CandidateValueOption> options = optionsForTier(pool, tier, includeNull);

        if (options.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(options.get(random.nextInt(options.size())).getArgument());
    }

    private List<CandidateValueOption> optionsForTier(
            CandidateValuePool pool,
            CandidateValueTier tier,
            boolean includeNull
    ) {
        return pool.getOptions().stream()
                .filter(option -> option.getTier() == tier)
                .filter(option -> includeNull || !isNullValue(option))
                .toList();
    }

    private void addSingleSlotMutations(CandidateSpace space, UniqueCandidateList candidates) {
        TestCandidate baseCandidate = space.getBaseCandidate();

        for (CandidateValuePool pool : space.getValuePools()) {
            int addedForSlot = 0;

            for (GeneratedArgument value : pool.getValues()) {
                if (candidates.isFull() || addedForSlot >= maxValuesPerSlot) {
                    break;
                }

                if (candidateAlreadyHasValue(baseCandidate, pool.getSlot(), value)) {
                    continue;
                }

                Optional<TestCandidate> mutation = replaceSlotValue(baseCandidate, pool.getSlot(), value);

                if (mutation.isPresent() && candidates.add(mutation.get())) {
                    addedForSlot++;
                }
            }

            if (candidates.isFull()) {
                return;
            }
        }
    }

    private void addTwoSlotMutations(CandidateSpace space, UniqueCandidateList candidates) {
        TestCandidate baseCandidate = space.getBaseCandidate();
        List<CandidateValuePool> pools = space.getValuePools();

        for (int firstIndex = 0; firstIndex < pools.size() - 1; firstIndex++) {
            CandidateValuePool firstPool = pools.get(firstIndex);

            for (int secondIndex = firstIndex + 1; secondIndex < pools.size(); secondIndex++) {
                CandidateValuePool secondPool = pools.get(secondIndex);
                addTwoSlotMutations(baseCandidate, firstPool, secondPool, candidates);

                if (candidates.isFull()) {
                    return;
                }
            }
        }
    }

    private void addTwoSlotMutations(
            TestCandidate baseCandidate,
            CandidateValuePool firstPool,
            CandidateValuePool secondPool,
            UniqueCandidateList candidates
    ) {
        for (GeneratedArgument firstValue : pairMutationValues(firstPool)) {
            if (candidateAlreadyHasValue(baseCandidate, firstPool.getSlot(), firstValue)) {
                continue;
            }

            Optional<TestCandidate> firstMutation = replaceSlotValue(baseCandidate, firstPool.getSlot(), firstValue);

            if (firstMutation.isEmpty()) {
                continue;
            }

            for (GeneratedArgument secondValue : pairMutationValues(secondPool)) {
                if (candidates.isFull()) {
                    return;
                }

                if (candidateAlreadyHasValue(baseCandidate, secondPool.getSlot(), secondValue)) {
                    continue;
                }

                replaceSlotValue(firstMutation.get(), secondPool.getSlot(), secondValue)
                        .ifPresent(candidates::add);
            }
        }
    }

    private List<GeneratedArgument> pairMutationValues(CandidateValuePool pool) {
        return pool.getOptions().stream()
                .filter(option -> option.getTier() != CandidateValueTier.NULL)
                .filter(option -> !isNullValue(option))
                .map(CandidateValueOption::getArgument)
                .limit(maxValuesPerSlot)
                .toList();
    }

    private Optional<TestCandidate> replaceSlotValue(
            TestCandidate candidate,
            CandidateValueSlot slot,
            GeneratedArgument value
    ) {
        if (slot.getKind() == CandidateValueSlotKind.METHOD_ARGUMENT) {
            return replaceMethodArgument(candidate, slot.getArgumentIndex(), value);
        }

        if (slot.getKind() == CandidateValueSlotKind.SETUP_OBJECT_ARGUMENT) {
            return replaceSetupObjectArgument(candidate, slot, value);
        }

        return Optional.empty();
    }

    private Optional<TestCandidate> replaceMethodArgument(
            TestCandidate candidate,
            int argumentIndex,
            GeneratedArgument value
    ) {
        if (argumentIndex >= candidate.getMethodArguments().size()) {
            return Optional.empty();
        }

        List<GeneratedArgument> methodArguments = new ArrayList<>(candidate.getMethodArguments());
        methodArguments.set(argumentIndex, value);

        return Optional.of(copyCandidate(candidate, candidate.getSetupObjects(), methodArguments));
    }

    private Optional<TestCandidate> replaceSetupObjectArgument(
            TestCandidate candidate,
            CandidateValueSlot slot,
            GeneratedArgument value
    ) {
        List<GeneratedSetupObject> setupObjects = new ArrayList<>(candidate.getSetupObjects());

        for (int index = 0; index < setupObjects.size(); index++) {
            GeneratedSetupObject setupObject = setupObjects.get(index);

            if (!slotMatchesSetupObject(slot, setupObject)) {
                continue;
            }

            if (slot.getArgumentIndex() >= setupObject.getArguments().size()) {
                return Optional.empty();
            }

            setupObjects.set(index, setupObjectWithArgument(setupObject, slot.getArgumentIndex(), value));
            return Optional.of(copyCandidate(candidate, setupObjects, candidate.getMethodArguments()));
        }

        return Optional.empty();
    }

    private GeneratedSetupObject setupObjectWithArgument(
            GeneratedSetupObject setupObject,
            int argumentIndex,
            GeneratedArgument value
    ) {
        List<GeneratedArgument> arguments = new ArrayList<>(setupObject.getArguments());
        arguments.set(argumentIndex, value);

        return new GeneratedSetupObject(
                setupObject.getType(),
                setupObject.getVariableName(),
                arguments,
                setupObject.getConstructorParameters()
        );
    }

    private boolean candidateAlreadyHasValue(
            TestCandidate candidate,
            CandidateValueSlot slot,
            GeneratedArgument value
    ) {
        return currentSlotValue(candidate, slot)
                .map(currentValue -> sameArgument(currentValue, value))
                .orElse(false);
    }

    private Optional<GeneratedArgument> currentSlotValue(TestCandidate candidate, CandidateValueSlot slot) {
        if (slot.getKind() == CandidateValueSlotKind.METHOD_ARGUMENT) {
            return methodArgument(candidate, slot.getArgumentIndex());
        }

        return setupObjectArgument(candidate, slot);
    }

    private Optional<GeneratedArgument> methodArgument(TestCandidate candidate, int argumentIndex) {
        if (argumentIndex >= candidate.getMethodArguments().size()) {
            return Optional.empty();
        }

        return Optional.of(candidate.getMethodArguments().get(argumentIndex));
    }

    private Optional<GeneratedArgument> setupObjectArgument(TestCandidate candidate, CandidateValueSlot slot) {
        for (GeneratedSetupObject setupObject : candidate.getSetupObjects()) {
            if (!slotMatchesSetupObject(slot, setupObject)) {
                continue;
            }

            if (slot.getArgumentIndex() >= setupObject.getArguments().size()) {
                return Optional.empty();
            }

            return Optional.of(setupObject.getArguments().get(slot.getArgumentIndex()));
        }

        return Optional.empty();
    }

    private boolean slotMatchesSetupObject(CandidateValueSlot slot, GeneratedSetupObject setupObject) {
        String ownerType = constructorOwnerType(slot.getOwnerId());

        return setupObject.getType().equals(ownerType)
                || setupObject.getType().equals(simpleName(ownerType));
    }

    private String constructorOwnerType(String ownerId) {
        int constructorIndex = ownerId.indexOf(".<init>");

        if (constructorIndex == -1) {
            return ownerId;
        }

        return ownerId.substring(0, constructorIndex);
    }

    private boolean sameArgument(GeneratedArgument first, GeneratedArgument second) {
        return first.getType().equals(second.getType())
                && first.getValue().equals(second.getValue());
    }

    private boolean isNullValue(CandidateValueOption option) {
        return option.getArgument().getValue().equals("null");
    }

    private TestCandidate copyCandidate(
            TestCandidate candidate,
            List<GeneratedSetupObject> setupObjects,
            List<GeneratedArgument> methodArguments
    ) {
        return new TestCandidate(
                candidate.getClassName(),
                candidate.getMethodName(),
                candidate.getReturnType(),
                candidate.getMethodParameterTypes(),
                setupObjects,
                candidate.getTargetVariableName(),
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

    private static final class UniqueCandidateList {
        private final int maxSize;
        private final List<TestCandidate> candidates = new ArrayList<>();
        private final Set<String> seenCandidateKeys = new LinkedHashSet<>();

        private UniqueCandidateList(int maxSize) {
            this.maxSize = maxSize;
        }

        private boolean add(TestCandidate candidate) {
            if (isFull()) {
                return false;
            }

            if (!seenCandidateKeys.add(candidateKey(candidate))) {
                return false;
            }

            candidates.add(candidate);
            return true;
        }

        private boolean isFull() {
            return candidates.size() >= maxSize;
        }

        private List<TestCandidate> toList() {
            return List.copyOf(candidates);
        }

        private static String candidateKey(TestCandidate candidate) {
            StringBuilder key = new StringBuilder();
            key.append(candidate.getClassName())
                    .append('#')
                    .append(candidate.getMethodName())
                    .append('#')
                    .append(candidate.getTargetVariableName());

            for (GeneratedSetupObject setupObject : candidate.getSetupObjects()) {
                key.append("|setup:")
                        .append(setupObject.getType())
                        .append(':')
                        .append(setupObject.getVariableName());

                appendArguments(key, setupObject.getArguments());
            }

            key.append("|method");
            appendArguments(key, candidate.getMethodArguments());

            return key.toString();
        }

        private static void appendArguments(StringBuilder key, List<GeneratedArgument> arguments) {
            for (GeneratedArgument argument : arguments) {
                key.append(':')
                        .append(argument.getType())
                        .append('=')
                        .append(argument.getValue());
            }
        }
    }
}
