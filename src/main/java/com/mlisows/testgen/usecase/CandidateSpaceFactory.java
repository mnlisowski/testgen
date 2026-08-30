package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.CandidateSpace;
import com.mlisows.testgen.domain.CandidateSpace.CandidateValuePool;
import com.mlisows.testgen.domain.CandidateSpace.CandidateValueSlot;
import com.mlisows.testgen.domain.CandidateSpace.CandidateValueSlotKind;
import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.GeneratedArgument;
import com.mlisows.testgen.domain.GeneratedSetupObject;
import com.mlisows.testgen.domain.MethodModel;
import com.mlisows.testgen.domain.ParameterModel;
import com.mlisows.testgen.domain.ProjectClassStructureIndex;
import com.mlisows.testgen.domain.ProjectTypeIndex;
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
        Objects.requireNonNull(classStructure, "classStructure must not be null");
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(typeIndex, "typeIndex must not be null");
        Objects.requireNonNull(classIndex, "classIndex must not be null");

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
        valuePools.addAll(methodArgumentPools(classStructure, method, baseCandidate.get(), typeIndex));
        valuePools.addAll(setupObjectArgumentPools(baseCandidate.get(), classIndex, typeIndex));

        return Optional.of(new CandidateSpace(baseCandidate.get(), valuePools));
    }

    private List<CandidateValuePool> methodArgumentPools(
            ClassStructure classStructure,
            MethodModel method,
            TestCandidate baseCandidate,
            ProjectTypeIndex typeIndex
    ) {
        List<CandidateValuePool> pools = new ArrayList<>();
        String ownerId = classStructure.getClassName() + "." + method.getName();

        for (int index = 0; index < method.getParameters().size(); index++) {
            ParameterModel parameter = method.getParameters().get(index);
            GeneratedArgument baseValue = baseCandidate.getMethodArguments().get(index);

            pools.add(new CandidateValuePool(
                    new CandidateValueSlot(
                            CandidateValueSlotKind.METHOD_ARGUMENT,
                            ownerId,
                            parameter.getName(),
                            parameter.getType(),
                            index
                    ),
                    valuesFor(parameter.getType(), typeIndex, baseValue)
            ));
        }

        return pools;
    }

    private List<CandidateValuePool> setupObjectArgumentPools(
            TestCandidate baseCandidate,
            ProjectClassStructureIndex classIndex,
            ProjectTypeIndex typeIndex
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
                    typeIndex
            ));
        }

        return pools;
    }

    private List<CandidateValuePool> constructorArgumentPools(
            String setupClassName,
            List<ParameterModel> constructorParameters,
            List<GeneratedArgument> arguments,
            ProjectTypeIndex typeIndex
    ) {
        List<CandidateValuePool> pools = new ArrayList<>();
        String ownerId = setupClassName + ".<init>";

        for (int index = 0; index < constructorParameters.size(); index++) {
            ParameterModel parameter = constructorParameters.get(index);
            GeneratedArgument baseValue = arguments.get(index);

            pools.add(new CandidateValuePool(
                    new CandidateValueSlot(
                            CandidateValueSlotKind.SETUP_OBJECT_ARGUMENT,
                            ownerId,
                            parameter.getName(),
                            parameter.getType(),
                            index
                    ),
                    valuesFor(parameter.getType(), typeIndex, baseValue)
            ));
        }

        return pools;
    }

    private List<GeneratedArgument> valuesFor(
            String type,
            ProjectTypeIndex typeIndex,
            GeneratedArgument baseValue
    ) {
        List<GeneratedArgument> values = new ArrayList<>();
        addIfMissing(values, baseValue);

        for (String seedValue : seedValueGenerator.seedValuesFor(type)) {
            addIfMissing(values, new GeneratedArgument(type, seedValue));
        }

        typeIndex.findByName(type)
                .filter(typeInfo -> typeInfo.getKind() == TypeKind.ENUM)
                .ifPresent(typeInfo -> typeInfo.getEnumConstants()
                        .forEach(enumConstant -> addIfMissing(
                                values,
                                new GeneratedArgument(
                                        type,
                                        typeInfo.getFullyQualifiedName() + "." + enumConstant
                                )
                        )));

        return List.copyOf(values);
    }

    private void addIfMissing(List<GeneratedArgument> values, GeneratedArgument candidateValue) {
        boolean alreadyExists = values.stream()
                .anyMatch(value -> value.getType().equals(candidateValue.getType())
                        && value.getValue().equals(candidateValue.getValue()));

        if (!alreadyExists) {
            values.add(candidateValue);
        }
    }
}
