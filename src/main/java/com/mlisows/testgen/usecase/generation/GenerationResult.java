package com.mlisows.testgen.usecase.generation;

import com.mlisows.testgen.usecase.GenerationReport;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record GenerationResult(
        List<GeneratedTestFile> tests,
        String reportText,
        GenerationMetrics metrics,
        Set<String> coveredGoalIds
) {
    public GenerationResult {
        tests = List.copyOf(tests);
        Objects.requireNonNull(reportText, "reportText");
        Objects.requireNonNull(metrics, "metrics");
        coveredGoalIds = Set.copyOf(coveredGoalIds);
        if (metrics.coveredGoals() != coveredGoalIds.size()) {
            throw new IllegalArgumentException("Covered goal count does not match goal identifiers");
        }
    }

    public static GenerationResult from(List<GeneratedTestFile> tests, GenerationReport report) {
        GenerationMetrics metrics = new GenerationMetrics(
                report.getAnalyzedClassCount(), report.getExecutedCandidateCount(),
                report.getSelectedCandidateCount(), report.getCoverageGoalCount(), report.getCoveredBranchCount()
        );
        return new GenerationResult(tests, report.toText(), metrics, report.getCoveredBranchIds());
    }
}
