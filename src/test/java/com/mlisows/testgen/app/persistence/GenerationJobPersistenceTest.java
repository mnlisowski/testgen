package com.mlisows.testgen.app.persistence;

import com.mlisows.testgen.domain.generation.GenerationSettings;
import com.mlisows.testgen.domain.profile.ObservedProfile;
import com.mlisows.testgen.infrastructure.persistence.job.GenerationJobEntity;
import com.mlisows.testgen.infrastructure.persistence.job.GenerationJobRepository;
import com.mlisows.testgen.infrastructure.persistence.job.JpaGenerationJobStore;
import com.mlisows.testgen.infrastructure.runtime.TextObservedProfileReader;
import com.mlisows.testgen.usecase.generation.GeneratedTestFile;
import com.mlisows.testgen.usecase.generation.GenerationMetrics;
import com.mlisows.testgen.usecase.generation.GenerationRequest;
import com.mlisows.testgen.usecase.generation.GenerationResult;
import com.mlisows.testgen.usecase.job.GenerationJob;
import com.mlisows.testgen.usecase.job.JobStatus;
import com.mlisows.testgen.usecase.ports.GenerationJobStore;
import com.mlisows.testgen.usecase.ports.ObservedProfileStore;
import com.mlisows.testgen.usecase.profile.StoredObservedProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class GenerationJobPersistenceTest {
    private static final ObservedProfile EMPTY_PROFILE = new ObservedProfile(List.of(), List.of());
    private static final GenerationSettings SETTINGS = new GenerationSettings(42, 120, 30, 3, 10);

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    private GenerationJobStore jobStore;

    @Autowired
    private GenerationJobRepository jobRepository;

    @Autowired
    private ObservedProfileStore profileStore;

    @Autowired
    private TextObservedProfileReader profileReader;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldUseDatabaseStoreWithPostgresProfile() {
        assertInstanceOf(JpaGenerationJobStore.class, jobStore);
    }

    @Test
    void shouldStoreQueuedJobWithSettingsAndPaths() {
        GenerationJob createdJob = jobStore.create("fit-bench", request(EMPTY_PROFILE));

        GenerationJob loadedJob = jobStore.findById(createdJob.id()).orElseThrow();

        assertEquals(JobStatus.QUEUED, loadedJob.status());
        assertEquals("fit-bench", loadedJob.projectId());
        assertEquals(SETTINGS, loadedJob.request().settings());
        assertEquals(Path.of("/projects/fit-bench"), loadedJob.request().projectRoot());
        assertEquals(Path.of("/work"), loadedJob.request().workRoot());
        assertEquals(createdJob.createdAt(), loadedJob.createdAt());
        assertTrue(loadedJob.profileId().isEmpty());
    }

    @Test
    void shouldRestoreProfileReferencedByJob() {
        String profileText = "hint|sample.Pricing.price(int)|amount|int|99|runtime-observed\n";
        ObservedProfile parsedProfile = profileReader.parse(profileText);
        StoredObservedProfile storedProfile = profileStore.create("fit-bench", profileText, parsedProfile);

        GenerationJob createdJob = jobStore.create("fit-bench", storedProfile.id(), request(parsedProfile));
        GenerationJob loadedJob = jobStore.findById(createdJob.id()).orElseThrow();

        assertEquals(storedProfile.id(), loadedJob.profileId().orElseThrow());
        assertEquals("99", loadedJob.request().observedProfile().getArgumentValueHints().getFirst().getValue());
    }

    @Test
    void shouldStoreResultOfSucceededJob() {
        UUID jobId = jobStore.create("fit-bench", request(EMPTY_PROFILE)).id();
        GenerationResult result = new GenerationResult(
                List.of(new GeneratedTestFile(Path.of("sample/PricingTest.java"), "class PricingTest {}")),
                "report text",
                new GenerationMetrics(1, 20, 2, 4, 2),
                Set.of("sample.Pricing#if:10:true", "sample.Pricing#if:10:false")
        );

        jobStore.start(jobId);
        jobStore.succeed(jobId, result);
        GenerationJob loadedJob = jobStore.findById(jobId).orElseThrow();

        assertEquals(JobStatus.SUCCEEDED, loadedJob.status());
        assertTrue(loadedJob.startedAt().isPresent());
        assertTrue(loadedJob.finishedAt().isPresent());
        assertEquals(result, loadedJob.result().orElseThrow());
    }

    @Test
    void shouldStoreErrorsOfFinishedJobs() {
        UUID rejectedJobId = jobStore.create("fit-bench", request(EMPTY_PROFILE)).id();
        UUID timedOutJobId = jobStore.create("fit-bench", request(EMPTY_PROFILE)).id();

        jobStore.fail(rejectedJobId, "Queue is full");
        jobStore.start(timedOutJobId);
        jobStore.timeOut(timedOutJobId, "Generation exceeded PT2M");

        GenerationJob rejectedJob = jobStore.findById(rejectedJobId).orElseThrow();
        GenerationJob timedOutJob = jobStore.findById(timedOutJobId).orElseThrow();
        assertEquals(JobStatus.FAILED, rejectedJob.status());
        assertEquals("Queue is full", rejectedJob.error().orElseThrow());
        assertTrue(rejectedJob.startedAt().isEmpty());
        assertEquals(JobStatus.TIMED_OUT, timedOutJob.status());
        assertEquals("Generation exceeded PT2M", timedOutJob.error().orElseThrow());
    }

    @Test
    void shouldRejectInvalidTransitionWithoutChangingStoredStatus() {
        UUID jobId = jobStore.create("fit-bench", request(EMPTY_PROFILE)).id();
        jobStore.start(jobId);

        assertThrows(IllegalStateException.class, () -> jobStore.start(jobId));

        assertEquals("RUNNING", jdbcTemplate.queryForObject(
                "SELECT status FROM generation_jobs WHERE id = ?", String.class, jobId));
    }

    @Test
    void shouldRejectUnknownJob() {
        UUID unknownJobId = UUID.randomUUID();

        assertTrue(jobStore.findById(unknownJobId).isEmpty());
        assertThrows(NoSuchElementException.class, () -> jobStore.start(unknownJobId));
    }

    @Test
    void shouldRejectChangeBasedOnStaleVersion() {
        UUID jobId = jobStore.create("fit-bench", request(EMPTY_PROFILE)).id();

        try (ExecutorService otherThread = Executors.newSingleThreadExecutor()) {
            assertThrows(ObjectOptimisticLockingFailureException.class, () ->
                    transactionTemplate.executeWithoutResult(transactionStatus -> {
                        // This transaction reads the job while it is still QUEUED.
                        GenerationJobEntity staleEntity = jobRepository.findById(jobId).orElseThrow();

                        // Another transaction starts the same job and commits first.
                        GenerationJob startedJob = startInOtherTransaction(otherThread, jobId);

                        // Committing a change made on the stale copy must not overwrite the newer state.
                        staleEntity.applyState(startedJob.fail("stale change", startedJob.createdAt()));
                    }));
        }

        assertEquals("RUNNING", jdbcTemplate.queryForObject(
                "SELECT status FROM generation_jobs WHERE id = ?", String.class, jobId));
    }

    private GenerationJob startInOtherTransaction(ExecutorService otherThread, UUID jobId) {
        try {
            return otherThread.submit(() -> jobStore.start(jobId)).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException(exception.getCause());
        }
    }

    private GenerationRequest request(ObservedProfile profile) {
        return new GenerationRequest(Path.of("/projects/fit-bench"), Path.of("/work"), SETTINGS, profile);
    }
}
