package com.mlisows.testgen.app.job;

public final class ProjectNotFoundException extends RuntimeException {
    public ProjectNotFoundException(String projectId) {
        super("Project not found: " + projectId);
    }
}
