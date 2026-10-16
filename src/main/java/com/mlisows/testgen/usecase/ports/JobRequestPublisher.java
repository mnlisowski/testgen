package com.mlisows.testgen.usecase.ports;

import java.util.UUID;

/**
 * Informs workers that a stored job is waiting to be executed. The message carries only the job id;
 * a worker reads everything else from the job store.
 */
public interface JobRequestPublisher {
    /**
     * Sends a request for the given job and waits until the messaging system confirms it.
     *
     * @throws com.mlisows.testgen.usecase.job.JobRequestPublicationException if the confirmation
     *         does not arrive; the request may still have been delivered
     */
    void publish(UUID jobId);
}
