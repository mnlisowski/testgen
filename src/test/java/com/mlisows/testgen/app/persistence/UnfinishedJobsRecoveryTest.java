package com.mlisows.testgen.app.persistence;

import com.mlisows.testgen.app.job.UnfinishedJobsRecovery;
import com.mlisows.testgen.domain.generation.GenerationSettings;
import com.mlisows.testgen.domain.profile.ObservedProfile;
import com.mlisows.testgen.usecase.generation.GenerationRequest;
import com.mlisows.testgen.usecase.job.GenerationJob;
import com.mlisows.testgen.usecase.job.JobStatus;
import com.mlisows.testgen.usecase.ports.GenerationJobStore;
import com.mlisows.testgen.usecase.ports.GenerationRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class UnfinishedJobsRecoveryTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Container
    @ServiceConnection
    static final KafkaContainer kafka = new KafkaContainer("apache/kafka:4.1.0");

    // The generator must not run in this test; the mock lets the test check that.
    @MockitoBean
    private GenerationRunner generationRunner;

    @Autowired
    private GenerationJobStore jobStore;

    @Autowired
    private UnfinishedJobsRecovery recovery;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListeners;

    @Test
    void shouldInterruptRunningJobsAndLeaveQueuedJobsForKafkaRedelivery() throws Exception {
        // State left in the database by a server that stopped unexpectedly.
        UUID runningJobId = jobStore.create("fit-bench", request()).id();
        jobStore.start(runningJobId);
        UUID queuedJobId = jobStore.create("fit-bench", request()).id();

        // The same step Spring runs once at startup, before the Kafka listener starts.
        recovery.interruptRunningJobs();

        GenerationJob interruptedJob = jobStore.findById(runningJobId).orElseThrow();
        assertEquals(JobStatus.INTERRUPTED, interruptedJob.status());
        assertTrue(interruptedJob.error().orElseThrow().contains("Server stopped"));
        assertTrue(interruptedJob.finishedAt().isPresent());

        // A QUEUED job is left for its Kafka request; this job never had one, so it stays QUEUED.
        assertEquals(JobStatus.QUEUED, jobStore.findById(queuedJobId).orElseThrow().status());
        verify(generationRunner, never()).run(any(), any());
    }

    @Test
    void shouldStartBeforeKafkaListenerContainers() {
        // Lifecycle beans start in ascending phase order.
        assertTrue(recovery.getPhase() < kafkaListeners.getPhase());
    }

    private GenerationRequest request() {
        return new GenerationRequest(Path.of("/projects/fit-bench"), Path.of("/work"),
                GenerationSettings.defaults(), new ObservedProfile(List.of(), List.of()));
    }
}
