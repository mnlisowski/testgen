package com.mlisows.testgen.infrastructure.persistence.job;

import com.mlisows.testgen.infrastructure.persistence.profile.ObservedProfileEntity;
import com.mlisows.testgen.infrastructure.persistence.project.ProjectEntity;
import com.mlisows.testgen.usecase.job.GenerationJob;
import com.mlisows.testgen.usecase.job.JobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "generation_jobs")
public class GenerationJobEntity {
    // The id is assigned by the domain model. Spring Data still detects a new entity
    // because the version is null until the first insert.
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id")
    private ObservedProfileEntity profile;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JobStatus status;

    @Column(name = "project_root", nullable = false, columnDefinition = "TEXT")
    private String projectRoot;

    @Column(name = "work_root", nullable = false, columnDefinition = "TEXT")
    private String workRoot;

    @Embedded
    private GenerationSettingsEmbeddable settings;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "error", columnDefinition = "TEXT")
    private String error;

    // Hibernate adds "AND version = ?" to every UPDATE. If another transaction changed the job
    // in the meantime, the update affects no row and Hibernate reports an optimistic locking failure.
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected GenerationJobEntity() {
    }

    public GenerationJobEntity(GenerationJob job, ProjectEntity project, ObservedProfileEntity profile) {
        this.id = job.id();
        this.project = Objects.requireNonNull(project, "project");
        this.profile = profile;
        this.projectRoot = job.request().projectRoot().toString();
        this.workRoot = job.request().workRoot().toString();
        this.settings = new GenerationSettingsEmbeddable(job.request().settings());
        this.createdAt = job.createdAt();
        applyState(job);
    }

    /** Copies the fields that change during status transitions. */
    public void applyState(GenerationJob job) {
        this.status = job.status();
        this.startedAt = job.startedAt().orElse(null);
        this.finishedAt = job.finishedAt().orElse(null);
        this.error = job.error().orElse(null);
    }

    public UUID getId() {
        return id;
    }

    public ProjectEntity getProject() {
        return project;
    }

    public ObservedProfileEntity getProfile() {
        return profile;
    }

    public JobStatus getStatus() {
        return status;
    }

    public String getProjectRoot() {
        return projectRoot;
    }

    public String getWorkRoot() {
        return workRoot;
    }

    public GenerationSettingsEmbeddable getSettings() {
        return settings;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public String getError() {
        return error;
    }
}
