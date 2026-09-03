package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.ArgumentValueHint;
import com.mlisows.testgen.domain.CandidateSpace;
import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.MethodGenerationPlan;
import com.mlisows.testgen.domain.MethodModel;
import com.mlisows.testgen.domain.ProjectClassStructureIndex;
import com.mlisows.testgen.domain.ProjectTypeIndex;
import com.mlisows.testgen.domain.TestCandidate;

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
        List<TestCandidate> candidates = new ArrayList<>();

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
                    continue;
                }

                candidates.addAll(candidateVariantGenerator.generateVariants(candidateSpace.get()));
            }
        }

        return candidateCoverageEvaluator.evaluate(candidates);
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
        return classStructure.getClassName() + "." + method.getName();
    }

    private String methodId(MethodGenerationPlan methodPlan) {
        return methodPlan.getClassName() + "." + methodPlan.getMethodName();
    }
}
