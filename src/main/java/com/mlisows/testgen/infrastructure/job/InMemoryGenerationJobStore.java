package com.mlisows.testgen.infrastructure.job;

import com.mlisows.testgen.usecase.generation.GenerationRequest;
import com.mlisows.testgen.usecase.generation.GenerationResult;
import com.mlisows.testgen.usecase.job.GenerationJob;
import com.mlisows.testgen.usecase.ports.GenerationJobStore;

import java.time.Clock;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

/** Process-local storage: jobs and results are lost on restart. */
public final class InMemoryGenerationJobStore implements GenerationJobStore {
    private final ConcurrentHashMap<UUID, GenerationJob> jobs = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryGenerationJobStore(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public GenerationJob create(String projectId, UUID profileId, GenerationRequest request) {
        while (true) {
            GenerationJob job = GenerationJob.queued(UUID.randomUUID(), projectId, profileId, request, clock.instant());
            if (jobs.putIfAbsent(job.id(), job) == null) {
                return job;
            }
        }
    }

    @Override
    public Optional<GenerationJob> findById(UUID id) {
        return Optional.ofNullable(jobs.get(Objects.requireNonNull(id, "id")));
    }

    @Override
    public GenerationJob start(UUID id) {
        return update(id, job -> job.start(clock.instant()));
    }

    @Override
    public GenerationJob succeed(UUID id, GenerationResult result) {
        return update(id, job -> job.succeed(result, clock.instant()));
    }

    @Override
    public GenerationJob fail(UUID id, String error) {
        return update(id, job -> job.fail(error, clock.instant()));
    }

    @Override
    public GenerationJob timeOut(UUID id, String error) {
        return update(id, job -> job.timeOut(error, clock.instant()));
    }

    @Override
    public GenerationJob interrupt(UUID id, String error) {
        return update(id, job -> job.interrupt(error, clock.instant()));
    }

    private GenerationJob update(UUID id, UnaryOperator<GenerationJob> transition) {
        return jobs.compute(Objects.requireNonNull(id, "id"), (key, current) -> {
            if (current == null) {
                throw new NoSuchElementException("Unknown generation job: " + key);
            }
            return transition.apply(current);
        });
    }
}
