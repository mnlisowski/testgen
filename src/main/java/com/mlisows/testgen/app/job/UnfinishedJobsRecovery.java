package com.mlisows.testgen.app.job;

import com.mlisows.testgen.usecase.job.JobStatus;
import com.mlisows.testgen.usecase.ports.GenerationJobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Handles jobs left unfinished by a previous server run. The execution queue lives in memory,
 * so after a restart nothing would execute these jobs or finish their status.
 *
 * <p>A RUNNING job was interrupted together with the server and is marked as INTERRUPTED.
 * A QUEUED job was accepted but never started, so it is submitted to the queue again.
 * The rule assumes a single server instance: another running instance could still be
 * executing a job that is RUNNING in the database.
 */
public class UnfinishedJobsRecovery implements ApplicationRunner {
    static final String RESTART_INTERRUPTION_MESSAGE = "Server stopped while the job was running";

    private static final Logger LOGGER = LoggerFactory.getLogger(UnfinishedJobsRecovery.class);

    private final GenerationJobStore jobStore;
    private final GenerationJobQueue jobQueue;

    public UnfinishedJobsRecovery(GenerationJobStore jobStore, GenerationJobQueue jobQueue) {
        this.jobStore = Objects.requireNonNull(jobStore, "jobStore");
        this.jobQueue = Objects.requireNonNull(jobQueue, "jobQueue");
    }

    @Override
    public void run(ApplicationArguments arguments) {
        interruptRunningJobs();
        resubmitQueuedJobs();
    }

    private void interruptRunningJobs() {
        List<UUID> runningJobIds = jobStore.findIdsByStatus(JobStatus.RUNNING);
        for (UUID jobId : runningJobIds) {
            jobStore.interrupt(jobId, RESTART_INTERRUPTION_MESSAGE);
        }
        if (!runningJobIds.isEmpty()) {
            LOGGER.warn("Marked {} jobs interrupted by the previous server stop as INTERRUPTED",
                    runningJobIds.size());
        }
    }

    private void resubmitQueuedJobs() {
        List<UUID> queuedJobIds = jobStore.findIdsByStatus(JobStatus.QUEUED);
        int resubmittedCount = 0;
        for (UUID jobId : queuedJobIds) {
            try {
                jobQueue.submit(jobId);
                resubmittedCount++;
            } catch (JobQueueFullException exception) {
                // The queue has already marked the job as FAILED; continue with the remaining jobs.
                LOGGER.warn("Could not resubmit queued job {} after restart: queue is full", jobId);
            }
        }
        if (!queuedJobIds.isEmpty()) {
            LOGGER.info("Resubmitted {} of {} queued jobs after restart", resubmittedCount, queuedJobIds.size());
        }
    }
}
