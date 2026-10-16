package com.mlisows.testgen.infrastructure.persistence.profile;

import com.mlisows.testgen.infrastructure.persistence.project.ProjectEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "observed_profiles")
public class ObservedProfileEntity {
    // Hibernate assigns the UUID during persist. Because the id is null before saving,
    // Spring Data recognizes a new entity and inserts it without a preceding SELECT.
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    // LAZY: loading a profile does not load its project. Reading project.getId()
    // does not require a query either, because the proxy already knows its id.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ObservedProfileEntity() {
    }

    public ObservedProfileEntity(ProjectEntity project, String content, Instant createdAt) {
        this.project = Objects.requireNonNull(project, "project");
        this.content = Objects.requireNonNull(content, "content");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public UUID getId() {
        return id;
    }

    public ProjectEntity getProject() {
        return project;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
