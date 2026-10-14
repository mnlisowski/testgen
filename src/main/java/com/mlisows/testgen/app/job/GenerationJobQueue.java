package com.mlisows.testgen.app.job;

import com.mlisows.testgen.usecase.job.ExecuteGenerationJobUseCase;
import com.mlisows.testgen.usecase.job.GenerationJob;
import com.mlisows.testgen.usecase.ports.GenerationJobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class GenerationJobQueue implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerationJobQueue.class);

    private final GenerationJobStore jobStore;
    private final ExecuteGenerationJobUseCase executeGenerationJob;
    private final ThreadPoolExecutor executor;

    public GenerationJobQueue(GenerationJobStore jobStore, ExecuteGenerationJobUseCase executeGenerationJob,
                              int queueCapacity) {
        this.jobStore = jobStore;
        this.executeGenerationJob = executeGenerationJob;
        this.executor = new ThreadPoolExecutor(
                1, 1, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                Thread.ofPlatform().name("generation-worker-", 0).factory(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    public void submit(UUID jobId) {
        try {
            executor.execute(new JobTask(jobId));
        } catch (RejectedExecutionException exception) {
            jobStore.fail(jobId, "Generation queue is full or shutting down");
            throw new JobQueueFullException(jobId);
        }
    }

    @Override
    public void close() {
        List<Runnable> waitingTasks = executor.shutdownNow();
        for (Runnable waitingTask : waitingTasks) {
            JobTask jobTask = (JobTask) waitingTask;
            jobStore.fail(jobTask.jobId, "Server stopped before execution started");
        }
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                LOGGER.error("Generation worker did not stop within the shutdown timeout");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private final class JobTask implements Runnable {
        private final UUID jobId;

        private JobTask(UUID jobId) {
            this.jobId = jobId;
        }

        @Override
        public void run() {
            try {
                GenerationJob completedJob = executeGenerationJob.execute(jobId);
                completedJob.error().ifPresent(error ->
                        LOGGER.warn("Generation job {} ended as {}: {}", jobId, completedJob.status(), error));
            } catch (RuntimeException exception) {
                // In particular, do not rerun the generator after a result-persistence failure.
                LOGGER.error("Could not complete generation job {}", jobId, exception);
            }
        }
    }
}
