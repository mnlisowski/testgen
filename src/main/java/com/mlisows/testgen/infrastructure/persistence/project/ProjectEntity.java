package com.mlisows.testgen.infrastructure.persistence.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "projects")
public class ProjectEntity {
    @Id
    @Column(name = "id", nullable = false, length = 100)
    private String id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    protected ProjectEntity() {
    }

    public ProjectEntity(String id, String name) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        if (id.isBlank() || id.length() > 100) {
            throw new IllegalArgumentException("Project id must contain 1 to 100 characters and not be blank");
        }
        if (name.isBlank() || name.length() > 255) {
            throw new IllegalArgumentException("Project name must contain 1 to 255 characters and not be blank");
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
