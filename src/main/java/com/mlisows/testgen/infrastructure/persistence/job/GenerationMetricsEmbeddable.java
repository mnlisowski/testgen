package com.mlisows.testgen.infrastructure.persistence.job;

import com.mlisows.testgen.usecase.generation.GenerationMetrics;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Generation metrics stored as columns of the generation_results table. */
@Embeddable
public class GenerationMetricsEmbeddable {
    @Column(name = "analyzed_classes", nullable = false)
    private int analyzedClasses;

    @Column(name = "executed_candidates", nullable = false)
    private int executedCandidates;

    @Column(name = "selected_candidates", nullable = false)
    private int selectedCandidates;

    @Column(name = "coverage_goals", nullable = false)
    private int coverageGoals;

    @Column(name = "covered_goals", nullable = false)
    private int coveredGoals;

    protected GenerationMetricsEmbeddable() {
    }

    public GenerationMetricsEmbeddable(GenerationMetrics metrics) {
        this.analyzedClasses = metrics.analyzedClasses();
        this.executedCandidates = metrics.executedCandidates();
        this.selectedCandidates = metrics.selectedCandidates();
        this.coverageGoals = metrics.coverageGoals();
        this.coveredGoals = metrics.coveredGoals();
    }

    public GenerationMetrics toMetrics() {
        return new GenerationMetrics(analyzedClasses, executedCandidates, selectedCandidates, coverageGoals,
                coveredGoals);
    }
}
