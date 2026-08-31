package com.mlisows.testgen.infrastructure.execution;

import com.mlisows.testgen.domain.BranchId;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class RecordedBranchCoverageResolver {

    List<BranchId> resolve(Map<String, Integer> branchHits) {
        Objects.requireNonNull(branchHits, "branchHits must not be null");

        return branchHits.keySet().stream()
                .map(BranchId::parse)
                .sorted(Comparator.comparing(BranchId::asString))
                .toList();
    }
}
