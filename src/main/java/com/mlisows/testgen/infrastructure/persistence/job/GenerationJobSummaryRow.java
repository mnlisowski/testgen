package com.mlisows.testgen.infrastructure.persistence.job;

import com.mlisows.testgen.domain.generation.GenerationSettings;
import com.mlisows.testgen.usecase.generation.GenerationMetrics;
import com.mlisows.testgen.usecase.job.GenerationJobSummary;
import com.mlisows.testgen.usecase.job.JobStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * One row of the job history query. Hibernate creates it directly from selected columns
 * (a JPQL constructor expression), without loading entities. Metric columns are null
 * for jobs without a result, because the result table is joined with LEFT JOIN.
 */
public record GenerationJobSummaryRow(
        UUID id,
        String projectId,
        UUID profileId,
        JobStatus status,
        long seed,
        int maxCandidates,
        int initialCandidates,
        int mutatedInitialCandidates,
        int maxValuesPerSlot,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt,
        String error,
        Integer analyzedClasses,
        Integer executedCandidates,
        Integer selectedCandidates,
        Integer coverageGoals,
        Integer coveredGoals
) {
    public GenerationJobSummary toSummary() {
        GenerationSettings settings = new GenerationSettings(
                seed, maxCandidates, initialCandidates, mutatedInitialCandidates, maxValuesPerSlot);

        GenerationMetrics metrics = null;
        if (analyzedClasses != null) {
            metrics = new GenerationMetrics(
                    analyzedClasses, executedCandidates, selectedCandidates, coverageGoals, coveredGoals);
        }

        return new GenerationJobSummary(id, projectId, profileId, status, settings, createdAt, startedAt,
                finishedAt, metrics, error);
    }
}
