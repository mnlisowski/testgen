package com.mlisows.testgen.app.job;

import com.mlisows.testgen.app.config.TestgenProperties;
import com.mlisows.testgen.app.profile.ProfileNotFoundException;
import com.mlisows.testgen.app.profile.ProfileProjectMismatchException;
import com.mlisows.testgen.app.project.ProjectRegistry;
import com.mlisows.testgen.domain.generation.GenerationSettings;
import com.mlisows.testgen.domain.profile.ObservedProfile;
import com.mlisows.testgen.usecase.generation.GenerationRequest;
import com.mlisows.testgen.usecase.generation.GenerationResult;
import com.mlisows.testgen.usecase.job.GenerationJob;
import com.mlisows.testgen.usecase.job.JobPage;
import com.mlisows.testgen.usecase.ports.GenerationJobStore;
import com.mlisows.testgen.usecase.ports.ObservedProfileStore;
import com.mlisows.testgen.usecase.profile.StoredObservedProfile;
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
    private final ObservedProfileStore profileStore;

    public GenerationJobService(
            ProjectRegistry projects,
            GenerationSettings defaults,
            TestgenProperties properties,
            GenerationJobStore jobStore,
            GenerationJobQueue jobQueue,
            ObservedProfileStore profileStore
    ) {
        this.projects = projects;
        this.defaults = defaults;
        this.workRoot = properties.workRootPath();
        this.jobStore = jobStore;
        this.jobQueue = jobQueue;
        this.profileStore = profileStore;
    }

    public GenerationJob submit(SubmitGenerationRequest submission) {
        Path projectRoot = projects.findProjectRoot(submission.projectId())
                .orElseThrow(() -> new ProjectNotFoundException(submission.projectId()));

        GenerationRequest generationRequest = new GenerationRequest(
                projectRoot, workRoot, submission.resolveSettings(defaults),
                resolveProfile(submission.projectId(), submission.profileId())
        );
        GenerationJob queuedJob = jobStore.create(submission.projectId(), submission.profileId(), generationRequest);
        jobQueue.submit(queuedJob.id());
        return queuedJob;
    }

    private ObservedProfile resolveProfile(String projectId, UUID profileId) {
        if (profileId == null) {
            return new ObservedProfile(List.of(), List.of());
        }
        StoredObservedProfile storedProfile = profileStore.findById(profileId)
                .orElseThrow(() -> new ProfileNotFoundException(profileId));
        if (!storedProfile.projectId().equals(projectId)) {
            throw new ProfileProjectMismatchException(profileId, projectId);
        }
        return storedProfile.profile();
    }

    public GenerationResult findResult(UUID jobId) {
        GenerationJob job = find(jobId);
        return job.result()
                .orElseThrow(() -> new JobResultUnavailableException(jobId, job.status()));
    }

    public JobPage findProjectHistory(String projectId, int page, int size) {
        if (projects.findProjectRoot(projectId).isEmpty()) {
            throw new ProjectNotFoundException(projectId);
        }
        return jobStore.findByProject(projectId, page, size);
    }

    public GenerationJob find(UUID jobId) {
        return jobStore.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));
    }
}
