package com.mlisows.testgen.usecase.generation;

import com.mlisows.testgen.usecase.GenerationReport;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record GenerationResult(List<GeneratedTestFile> tests, GenerationReport report) {
    public GenerationResult {
        tests = List.copyOf(tests);
        Objects.requireNonNull(report, "report");
    }

    public Set<String> coveredGoalIds() {
        return report.getCoveredBranchIds();
    }
}
