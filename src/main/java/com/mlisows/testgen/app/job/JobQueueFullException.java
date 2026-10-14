package com.mlisows.testgen.app.job;

import java.util.UUID;

public final class JobQueueFullException extends RuntimeException {
    private final UUID jobId;

    public JobQueueFullException(UUID jobId) {
        super("Generation queue is full or shutting down. Submit a new job later.");
        this.jobId = jobId;
    }

    public UUID jobId() {
        return jobId;
    }
}
