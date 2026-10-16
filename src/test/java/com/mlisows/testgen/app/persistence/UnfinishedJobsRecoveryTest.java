package com.mlisows.testgen.app.persistence;

import com.mlisows.testgen.app.job.UnfinishedJobsRecovery;
import com.mlisows.testgen.domain.generation.GenerationSettings;
import com.mlisows.testgen.domain.profile.ObservedProfile;
import com.mlisows.testgen.usecase.generation.GenerationMetrics;
import com.mlisows.testgen.usecase.generation.GenerationRequest;
import com.mlisows.testgen.usecase.generation.GenerationResult;
import com.mlisows.testgen.usecase.job.GenerationJob;
import com.mlisows.testgen.usecase.job.JobStatus;
import com.mlisows.testgen.usecase.ports.GenerationJobStore;
import com.mlisows.testgen.usecase.ports.GenerationRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class UnfinishedJobsRecoveryTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Container
    @ServiceConnection
    static final KafkaContainer kafka = new KafkaContainer("apache/kafka:4.1.0");

    // The generator itself is not under test; the mock finishes every job immediately.
    @MockitoBean
    private GenerationRunner generationRunner;

    @Autowired
    private GenerationJobStore jobStore;

    @Autowired
    private UnfinishedJobsRecovery recovery;

    @Test
    void shouldInterruptRunningJobsAndExecuteQueuedJobsLeftByPreviousRun() throws Exception {
        when(generationRunner.run(any(), any())).thenReturn(emptyResult());

        // State left in the database by a server that stopped unexpectedly.
        UUID runningJobId = jobStore.create("fit-bench", request()).id();
        jobStore.start(runningJobId);
        UUID queuedJobId = jobStore.create("fit-bench", request()).id();

        // The same logic Spring runs once after the application starts.
        recovery.run(new DefaultApplicationArguments());

        GenerationJob interruptedJob = jobStore.findById(runningJobId).orElseThrow();
        assertEquals(JobStatus.INTERRUPTED, interruptedJob.status());
        assertTrue(interruptedJob.error().orElseThrow().contains("Server stopped"));
        assertTrue(interruptedJob.finishedAt().isPresent());

        awaitStatus(queuedJobId, JobStatus.SUCCEEDED);
        verify(generationRunner).run(eq(queuedJobId), any());
        verify(generationRunner, never()).run(eq(runningJobId), any());
    }

    private void awaitStatus(UUID jobId, JobStatus expectedStatus) throws InterruptedException {
        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (jobStore.findById(jobId).orElseThrow().status() != expectedStatus
                && System.nanoTime() < deadlineNanos) {
            Thread.sleep(20);
        }
        assertEquals(expectedStatus, jobStore.findById(jobId).orElseThrow().status());
    }

    private GenerationRequest request() {
        return new GenerationRequest(Path.of("/projects/fit-bench"), Path.of("/work"),
                GenerationSettings.defaults(), new ObservedProfile(List.of(), List.of()));
    }

    private GenerationResult emptyResult() {
        return new GenerationResult(List.of(), "report", new GenerationMetrics(0, 0, 0, 0, 0), Set.of());
    }
}
