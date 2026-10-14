package com.mlisows.testgen.app.project;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ProjectRegistry {
    private final Map<String, Path> projects;

    public ProjectRegistry(Map<String, Path> projects) {
        this.projects = Map.copyOf(projects);
    }

    public List<String> projectIds() {
        return projects.keySet().stream().sorted().toList();
    }

    public Optional<Path> findProjectRoot(String projectId) {
        return Optional.ofNullable(projects.get(projectId));
    }
}
