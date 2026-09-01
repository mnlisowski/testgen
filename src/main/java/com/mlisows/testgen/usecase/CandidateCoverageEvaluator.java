package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.TestCandidate;
import com.mlisows.testgen.domain.TestCandidateExecutionResult;
import com.mlisows.testgen.usecase.ports.TestCandidateExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CandidateCoverageEvaluator {
    private final TestCandidateExecutor candidateExecutor;

    public CandidateCoverageEvaluator(TestCandidateExecutor candidateExecutor) {
        this.candidateExecutor = Objects.requireNonNull(candidateExecutor, "candidateExecutor must not be null");
    }

    public List<TestCandidateExecutionResult> selectCoverageImprovingResults(List<TestCandidate> candidates) {
        return evaluate(candidates).getSelectedResults();
    }

    public CandidateCoverageEvaluation evaluate(List<TestCandidate> candidates) {
        Objects.requireNonNull(candidates, "candidates must not be null");

        CandidateArchive archive = new CandidateArchive();
        List<TestCandidateExecutionResult> executedResults = new ArrayList<>();

        for (TestCandidate candidate : candidates) {
            TestCandidateExecutionResult result = candidateExecutor.execute(candidate);
            executedResults.add(result);
            archive.addIfImprovesCoverage(result);
        }

        return new CandidateCoverageEvaluation(executedResults, archive.getSelectedResults());
    }
}
