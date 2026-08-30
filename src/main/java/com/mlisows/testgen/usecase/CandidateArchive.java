package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.BranchId;
import com.mlisows.testgen.domain.TestCandidateExecutionResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class CandidateArchive {
    private final Set<String> coveredBranchIds = new HashSet<>();
    private final List<TestCandidateExecutionResult> selectedResults = new ArrayList<>();

    public boolean addIfImprovesCoverage(TestCandidateExecutionResult result) {
        Objects.requireNonNull(result, "result must not be null");

        List<String> newlyCoveredBranchIds = result.getCoveredBranches().stream()
                .map(BranchId::asString)
                .filter(branchId -> !coveredBranchIds.contains(branchId))
                .toList();

        if (newlyCoveredBranchIds.isEmpty()) {
            return false;
        }

        coveredBranchIds.addAll(newlyCoveredBranchIds);
        selectedResults.add(result);
        return true;
    }

    public List<TestCandidateExecutionResult> getSelectedResults() {
        return List.copyOf(selectedResults);
    }

    public Set<String> getCoveredBranchIds() {
        return Set.copyOf(coveredBranchIds);
    }
}
