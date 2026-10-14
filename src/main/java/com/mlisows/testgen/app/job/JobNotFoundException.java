package com.mlisows.testgen.app.job;

import java.util.UUID;

public final class JobNotFoundException extends RuntimeException {
    public JobNotFoundException(UUID jobId) {
        super("Job not found: " + jobId);
    }
}
