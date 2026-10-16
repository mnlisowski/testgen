package com.mlisows.testgen.app.job;

import com.mlisows.testgen.usecase.job.ExecuteGenerationJobUseCase;
import com.mlisows.testgen.usecase.job.GenerationJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Runs one generation job at a time on its own thread, while the caller (the Kafka listener)
 * waits for the result.
 *
 * <p>The separate thread exists for shutdown: the Kafka listener thread is managed by Spring Kafka
 * and is not interrupted when the server stops. Interrupting this thread makes the runner stop
 * the generator process and mark the job as INTERRUPTED, instead of leaving an orphaned process.
 */
public class GenerationJobExecutor implements SmartLifecycle {
    // Lifecycle beans stop in descending phase order. Kafka listener containers use
    // Integer.MAX_VALUE - 100, so this executor stops first and the waiting listener returns quickly.
    private static final int PHASE = Integer.MAX_VALUE;
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 10;
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerationJobExecutor.class);

    private final ExecuteGenerationJobUseCase executeGenerationJob;
    private final ExecutorService executor;
    private volatile boolean running;

    public GenerationJobExecutor(ExecuteGenerationJobUseCase executeGenerationJob) {
        this.executeGenerationJob = Objects.requireNonNull(executeGenerationJob, "executeGenerationJob");
        this.executor = Executors.newSingleThreadExecutor(
                Thread.ofPlatform().name("generation-worker-", 0).factory());
    }

    /**
     * Executes the job and waits until it finishes.
     *
     * @return the finished job, or an empty result if the job was not QUEUED any more
     * @throws java.util.concurrent.RejectedExecutionException if the executor is already stopped
     */
    public Optional<GenerationJob> executeAndWait(UUID jobId) throws InterruptedException {
        Future<Optional<GenerationJob>> execution = executor.submit(() -> executeGenerationJob.execute(jobId));
        try {
            return execution.get();
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Generation job " + jobId + " failed", exception.getCause());
        }
    }

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        running = false;
        // shutdownNow() interrupts the active job; the runner then stops the generator process.
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                LOGGER.error("Generation worker did not stop within {} s", SHUTDOWN_TIMEOUT_SECONDS);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
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
