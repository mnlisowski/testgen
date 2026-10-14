package com.mlisows.testgen.usecase.job;

import com.mlisows.testgen.usecase.generation.GenerationRequest;
import com.mlisows.testgen.usecase.generation.GenerationResult;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class GenerationJob {
    private final UUID id;
    private final String projectId;
    private final GenerationRequest request;
    private final JobStatus status;
    private final Instant createdAt;
    private final Instant startedAt;
    private final Instant finishedAt;
    private final GenerationResult result;
    private final String error;

    private GenerationJob(UUID id, String projectId, GenerationRequest request, JobStatus status,
                          Instant createdAt, Instant startedAt, Instant finishedAt,
                          GenerationResult result, String error) {
        this.id = id;
        this.projectId = projectId;
        this.request = request;
        this.status = status;
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.result = result;
        this.error = error;
    }

    public static GenerationJob queued(UUID id, String projectId, GenerationRequest request, Instant now) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(projectId, "projectId");
        if (projectId.isBlank()) {
            throw new IllegalArgumentException("projectId must not be blank");
        }
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(now, "now");
        return new GenerationJob(id, projectId, request, JobStatus.QUEUED, now, null, null, null, null);
    }

    public GenerationJob start(Instant now) {
        requireStatus(JobStatus.QUEUED);
        return new GenerationJob(id, projectId, request, JobStatus.RUNNING, createdAt,
                Objects.requireNonNull(now, "now"), null, null, null);
    }

    public GenerationJob succeed(GenerationResult result, Instant now) {
        requireStatus(JobStatus.RUNNING);
        return finish(JobStatus.SUCCEEDED, Objects.requireNonNull(result, "result"), null, now);
    }

    /** A queued job can fail if submitting it to the executor is rejected. */
    public GenerationJob fail(String error, Instant now) {
        if (status != JobStatus.QUEUED && status != JobStatus.RUNNING) {
            throw new IllegalStateException("Cannot fail job " + id + " in status " + status);
        }
        return finish(JobStatus.FAILED, null, requireError(error), now);
    }

    public GenerationJob timeOut(String error, Instant now) {
        requireStatus(JobStatus.RUNNING);
        return finish(JobStatus.TIMED_OUT, null, requireError(error), now);
    }

    public GenerationJob interrupt(String error, Instant now) {
        requireStatus(JobStatus.RUNNING);
        return finish(JobStatus.INTERRUPTED, null, requireError(error), now);
    }

    private GenerationJob finish(JobStatus status, GenerationResult result, String error, Instant now) {
        return new GenerationJob(id, projectId, request, status, createdAt, startedAt,
                Objects.requireNonNull(now, "now"), result, error);
    }

    private static String requireError(String error) {
        Objects.requireNonNull(error, "error");
        if (error.isBlank()) {
            throw new IllegalArgumentException("error must not be blank");
        }
        return error;
    }

    private void requireStatus(JobStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Job " + id + " must be " + expected + ", was " + status);
        }
    }

    public UUID id() {
        return id;
    }

    public String projectId() {
        return projectId;
    }

    public GenerationRequest request() {
        return request;
    }

    public JobStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Optional<Instant> startedAt() {
        return Optional.ofNullable(startedAt);
    }

    public Optional<Instant> finishedAt() {
        return Optional.ofNullable(finishedAt);
    }

    public Optional<GenerationResult> result() {
        return Optional.ofNullable(result);
    }

    public Optional<String> error() {
        return Optional.ofNullable(error);
    }
}
