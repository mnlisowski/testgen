package com.mlisows.testgen.infrastructure.job;

import com.mlisows.testgen.domain.generation.GenerationSettings;
import com.mlisows.testgen.domain.profile.ObservedProfile;
import com.mlisows.testgen.usecase.CandidateCoverageEvaluation;
import com.mlisows.testgen.usecase.GenerationReport;
import com.mlisows.testgen.usecase.generation.GenerationRequest;
import com.mlisows.testgen.usecase.generation.GenerationResult;
import com.mlisows.testgen.usecase.job.JobStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryGenerationJobStoreTest {
    private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");
    private final InMemoryGenerationJobStore store =
            new InMemoryGenerationJobStore(Clock.fixed(NOW, ZoneOffset.UTC));

    private GenerationRequest request() {
        return new GenerationRequest(Path.of("examples/fit-bench"), Path.of("target/job-test"),
                GenerationSettings.defaults(), new ObservedProfile(List.of(), List.of()));
    }

    private GenerationResult result() {
        return GenerationResult.from(List.of(),
                GenerationReport.from(List.of(), List.of(), CandidateCoverageEvaluation.empty()));
    }

    @Test
    void shouldCreateIndependentQueuedJobsAndPreserveRequest() {
        var request = request();
        var first = store.create("fit-bench", request);
        var second = store.create("fit-bench", request);

        assertNotEquals(first.id(), second.id());
        assertEquals("fit-bench", first.projectId());
        assertSame(request, first.request());
        assertEquals(JobStatus.QUEUED, first.status());
        assertEquals(NOW, first.createdAt());
        assertTrue(first.startedAt().isEmpty());
        assertTrue(first.finishedAt().isEmpty());
        assertTrue(first.result().isEmpty());
        assertTrue(first.error().isEmpty());
        assertSame(first, store.findById(first.id()).orElseThrow());
    }

    @Test
    void shouldStoreResultWithoutChangingPreviouslyReadSnapshots() {
        var queued = store.create("fit-bench", request());
        var running = store.start(queued.id());
        var result = result();
        var completed = store.succeed(queued.id(), result);

        assertEquals(JobStatus.QUEUED, queued.status());
        assertEquals(JobStatus.RUNNING, running.status());
        assertTrue(running.finishedAt().isEmpty());
        assertEquals(JobStatus.SUCCEEDED, completed.status());
        assertEquals(NOW, completed.startedAt().orElseThrow());
        assertEquals(NOW, completed.finishedAt().orElseThrow());
        assertSame(result, completed.result().orElseThrow());
        assertTrue(completed.error().isEmpty());
        assertSame(completed, store.findById(queued.id()).orElseThrow());
    }

    @ParameterizedTest
    @EnumSource(value = JobStatus.class, names = {"SUCCEEDED", "FAILED", "TIMED_OUT", "INTERRUPTED"})
    void shouldPreventOverwritingTerminalState(JobStatus terminal) {
        UUID profileId = UUID.randomUUID();
        var id = store.create("fit-bench", profileId, request()).id();
        assertEquals(profileId, store.findById(id).orElseThrow().profileId().orElseThrow());
        assertEquals(profileId, store.start(id).profileId().orElseThrow());
        var finished = switch (terminal) {
            case SUCCEEDED -> store.succeed(id, result());
            case FAILED -> store.fail(id, "Execution failed");
            case TIMED_OUT -> store.timeOut(id, "Timeout");
            case INTERRUPTED -> store.interrupt(id, "Worker stopped");
            default -> throw new AssertionError(terminal);
        };

        assertEquals(terminal, finished.status());
        assertEquals(profileId, finished.profileId().orElseThrow());
        assertTrue(finished.finishedAt().isPresent());
        if (terminal != JobStatus.SUCCEEDED) {
            assertTrue(finished.error().isPresent());
            assertTrue(finished.result().isEmpty());
        }
        assertThrows(IllegalStateException.class, () -> store.start(id));
        assertThrows(IllegalStateException.class, () -> store.succeed(id, result()));
        assertThrows(IllegalStateException.class, () -> store.fail(id, "Late failure"));
        assertThrows(IllegalStateException.class, () -> store.timeOut(id, "Late timeout"));
        assertThrows(IllegalStateException.class, () -> store.interrupt(id, "Late interruption"));
        assertSame(finished, store.findById(id).orElseThrow());
    }

    @Test
    void shouldAllowQueueRejectionButNotSuccessOrTimeoutBeforeStarting() {
        var queued = store.create("fit-bench", request());
        assertThrows(IllegalStateException.class, () -> store.succeed(queued.id(), result()));
        assertThrows(IllegalStateException.class, () -> store.timeOut(queued.id(), "Timeout"));
        assertThrows(IllegalStateException.class, () -> store.interrupt(queued.id(), "Stopped"));
        assertSame(queued, store.findById(queued.id()).orElseThrow());

        var rejected = store.fail(queued.id(), "Executor queue is full");
        assertEquals(JobStatus.FAILED, rejected.status());
        assertTrue(rejected.startedAt().isEmpty());
        assertEquals("Executor queue is full", rejected.error().orElseThrow());
    }

    @Test
    void shouldLeaveStateUnchangedWhenCompletionDataIsInvalid() {
        var running = store.start(store.create("fit-bench", request()).id());
        assertThrows(NullPointerException.class, () -> store.succeed(running.id(), null));
        assertThrows(IllegalArgumentException.class, () -> store.fail(running.id(), " "));
        assertSame(running, store.findById(running.id()).orElseThrow());
    }

    @Test
    void shouldNotCreateJobsWhenUpdatingAnUnknownId() {
        UUID id = UUID.randomUUID();
        assertTrue(store.findById(id).isEmpty());
        assertThrows(NoSuchElementException.class, () -> store.start(id));
        assertTrue(store.findById(id).isEmpty());
    }

    @Test
    void shouldAllowOnlyOneConcurrentStart() throws Exception {
        UUID id = store.create("fit-bench", request()).id();
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Boolean> attempt = () -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) throw new AssertionError("Start timed out");
                try {
                    store.start(id);
                    return true;
                } catch (IllegalStateException alreadyStarted) {
                    return false;
                }
            };
            var first = executor.submit(attempt);
            var second = executor.submit(attempt);
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            assertNotEquals(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));
        } finally {
            start.countDown();
        }
        assertEquals(JobStatus.RUNNING, store.findById(id).orElseThrow().status());
    }
}
