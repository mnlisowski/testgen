package com.mlisows.testgen.usecase.generation;

public record GenerationMetrics(
        int analyzedClasses,
        int executedCandidates,
        int selectedCandidates,
        int coverageGoals,
        int coveredGoals
) {
    public GenerationMetrics {
        if (analyzedClasses < 0 || executedCandidates < 0 || selectedCandidates < 0
                || coverageGoals < 0 || coveredGoals < 0) {
            throw new IllegalArgumentException("Generation metrics must not be negative");
        }
    }
}
