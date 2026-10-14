package com.mlisows.testgen.usecase.job;

import com.mlisows.testgen.usecase.generation.GenerationResult;
import com.mlisows.testgen.usecase.ports.GenerationJobStore;
import com.mlisows.testgen.usecase.ports.GenerationRunner;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/** Synchronous execution; scheduling and queue capacity belong to the caller. */
public final class ExecuteGenerationJobUseCase {
    private final GenerationJobStore jobStore;
    private final GenerationRunner generationRunner;

    public ExecuteGenerationJobUseCase(GenerationJobStore jobStore, GenerationRunner generationRunner) {
        this.jobStore = Objects.requireNonNull(jobStore, "jobStore");
        this.generationRunner = Objects.requireNonNull(generationRunner, "generationRunner");
    }

    public GenerationJob execute(UUID jobId) {
        GenerationJob runningJob = jobStore.start(jobId);
        GenerationResult generationResult;
        try {
            generationResult = generationRunner.run(jobId, runningJob.request());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return jobStore.interrupt(jobId, "Generation execution was interrupted");
        } catch (TimeoutException exception) {
            return jobStore.timeOut(jobId, errorMessage(exception));
        } catch (IOException | RuntimeException exception) {
            return jobStore.fail(jobId, errorMessage(exception));
        }
        // A persistence failure here must not be reported as a generator failure or trigger a rerun.
        return jobStore.succeed(jobId, generationResult);
    }

    private static String errorMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }
}
