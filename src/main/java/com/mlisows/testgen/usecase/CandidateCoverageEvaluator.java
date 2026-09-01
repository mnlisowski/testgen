package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.TestCandidate;
import com.mlisows.testgen.domain.TestCandidateExecutionResult;
import com.mlisows.testgen.usecase.ports.TestCandidateExecutor;

import java.util.List;
import java.util.Objects;

public final class CandidateCoverageEvaluator {
    private final TestCandidateExecutor candidateExecutor;

    public CandidateCoverageEvaluator(TestCandidateExecutor candidateExecutor) {
        this.candidateExecutor = Objects.requireNonNull(candidateExecutor, "candidateExecutor must not be null");
    }

    public List<TestCandidateExecutionResult> selectCoverageImprovingResults(List<TestCandidate> candidates) {
        Objects.requireNonNull(candidates, "candidates must not be null");

        CandidateArchive archive = new CandidateArchive();

        for (TestCandidate candidate : candidates) {
            TestCandidateExecutionResult result = candidateExecutor.execute(candidate);
            archive.addIfImprovesCoverage(result);
        }

        return archive.getSelectedResults();
    }
}
