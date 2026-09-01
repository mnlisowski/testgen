package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.ExecutionOutcome;
import com.mlisows.testgen.domain.TestCandidateExecutionResult;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class CandidateCoverageEvaluation {
    private final List<TestCandidateExecutionResult> executedResults;
    private final List<TestCandidateExecutionResult> selectedResults;

    public CandidateCoverageEvaluation(
            List<TestCandidateExecutionResult> executedResults,
            List<TestCandidateExecutionResult> selectedResults
    ) {
        this.executedResults = List.copyOf(Objects.requireNonNull(
                executedResults,
                "executedResults must not be null"
        ));
        this.selectedResults = List.copyOf(Objects.requireNonNull(
                selectedResults,
                "selectedResults must not be null"
        ));
    }

    public static CandidateCoverageEvaluation empty() {
        return new CandidateCoverageEvaluation(List.of(), List.of());
    }

    public List<TestCandidateExecutionResult> getExecutedResults() {
        return executedResults;
    }

    public List<TestCandidateExecutionResult> getSelectedResults() {
        return selectedResults;
    }

    public int getExecutedCandidateCount() {
        return executedResults.size();
    }

    public int getSelectedCandidateCount() {
        return selectedResults.size();
    }

    public long countExecutedByOutcome(ExecutionOutcome outcome) {
        Objects.requireNonNull(outcome, "outcome must not be null");

        return executedResults.stream()
                .filter(result -> result.getOutcome() == outcome)
                .count();
    }

    public Set<String> getCoveredBranchIds() {
        Set<String> coveredBranchIds = new LinkedHashSet<>();

        selectedResults.stream()
                .flatMap(result -> result.getCoveredBranches().stream())
                .map(branchId -> branchId.asString())
                .forEach(coveredBranchIds::add);

        return Set.copyOf(coveredBranchIds);
    }
}
