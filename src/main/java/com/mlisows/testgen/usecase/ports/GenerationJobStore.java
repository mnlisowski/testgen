package com.mlisows.testgen.usecase.ports;

import com.mlisows.testgen.usecase.generation.GenerationRequest;
import com.mlisows.testgen.usecase.generation.GenerationResult;
import com.mlisows.testgen.usecase.job.GenerationJob;
import com.mlisows.testgen.usecase.job.JobPage;
import com.mlisows.testgen.usecase.job.JobStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Stores job snapshots. Each transition must atomically check and update the current status.
 */
public interface GenerationJobStore {
    default GenerationJob create(String projectId, GenerationRequest request) {
        return create(projectId, null, request);
    }

    GenerationJob create(String projectId, UUID profileId, GenerationRequest request);
    Optional<GenerationJob> findById(UUID id);

    /** Returns identifiers of jobs with the given status, oldest first. */
    List<UUID> findIdsByStatus(JobStatus status);

    /** Returns one page of job summaries of a project, newest first. */
    JobPage findByProject(String projectId, int page, int size);

    GenerationJob start(UUID id);

    /**
     * Starts the job if it is still QUEUED. Returns an empty result if the job has another status
     * or another process changed it at the same time; this is how duplicate requests are skipped.
     *
     * @throws java.util.NoSuchElementException if the job does not exist
     */
    Optional<GenerationJob> startIfQueued(UUID id);
    GenerationJob succeed(UUID id, GenerationResult result);
    GenerationJob fail(UUID id, String error);
    GenerationJob timeOut(UUID id, String error);
    GenerationJob interrupt(UUID id, String error);
}
