package com.mlisows.testgen.infrastructure.persistence.job;

import com.mlisows.testgen.domain.profile.ObservedProfile;
import com.mlisows.testgen.infrastructure.persistence.profile.ObservedProfileEntity;
import com.mlisows.testgen.infrastructure.persistence.profile.ObservedProfileRepository;
import com.mlisows.testgen.infrastructure.persistence.project.ProjectEntity;
import com.mlisows.testgen.infrastructure.persistence.project.ProjectRepository;
import com.mlisows.testgen.usecase.generation.GenerationRequest;
import com.mlisows.testgen.usecase.generation.GenerationResult;
import com.mlisows.testgen.usecase.job.GenerationJob;
import com.mlisows.testgen.usecase.job.JobStatus;
import com.mlisows.testgen.usecase.ports.GenerationJobStore;
import com.mlisows.testgen.usecase.ports.ObservedProfileStore;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Stores generation jobs in PostgreSQL. Each status change loads the job, applies the transition
 * rules from {@link GenerationJob} and saves it in one transaction; the {@code @Version} column
 * rejects a concurrent change of the same job.
 */
public class JpaGenerationJobStore implements GenerationJobStore {
    private static final ObservedProfile EMPTY_PROFILE = new ObservedProfile(List.of(), List.of());

    private final GenerationJobRepository jobRepository;
    private final GenerationResultRepository resultRepository;
    private final ProjectRepository projectRepository;
    private final ObservedProfileRepository profileRepository;
    private final ObservedProfileStore profileStore;
    private final Clock clock;

    public JpaGenerationJobStore(
            GenerationJobRepository jobRepository,
            GenerationResultRepository resultRepository,
            ProjectRepository projectRepository,
            ObservedProfileRepository profileRepository,
            ObservedProfileStore profileStore,
            Clock clock
    ) {
        this.jobRepository = Objects.requireNonNull(jobRepository, "jobRepository");
        this.resultRepository = Objects.requireNonNull(resultRepository, "resultRepository");
        this.projectRepository = Objects.requireNonNull(projectRepository, "projectRepository");
        this.profileRepository = Objects.requireNonNull(profileRepository, "profileRepository");
        this.profileStore = Objects.requireNonNull(profileStore, "profileStore");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    @Transactional
    public GenerationJob create(String projectId, UUID profileId, GenerationRequest request) {
        GenerationJob queuedJob = GenerationJob.queued(UUID.randomUUID(), projectId, profileId, request, now());

        ProjectEntity project = projectRepository.getReferenceById(projectId);
        ObservedProfileEntity profile = null;
        if (profileId != null) {
            profile = profileRepository.getReferenceById(profileId);
        }
        jobRepository.save(new GenerationJobEntity(queuedJob, project, profile));

        return queuedJob;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GenerationJob> findById(UUID id) {
        Objects.requireNonNull(id, "id");

        return jobRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findIdsByStatus(JobStatus status) {
        Objects.requireNonNull(status, "status");

        return jobRepository.findIdsByStatus(status);
    }

    @Override
    @Transactional
    public GenerationJob start(UUID id) {
        return update(id, job -> job.start(now()));
    }

    @Override
    @Transactional
    public GenerationJob succeed(UUID id, GenerationResult result) {
        Objects.requireNonNull(result, "result");

        GenerationJobEntity jobEntity = loadEntity(id);
        GenerationJob succeededJob = toDomain(jobEntity).succeed(result, now());
        jobEntity.applyState(succeededJob);
        resultRepository.save(new GenerationResultEntity(jobEntity, result));

        return succeededJob;
    }

    @Override
    @Transactional
    public GenerationJob fail(UUID id, String error) {
        return update(id, job -> job.fail(error, now()));
    }

    @Override
    @Transactional
    public GenerationJob timeOut(UUID id, String error) {
        return update(id, job -> job.timeOut(error, now()));
    }

    @Override
    @Transactional
    public GenerationJob interrupt(UUID id, String error) {
        return update(id, job -> job.interrupt(error, now()));
    }

    /**
     * The entity is managed by Hibernate inside the transaction, so changing its fields is enough:
     * the UPDATE (with the version check) is sent when the transaction commits.
     */
    private GenerationJob update(UUID id, UnaryOperator<GenerationJob> transition) {
        GenerationJobEntity jobEntity = loadEntity(id);
        GenerationJob changedJob = transition.apply(toDomain(jobEntity));
        jobEntity.applyState(changedJob);
        return changedJob;
    }

    private GenerationJobEntity loadEntity(UUID id) {
        Objects.requireNonNull(id, "id");

        return jobRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Unknown generation job: " + id));
    }

    private GenerationJob toDomain(GenerationJobEntity jobEntity) {
        UUID profileId = null;
        if (jobEntity.getProfile() != null) {
            profileId = jobEntity.getProfile().getId();
        }

        GenerationRequest request = new GenerationRequest(
                Path.of(jobEntity.getProjectRoot()),
                Path.of(jobEntity.getWorkRoot()),
                jobEntity.getSettings().toSettings(),
                loadProfile(profileId)
        );

        GenerationResult result = null;
        if (jobEntity.getStatus() == JobStatus.SUCCEEDED) {
            result = resultRepository.findById(jobEntity.getId())
                    .map(GenerationResultEntity::toResult)
                    .orElseThrow(() -> new IllegalStateException(
                            "Succeeded job has no stored result: " + jobEntity.getId()));
        }

        return GenerationJob.restore(
                jobEntity.getId(),
                jobEntity.getProject().getId(),
                profileId,
                request,
                jobEntity.getStatus(),
                jobEntity.getCreatedAt(),
                jobEntity.getStartedAt(),
                jobEntity.getFinishedAt(),
                result,
                jobEntity.getError()
        );
    }

    private ObservedProfile loadProfile(UUID profileId) {
        if (profileId == null) {
            return EMPTY_PROFILE;
        }
        return profileStore.findById(profileId)
                .orElseThrow(() -> new IllegalStateException("Stored job references a missing profile: " + profileId))
                .profile();
    }

    private Instant now() {
        // PostgreSQL stores microseconds; the returned job must equal a job read back later.
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }
}
