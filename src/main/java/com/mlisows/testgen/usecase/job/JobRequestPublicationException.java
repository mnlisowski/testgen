package com.mlisows.testgen.usecase.job;

import java.util.UUID;

/**
 * The job request was not confirmed by the messaging system. The job itself is already stored,
 * so it stays QUEUED and can be published again later.
 */
public class JobRequestPublicationException extends RuntimeException {
    private final UUID jobId;

    public JobRequestPublicationException(UUID jobId, Throwable cause) {
        super("Could not confirm publication of job request " + jobId, cause);
        this.jobId = jobId;
    }

    public UUID jobId() {
        return jobId;
    }
}
