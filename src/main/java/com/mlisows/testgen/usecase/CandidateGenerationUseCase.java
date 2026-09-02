package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.CandidateSpace;
import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.MethodModel;
import com.mlisows.testgen.domain.ProjectClassStructureIndex;
import com.mlisows.testgen.domain.ProjectTypeIndex;
import com.mlisows.testgen.domain.ArgumentValueHint;
import com.mlisows.testgen.domain.TestCandidate;
import com.mlisows.testgen.domain.TestCandidateExecutionResult;

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
            List<ArgumentValueHint> staticHints
    ) {
        return evaluate(classStructure, method, typeIndex, classIndex, staticHints).getSelectedResults();
    }

    public CandidateCoverageEvaluation evaluate(
            ClassStructure classStructure,
            MethodModel method,
            ProjectTypeIndex typeIndex,
            ProjectClassStructureIndex classIndex,
            List<ArgumentValueHint> staticHints
    ) {
        Objects.requireNonNull(classStructure, "classStructure must not be null");
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(typeIndex, "typeIndex must not be null");
        Objects.requireNonNull(classIndex, "classIndex must not be null");
        Objects.requireNonNull(staticHints, "staticHints must not be null");

        Optional<CandidateSpace> candidateSpace = candidateSpaceFactory.create(
                classStructure,
                method,
                typeIndex,
                classIndex,
                staticHints
        );

        if (candidateSpace.isEmpty()) {
            return CandidateCoverageEvaluation.empty();
        }

        List<TestCandidate> candidateVariants = candidateVariantGenerator.generateVariants(candidateSpace.get());

        return candidateCoverageEvaluator.evaluate(candidateVariants);
    }
}
