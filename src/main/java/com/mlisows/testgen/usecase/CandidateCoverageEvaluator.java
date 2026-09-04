package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.generation.TestCandidate;
import com.mlisows.testgen.domain.execution.TestCandidateExecutionResult;
import com.mlisows.testgen.usecase.ports.TestCandidateExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class CandidateCoverageEvaluator {
    private final TestCandidateExecutor candidateExecutor;

    public CandidateCoverageEvaluator(TestCandidateExecutor candidateExecutor) {
        this.candidateExecutor = Objects.requireNonNull(candidateExecutor, "candidateExecutor must not be null");
    }

    public List<TestCandidateExecutionResult> selectCoverageImprovingResults(List<TestCandidate> candidates) {
        return evaluate(candidates, Set.of(), 0).getSelectedResults();
    }

    public CandidateCoverageEvaluation evaluate(
            List<TestCandidate> candidates,
            Set<String> candidateSpaceMethodSignatures,
            int failedCandidateSpaceMethodCount
    ) {
        Objects.requireNonNull(candidates, "candidates must not be null");
        Objects.requireNonNull(candidateSpaceMethodSignatures, "candidateSpaceMethodSignatures must not be null");

        CandidateArchive archive = new CandidateArchive();
        List<TestCandidateExecutionResult> executedResults = new ArrayList<>();

        for (TestCandidate candidate : candidates) {
            TestCandidateExecutionResult result = candidateExecutor.execute(candidate);
            executedResults.add(result);
            archive.addIfImprovesCoverage(result);
        }

        return new CandidateCoverageEvaluation(
                executedResults,
                archive.getSelectedResults(),
                candidateSpaceMethodSignatures,
                failedCandidateSpaceMethodCount
        );
    }
}
