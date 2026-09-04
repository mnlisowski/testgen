package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.generation.ArgumentValueHint;
import com.mlisows.testgen.domain.generation.CandidateSpace;
import com.mlisows.testgen.domain.generation.MethodGenerationPlan;
import com.mlisows.testgen.domain.generation.TestCandidate;
import com.mlisows.testgen.domain.structure.ClassStructure;
import com.mlisows.testgen.domain.structure.ExecutableSignature;
import com.mlisows.testgen.domain.structure.MethodModel;
import com.mlisows.testgen.domain.structure.ProjectClassStructureIndex;
import com.mlisows.testgen.domain.structure.ProjectTypeIndex;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class EvaluateProjectCandidatesUseCase {
    private final MethodGenerationPlanner methodGenerationPlanner;
    private final CandidateSpaceFactory candidateSpaceFactory;
    private final CandidateVariantGenerator candidateVariantGenerator;
    private final CandidateCoverageEvaluator candidateCoverageEvaluator;

    public EvaluateProjectCandidatesUseCase(
            MethodGenerationPlanner methodGenerationPlanner,
            CandidateSpaceFactory candidateSpaceFactory,
            CandidateVariantGenerator candidateVariantGenerator,
            CandidateCoverageEvaluator candidateCoverageEvaluator
    ) {
        this.methodGenerationPlanner = Objects.requireNonNull(
                methodGenerationPlanner,
                "methodGenerationPlanner must not be null"
        );
        this.candidateSpaceFactory = Objects.requireNonNull(
                candidateSpaceFactory,
                "candidateSpaceFactory must not be null"
        );
        this.candidateVariantGenerator = Objects.requireNonNull(
                candidateVariantGenerator,
                "candidateVariantGenerator must not be null"
        );
        this.candidateCoverageEvaluator = Objects.requireNonNull(
                candidateCoverageEvaluator,
                "candidateCoverageEvaluator must not be null"
        );
    }

    public CandidateCoverageEvaluation evaluate(
            List<ClassStructure> classStructures,
            List<MethodGenerationPlan> methodPlans,
            ProjectTypeIndex typeIndex,
            ProjectClassStructureIndex classIndex,
            List<ArgumentValueHint> argumentValueHints
    ) {
        Objects.requireNonNull(classStructures, "classStructures must not be null");
        Objects.requireNonNull(methodPlans, "methodPlans must not be null");
        Objects.requireNonNull(typeIndex, "typeIndex must not be null");
        Objects.requireNonNull(classIndex, "classIndex must not be null");
        Objects.requireNonNull(argumentValueHints, "argumentValueHints must not be null");

        Set<String> supportedMethodIds = supportedMethodIds(methodPlans);
        Set<String> candidateSpaceMethodSignatures = new LinkedHashSet<>();
        List<TestCandidate> candidates = new ArrayList<>();
        int failedCandidateSpaceMethodCount = 0;

        for (ClassStructure classStructure : classStructures) {
            for (MethodModel method : classStructure.getMethods()) {
                if (!supportedMethodIds.contains(methodId(classStructure, method))) {
                    continue;
                }

                Optional<CandidateSpace> candidateSpace = candidateSpaceFactory.create(
                        classStructure,
                        method,
                        typeIndex,
                        classIndex,
                        argumentValueHints
                );

                if (candidateSpace.isEmpty()) {
                    failedCandidateSpaceMethodCount++;
                    continue;
                }

                candidateSpaceMethodSignatures.add(methodId(classStructure, method));
                candidates.addAll(candidateVariantGenerator.generateVariants(candidateSpace.get()));
            }
        }

        return candidateCoverageEvaluator.evaluate(
                candidates,
                candidateSpaceMethodSignatures,
                failedCandidateSpaceMethodCount
        );
    }

    private Set<String> supportedMethodIds(List<MethodGenerationPlan> methodPlans) {
        Set<String> methodIds = new LinkedHashSet<>();

        for (MethodGenerationPlan methodPlan : methodPlans) {
            if (methodGenerationPlanner.isSupported(methodPlan)) {
                methodIds.add(methodId(methodPlan));
            }
        }

        return methodIds;
    }

    private String methodId(ClassStructure classStructure, MethodModel method) {
        return ExecutableSignature.method(classStructure.getClassName(), method);
    }


    private String methodId(MethodGenerationPlan methodPlan) {
        return methodPlan.signature();

    }
}
