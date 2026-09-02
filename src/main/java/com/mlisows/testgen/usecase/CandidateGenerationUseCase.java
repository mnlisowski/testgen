package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.CandidateSpace;
import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.MethodGenerationPlan;
import com.mlisows.testgen.domain.MethodModel;
import com.mlisows.testgen.domain.ProjectClassStructureIndex;
import com.mlisows.testgen.domain.ProjectTypeIndex;
import com.mlisows.testgen.domain.ArgumentValueHint;
import com.mlisows.testgen.domain.TestCandidate;
import com.mlisows.testgen.domain.TestCandidateExecutionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class CandidateGenerationUseCase {
    private final CandidateSpaceFactory candidateSpaceFactory;
    private final CandidateVariantGenerator candidateVariantGenerator;
    private final CandidateCoverageEvaluator candidateCoverageEvaluator;

    public CandidateGenerationUseCase(
            CandidateSpaceFactory candidateSpaceFactory,
            CandidateVariantGenerator candidateVariantGenerator,
            CandidateCoverageEvaluator candidateCoverageEvaluator
    ) {
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

    public List<TestCandidateExecutionResult> execute(
            ClassStructure classStructure,
            MethodModel method,
            ProjectTypeIndex typeIndex,
            ProjectClassStructureIndex classIndex,
            List<ArgumentValueHint> argumentValueHints
    ) {
        return evaluate(classStructure, method, typeIndex, classIndex, argumentValueHints).getSelectedResults();
    }

    public CandidateCoverageEvaluation evaluateProject(
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

        MethodGenerationPlanner planner = new MethodGenerationPlanner();
        List<TestCandidateExecutionResult> executedResults = new ArrayList<>();
        List<TestCandidateExecutionResult> selectedResults = new ArrayList<>();

        for (ClassStructure classStructure : classStructures) {
            for (MethodModel method : classStructure.getMethods()) {
                Optional<MethodGenerationPlan> plan = methodPlan(
                        methodPlans,
                        classStructure.getClassName(),
                        method.getName()
                );

                if (plan.isEmpty() || !planner.isSupported(plan.get())) {
                    continue;
                }

                CandidateCoverageEvaluation evaluation = evaluate(
                        classStructure,
                        method,
                        typeIndex,
                        classIndex,
                        argumentValueHints
                );

                executedResults.addAll(evaluation.getExecutedResults());
                selectedResults.addAll(evaluation.getSelectedResults());
            }
        }

        return new CandidateCoverageEvaluation(executedResults, selectedResults);
    }

    public CandidateCoverageEvaluation evaluate(
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

        Optional<CandidateSpace> candidateSpace = candidateSpaceFactory.create(
                classStructure,
                method,
                typeIndex,
                classIndex,
                argumentValueHints
        );

        if (candidateSpace.isEmpty()) {
            return CandidateCoverageEvaluation.empty();
        }

        List<TestCandidate> candidateVariants = candidateVariantGenerator.generateVariants(candidateSpace.get());

        return candidateCoverageEvaluator.evaluate(candidateVariants);
    }

    private Optional<MethodGenerationPlan> methodPlan(
            List<MethodGenerationPlan> methodPlans,
            String className,
            String methodName
    ) {
        return methodPlans.stream()
                .filter(plan -> plan.getClassName().equals(className))
                .filter(plan -> plan.getMethodName().equals(methodName))
                .findFirst();
    }
}
