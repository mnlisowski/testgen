package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.CandidateSpace;
import com.mlisows.testgen.domain.CandidateSpace.CandidateValueOption;
import com.mlisows.testgen.domain.CandidateSpace.CandidateValuePool;
import com.mlisows.testgen.domain.CandidateSpace.CandidateValueSlot;
import com.mlisows.testgen.domain.CandidateSpace.CandidateValueSlotKind;
import com.mlisows.testgen.domain.CandidateSpace.CandidateValueTier;
import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.GeneratedArgument;
import com.mlisows.testgen.domain.GeneratedSetupObject;
import com.mlisows.testgen.domain.MethodModel;
import com.mlisows.testgen.domain.ParameterModel;
import com.mlisows.testgen.domain.ProjectClassStructureIndex;
import com.mlisows.testgen.domain.ProjectTypeIndex;
import com.mlisows.testgen.domain.ArgumentValueHint;
import com.mlisows.testgen.domain.TestCandidate;
import com.mlisows.testgen.domain.TypeKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class CandidateSpaceFactory {
    private final InitialTestCandidateFactory initialCandidateFactory;
    private final SeedValueGenerator seedValueGenerator;

    public CandidateSpaceFactory(
            InitialTestCandidateFactory initialCandidateFactory,
            SeedValueGenerator seedValueGenerator
    ) {
        this.initialCandidateFactory = Objects.requireNonNull(
                initialCandidateFactory,
                "initialCandidateFactory must not be null"
        );
        this.seedValueGenerator = Objects.requireNonNull(seedValueGenerator, "seedValueGenerator must not be null");
    }

    public CandidateSpaceFactory() {
        this(new InitialTestCandidateFactory(), new SeedValueGenerator());
    }

    public Optional<CandidateSpace> create(
            ClassStructure classStructure,
            MethodModel method,
            ProjectTypeIndex typeIndex,
            ProjectClassStructureIndex classIndex
    ) {
        return create(classStructure, method, typeIndex, classIndex, List.of());
    }

    public Optional<CandidateSpace> create(
            ClassStructure classStructure,
            MethodModel method,
            ProjectTypeIndex typeIndex,
            ProjectClassStructureIndex classIndex,
            List<ArgumentValueHint> argumentValueHints
    ) {
        Objects.requireNonNull(classStructure, "classStructure must not be null");
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(typeIndex, "typeIndex must not be null");
        Objects.requireNonNull(classIndex, "classIndex must not be null");
        Objects.requireNonNull(argumentValueHints, "argumentValueHints must not be null");

        Optional<TestCandidate> baseCandidate = initialCandidateFactory.create(
                classStructure,
                method,
                typeIndex,
                classIndex
        );

        if (baseCandidate.isEmpty()) {
            return Optional.empty();
        }

        List<CandidateValuePool> valuePools = new ArrayList<>();
        valuePools.addAll(methodArgumentPools(
                classStructure,
                method,
                baseCandidate.get(),
                typeIndex,
                classIndex,
                argumentValueHints
        ));
        valuePools.addAll(setupObjectArgumentPools(baseCandidate.get(), classIndex, typeIndex, argumentValueHints));

        return Optional.of(new CandidateSpace(baseCandidate.get(), valuePools));
    }

    private List<CandidateValuePool> methodArgumentPools(
            ClassStructure classStructure,
            MethodModel method,
            TestCandidate baseCandidate,
            ProjectTypeIndex typeIndex,
            ProjectClassStructureIndex classIndex,
            List<ArgumentValueHint> argumentValueHints
    ) {
        List<CandidateValuePool> pools = new ArrayList<>();
        String ownerId = classStructure.getClassName() + "." + method.getName();

        for (int index = 0; index < method.getParameters().size(); index++) {
            ParameterModel parameter = method.getParameters().get(index);
            GeneratedArgument baseValue = baseCandidate.getMethodArguments().get(index);

            CandidateValueSlot slot = new CandidateValueSlot(
                    CandidateValueSlotKind.METHOD_ARGUMENT,
                    ownerId,
                    parameter.getName(),
                    parameter.getType(),
                    index
            );

            pools.add(new CandidateValuePool(
                    slot,
                    optionsFor(parameter.getType(), typeIndex, classIndex, baseValue, argumentValueHints, slot.id())
            ));
        }

        return pools;
    }

    private List<CandidateValuePool> setupObjectArgumentPools(
            TestCandidate baseCandidate,
            ProjectClassStructureIndex classIndex,
            ProjectTypeIndex typeIndex,
            List<ArgumentValueHint> argumentValueHints
    ) {
        List<CandidateValuePool> pools = new ArrayList<>();

        for (GeneratedSetupObject setupObject : baseCandidate.getSetupObjects()) {
            List<ParameterModel> constructorParameters = setupObject.getConstructorParameters();

            if (constructorParameters.isEmpty()) {
                continue;
            }

            if (constructorParameters.size() != setupObject.getArguments().size()) {
                continue;
            }

            String setupClassName = classIndex.findByName(setupObject.getType())
                    .map(ClassStructure::getClassName)
                    .orElse(setupObject.getType());

            pools.addAll(constructorArgumentPools(
                    setupClassName,
                    constructorParameters,
                    setupObject.getArguments(),
                    classIndex,
                    typeIndex,
                    argumentValueHints
            ));
        }

        return pools;
    }

    private List<CandidateValuePool> constructorArgumentPools(
            String setupClassName,
            List<ParameterModel> constructorParameters,
            List<GeneratedArgument> arguments,
            ProjectClassStructureIndex classIndex,
            ProjectTypeIndex typeIndex,
            List<ArgumentValueHint> argumentValueHints
    ) {
        List<CandidateValuePool> pools = new ArrayList<>();
        String ownerId = setupClassName + ".<init>";

        for (int index = 0; index < constructorParameters.size(); index++) {
            ParameterModel parameter = constructorParameters.get(index);
            GeneratedArgument baseValue = arguments.get(index);

            CandidateValueSlot slot = new CandidateValueSlot(
                    CandidateValueSlotKind.SETUP_OBJECT_ARGUMENT,
                    ownerId,
                    parameter.getName(),
                    parameter.getType(),
                    index
            );

            pools.add(new CandidateValuePool(
                    slot,
                    optionsFor(parameter.getType(), typeIndex, classIndex, baseValue, argumentValueHints, slot.id())
            ));
        }

        return pools;
    }

    private List<CandidateValueOption> optionsFor(
            String type,
            ProjectTypeIndex typeIndex,
            ProjectClassStructureIndex classIndex,
            GeneratedArgument baseValue,
            List<ArgumentValueHint> argumentValueHints,
            String slotId
    ) {
        List<CandidateValueOption> options = new ArrayList<>();

        argumentValueHints.stream()
                .filter(hint -> hint.slotId().equals(slotId))
                .filter(hint -> hint.getType().equals(type))
                .forEach(hint -> addIfMissing(
                        options,
                        new GeneratedArgument(type, hint.getValue()),
                        CandidateValueTier.EXACT,
                        hint.getSource()
                ));

        addIfMissing(options, baseValue, CandidateValueTier.FALLBACK, "base-candidate");

        if (isNullableReferenceType(type, typeIndex, classIndex)) {
            addIfMissing(
                    options,
                    new GeneratedArgument(type, "null"),
                    CandidateValueTier.FALLBACK,
                    "null-reference"
            );
        }

        for (String seedValue : seedValueGenerator.seedValuesFor(type)) {
            addIfMissing(
                    options,
                    new GeneratedArgument(type, seedValue),
                    CandidateValueTier.FALLBACK,
                    "default-seed"
            );
        }

        typeIndex.findByName(type)
                .filter(typeInfo -> typeInfo.getKind() == TypeKind.ENUM)
                .ifPresent(typeInfo -> typeInfo.getEnumConstants()
                        .forEach(enumConstant -> addIfMissing(
                                options,
                                new GeneratedArgument(
                                        type,
                                        typeInfo.getFullyQualifiedName() + "." + enumConstant
                                ),
                                CandidateValueTier.FALLBACK,
                                "enum-constant"
                        )));

        return List.copyOf(options);
    }

    private boolean isNullableReferenceType(
            String type,
            ProjectTypeIndex typeIndex,
            ProjectClassStructureIndex classIndex
    ) {
        if (isPrimitiveType(type)) {
            return false;
        }

        if (type.equals("String") || type.equals("java.lang.String")) {
            return true;
        }

        if (classIndex.findByName(type).isPresent()) {
            return true;
        }

        return typeIndex.findByName(type)
                .map(typeInfo -> typeInfo.getKind() == TypeKind.CLASS
                        || typeInfo.getKind() == TypeKind.INTERFACE)
                .orElse(false);
    }

    private boolean isPrimitiveType(String type) {
        return type.equals("boolean")
                || type.equals("byte")
                || type.equals("short")
                || type.equals("int")
                || type.equals("long")
                || type.equals("float")
                || type.equals("double")
                || type.equals("char");
    }

    private void addIfMissing(
            List<CandidateValueOption> options,
            GeneratedArgument candidateValue,
            CandidateValueTier tier,
            String source
    ) {
        boolean alreadyExists = options.stream()
                .map(CandidateValueOption::getArgument)
                .anyMatch(value -> value.getType().equals(candidateValue.getType())
                        && value.getValue().equals(candidateValue.getValue()));

        if (!alreadyExists) {
            options.add(new CandidateValueOption(candidateValue, tier, source));
        }
    }
}
