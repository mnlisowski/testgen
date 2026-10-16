package com.mlisows.testgen.usecase.job;

import com.mlisows.testgen.domain.generation.GenerationSettings;
import com.mlisows.testgen.usecase.generation.GenerationMetrics;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Lightweight view of a job for lists. Unlike {@link GenerationJob}, it contains neither the
 * observed profile nor the report and test sources, so a store can read it without loading them.
 *
 * @param profileId  {@code null} when the job runs without a profile
 * @param startedAt  {@code null} until the job starts
 * @param finishedAt {@code null} until the job finishes
 * @param metrics    {@code null} unless the job succeeded
 * @param error      {@code null} unless the job ended with an error
 */
public record GenerationJobSummary(
        UUID id,
        String projectId,
        UUID profileId,
        JobStatus status,
        GenerationSettings settings,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt,
        GenerationMetrics metrics,
        String error
) {
    public GenerationJobSummary {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public static GenerationJobSummary from(GenerationJob job) {
        GenerationMetrics metrics = job.result().map(result -> result.metrics()).orElse(null);

        return new GenerationJobSummary(
                job.id(),
                job.projectId(),
                job.profileId().orElse(null),
                job.status(),
                job.request().settings(),
                job.createdAt(),
                job.startedAt().orElse(null),
                job.finishedAt().orElse(null),
                metrics,
                job.error().orElse(null)
        );
    }
}
