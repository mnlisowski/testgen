package com.mlisows.testgen.app.job;

import com.mlisows.testgen.app.config.TestgenProperties;
import com.mlisows.testgen.app.project.ProjectRegistry;
import com.mlisows.testgen.domain.generation.GenerationSettings;
import com.mlisows.testgen.domain.profile.ObservedProfile;
import com.mlisows.testgen.usecase.generation.GenerationRequest;
import com.mlisows.testgen.usecase.generation.GenerationResult;
import com.mlisows.testgen.usecase.job.GenerationJob;
import com.mlisows.testgen.usecase.ports.GenerationJobStore;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
public class GenerationJobService {
    private final ProjectRegistry projects;
    private final GenerationSettings defaults;
    private final Path workRoot;
    private final GenerationJobStore jobStore;
    private final GenerationJobQueue jobQueue;

    public GenerationJobService(ProjectRegistry projects, GenerationSettings defaults,
                                TestgenProperties properties, GenerationJobStore jobStore,
                                GenerationJobQueue jobQueue) {
        this.projects = projects;
        this.defaults = defaults;
        this.workRoot = properties.workRootPath();
        this.jobStore = jobStore;
        this.jobQueue = jobQueue;
    }

    public GenerationJob submit(SubmitGenerationRequest submission) {
        Path projectRoot = projects.findProjectRoot(submission.projectId())
                .orElseThrow(() -> new ProjectNotFoundException(submission.projectId()));

        GenerationRequest generationRequest = new GenerationRequest(
                projectRoot, workRoot, submission.resolveSettings(defaults),
                new ObservedProfile(List.of(), List.of())
        );
        GenerationJob queuedJob = jobStore.create(submission.projectId(), generationRequest);
        jobQueue.submit(queuedJob.id());
        return queuedJob;
    }

    public GenerationResult findResult(UUID jobId) {
        GenerationJob job = find(jobId);
        return job.result()
                .orElseThrow(() -> new JobResultUnavailableException(jobId, job.status()));
    }

    public GenerationJob find(UUID jobId) {
        return jobStore.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));
    }
}
