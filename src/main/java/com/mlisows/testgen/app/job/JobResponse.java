package com.mlisows.testgen.app.job;

import com.mlisows.testgen.domain.generation.GenerationSettings;
import com.mlisows.testgen.usecase.generation.GenerationMetrics;
import com.mlisows.testgen.usecase.job.GenerationJob;
import com.mlisows.testgen.usecase.job.JobStatus;

import java.time.Instant;
import java.util.UUID;

public record JobResponse(
        UUID id,
        String projectId,
        UUID profileId,
        JobStatus status,
        GenerationSettings settings,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt,
        GenerationMetrics summary,
        String error
) {
    public static JobResponse from(GenerationJob job) {
        GenerationMetrics summary = job.result().map(result -> result.metrics()).orElse(null);
        String publicError = switch (job.status()) {
            case FAILED -> "Generation failed. See server logs for job " + job.id();
            case TIMED_OUT -> "Generation exceeded the configured time limit.";
            case INTERRUPTED -> "Generation was interrupted.";
            default -> null;
        };
        return new JobResponse(job.id(), job.projectId(), job.profileId().orElse(null), job.status(), job.request().settings(),
                job.createdAt(), job.startedAt().orElse(null), job.finishedAt().orElse(null), summary, publicError);
    }
}
