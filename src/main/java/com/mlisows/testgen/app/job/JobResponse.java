package com.mlisows.testgen.app.job;

import com.mlisows.testgen.domain.generation.GenerationSettings;
import com.mlisows.testgen.usecase.generation.GenerationMetrics;
import com.mlisows.testgen.usecase.job.GenerationJob;
import com.mlisows.testgen.usecase.job.GenerationJobSummary;
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
        return from(GenerationJobSummary.from(job));
    }

    public static JobResponse from(GenerationJobSummary job) {
        return new JobResponse(job.id(), job.projectId(), job.profileId(), job.status(), job.settings(),
                job.createdAt(), job.startedAt(), job.finishedAt(), job.metrics(), publicError(job));
    }

    // Internal error details can contain server paths, so the API returns only a general message.
    private static String publicError(GenerationJobSummary job) {
        return switch (job.status()) {
            case FAILED -> "Generation failed. See server logs for job " + job.id();
            case TIMED_OUT -> "Generation exceeded the configured time limit.";
            case INTERRUPTED -> "Generation was interrupted.";
            default -> null;
        };
    }
}
