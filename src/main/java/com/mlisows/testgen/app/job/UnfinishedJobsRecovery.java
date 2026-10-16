package com.mlisows.testgen.app.job;

import com.mlisows.testgen.usecase.job.JobStatus;
import com.mlisows.testgen.usecase.ports.GenerationJobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Marks jobs that were RUNNING when the previous server run stopped as INTERRUPTED.
 * Their generator process ended together with the server, so nothing would finish them.
 *
 * <p>QUEUED jobs are not touched: their unconfirmed Kafka requests are delivered again.
 *
 * <p>It runs as a lifecycle bean with a low phase, so it finishes before the Kafka listener starts.
 * Otherwise it could mark as interrupted a job that the listener has just started. The rule assumes
 * a single server instance: another running instance could still be executing a RUNNING job.
 */
public class UnfinishedJobsRecovery implements SmartLifecycle {
    static final String RESTART_INTERRUPTION_MESSAGE = "Server stopped while the job was running";

    // Lifecycle beans start in ascending phase order; Kafka listener containers use Integer.MAX_VALUE - 100.
    private static final int PHASE = 0;
    private static final Logger LOGGER = LoggerFactory.getLogger(UnfinishedJobsRecovery.class);

    private final GenerationJobStore jobStore;
    private volatile boolean running;

    public UnfinishedJobsRecovery(GenerationJobStore jobStore) {
        this.jobStore = Objects.requireNonNull(jobStore, "jobStore");
    }

    @Override
    public void start() {
        interruptRunningJobs();
        running = true;
    }

    /** Marks every RUNNING job as INTERRUPTED. Public so tests can repeat the startup step. */
    public void interruptRunningJobs() {
        List<UUID> runningJobIds = jobStore.findIdsByStatus(JobStatus.RUNNING);
        for (UUID jobId : runningJobIds) {
            jobStore.interrupt(jobId, RESTART_INTERRUPTION_MESSAGE);
        }
        if (!runningJobIds.isEmpty()) {
            LOGGER.warn("Marked {} jobs interrupted by the previous server stop as INTERRUPTED",
                    runningJobIds.size());
        }
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return PHASE;
    }
}
