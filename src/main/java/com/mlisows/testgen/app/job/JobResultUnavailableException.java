package com.mlisows.testgen.app.job;

import com.mlisows.testgen.usecase.job.JobStatus;

import java.util.UUID;

public final class JobResultUnavailableException extends RuntimeException {
    public JobResultUnavailableException(UUID jobId, JobStatus status) {
        super("Result is unavailable for job " + jobId + " in status " + status);
    }
}
