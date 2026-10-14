package com.mlisows.testgen.usecase.ports;

import com.mlisows.testgen.usecase.generation.GenerationRequest;
import com.mlisows.testgen.usecase.generation.GenerationResult;
import com.mlisows.testgen.usecase.job.GenerationJob;

import java.util.Optional;
import java.util.UUID;

/**
 * Stores job snapshots. Each transition must atomically check and update the current status.
 */
public interface GenerationJobStore {
    GenerationJob create(String projectId, GenerationRequest request);
    Optional<GenerationJob> findById(UUID id);
    GenerationJob start(UUID id);
    GenerationJob succeed(UUID id, GenerationResult result);
    GenerationJob fail(UUID id, String error);
    GenerationJob timeOut(UUID id, String error);
    GenerationJob interrupt(UUID id, String error);
}
