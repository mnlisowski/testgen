package com.mlisows.testgen.app.job;

import com.mlisows.testgen.domain.generation.GenerationSettings;
import com.mlisows.testgen.domain.profile.ObservedProfile;
import com.mlisows.testgen.infrastructure.job.InMemoryGenerationJobStore;
import com.mlisows.testgen.usecase.CandidateCoverageEvaluation;
import com.mlisows.testgen.usecase.GenerationReport;
import com.mlisows.testgen.usecase.generation.GenerationRequest;
import com.mlisows.testgen.usecase.generation.GenerationResult;
import com.mlisows.testgen.usecase.job.ExecuteGenerationJobUseCase;
import com.mlisows.testgen.usecase.job.GenerationJob;
import com.mlisows.testgen.usecase.job.JobStatus;
import com.mlisows.testgen.usecase.ports.GenerationRunner;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerationJobQueueTest {
    private final InMemoryGenerationJobStore jobStore = new InMemoryGenerationJobStore(Clock.systemUTC());

    @Test
    void shouldLimitQueueAndExecuteOnlyOneJobAtATime() throws Exception {
        var firstStarted = new CountDownLatch(1);
        var allowCompletion = new CountDownLatch(1);
        var activeRuns = new AtomicInteger();
        var maximumActiveRuns = new AtomicInteger();

        GenerationRunner runner = (jobId, request) -> {
            int activeCount = activeRuns.incrementAndGet();
            maximumActiveRuns.accumulateAndGet(activeCount, Math::max);
            firstStarted.countDown();
            try {
                if (!allowCompletion.await(10, TimeUnit.SECONDS)) {
                    throw new AssertionError("Test did not release the runner");
                }
                return emptyResult();
            } finally {
                activeRuns.decrementAndGet();
            }
        };

        try (var queue = new GenerationJobQueue(jobStore, new ExecuteGenerationJobUseCase(jobStore, runner), 1)) {
            GenerationJob firstJob = createJob();
            GenerationJob waitingJob = createJob();
            GenerationJob rejectedJob = createJob();

            queue.submit(firstJob.id());
            assertTrue(firstStarted.await(5, TimeUnit.SECONDS));
            queue.submit(waitingJob.id());
            JobQueueFullException rejection = assertThrows(JobQueueFullException.class,
                    () -> queue.submit(rejectedJob.id()));

            assertEquals(rejectedJob.id(), rejection.jobId());
            assertEquals(JobStatus.RUNNING, jobStore.findById(firstJob.id()).orElseThrow().status());
            assertEquals(JobStatus.QUEUED, jobStore.findById(waitingJob.id()).orElseThrow().status());
            assertEquals(JobStatus.FAILED, jobStore.findById(rejectedJob.id()).orElseThrow().status());

            allowCompletion.countDown();
            awaitStatus(firstJob.id(), JobStatus.SUCCEEDED);
            awaitStatus(waitingJob.id(), JobStatus.SUCCEEDED);
            assertEquals(1, maximumActiveRuns.get());
        } finally {
            allowCompletion.countDown();
        }
    }

    @Test
    void shouldInterruptActiveJobFailWaitingJobsAndRejectSubmissionsAfterShutdown() throws Exception {
        var started = new CountDownLatch(1);
        GenerationRunner runner = (jobId, request) -> {
            started.countDown();
            new CountDownLatch(1).await();
            return emptyResult();
        };
        var queue = new GenerationJobQueue(jobStore, new ExecuteGenerationJobUseCase(jobStore, runner), 1);
        GenerationJob runningJob = createJob();
        GenerationJob waitingJob = createJob();
        try {
            queue.submit(runningJob.id());
            assertTrue(started.await(5, TimeUnit.SECONDS));
            queue.submit(waitingJob.id());
        } finally {
            queue.close();
        }

        assertEquals(JobStatus.INTERRUPTED, jobStore.findById(runningJob.id()).orElseThrow().status());
        assertEquals(JobStatus.FAILED, jobStore.findById(waitingJob.id()).orElseThrow().status());
        GenerationJob lateJob = createJob();
        assertThrows(JobQueueFullException.class, () -> queue.submit(lateJob.id()));
        assertEquals(JobStatus.FAILED, jobStore.findById(lateJob.id()).orElseThrow().status());
    }

    private GenerationJob createJob() {
        GenerationRequest request = new GenerationRequest(Path.of("project"), Path.of("work"),
                GenerationSettings.defaults(), new ObservedProfile(List.of(), List.of()));
        return jobStore.create("sample", request);
    }

    private GenerationResult emptyResult() {
        return GenerationResult.from(List.of(),
                GenerationReport.from(List.of(), List.of(), CandidateCoverageEvaluation.empty()));
    }

    private void awaitStatus(UUID jobId, JobStatus expectedStatus) throws Exception {
        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (jobStore.findById(jobId).orElseThrow().status() != expectedStatus
                && System.nanoTime() < deadlineNanos) {
            Thread.sleep(10);
        }
        assertEquals(expectedStatus, jobStore.findById(jobId).orElseThrow().status());
    }
}
