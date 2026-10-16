package com.mlisows.testgen.app.messaging;

import com.mlisows.testgen.app.job.GenerationJobExecutor;
import com.mlisows.testgen.infrastructure.messaging.JobRequestMessage;
import com.mlisows.testgen.usecase.job.GenerationJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;

/**
 * Receives job requests from Kafka and executes the requested jobs.
 *
 * <p>Spring Kafka commits the message offset after this method returns (ack mode RECORD), so a
 * request is confirmed only after its job has finished:
 * <ul>
 *   <li>on a normal server stop the executor interrupts the job, which ends as INTERRUPTED,
 *       and the request is confirmed;</li>
 *   <li>if the server process dies, the request stays unconfirmed and is delivered again after
 *       the restart; a job that is no longer QUEUED is then skipped.</li>
 * </ul>
 */
public class JobRequestListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobRequestListener.class);

    private final GenerationJobExecutor jobExecutor;

    public JobRequestListener(GenerationJobExecutor jobExecutor) {
        this.jobExecutor = Objects.requireNonNull(jobExecutor, "jobExecutor");
    }

    // The topic name comes from configuration; ${...} is resolved by Spring when the listener starts.
    // The consumer group is set by spring.kafka.consumer.group-id.
    @KafkaListener(topics = "${testgen.messaging.job-requests-topic}")
    public void onJobRequest(JobRequestMessage message) throws InterruptedException {
        LOGGER.info("Received request for job {} (event {}, schema version {})",
                message.jobId(), message.eventId(), message.schemaVersion());

        Optional<GenerationJob> finishedJob;
        try {
            finishedJob = jobExecutor.executeAndWait(message.jobId());
        } catch (RejectedExecutionException exception) {
            // The server is stopping and the job did not start. Rethrowing leaves the request
            // unconfirmed, so Kafka delivers it again after the next start.
            throw exception;
        } catch (RuntimeException exception) {
            // Not rethrown on purpose: the generator may already have run, and a redelivered
            // request must not start it again. Retry and dead-letter handling come later.
            LOGGER.error("Could not complete job {} from event {}", message.jobId(), message.eventId(), exception);
            return;
        }

        if (finishedJob.isEmpty()) {
            LOGGER.info("Skipped request for job {}: the job is no longer QUEUED", message.jobId());
            return;
        }
        GenerationJob job = finishedJob.get();
        LOGGER.info("Job {} finished with status {}", job.id(), job.status());
    }
}
