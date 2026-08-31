package com.mlisows.testgen.infrastructure.project;

import java.nio.file.Path;
import java.util.Objects;

public final class MavenProjectLayout {
    private final Path projectRoot;
    private final Path pomPath;
    private final Path mainSourceRoot;

    public MavenProjectLayout(Path projectRoot, Path pomPath, Path mainSourceRoot) {
        this.projectRoot = Objects.requireNonNull(projectRoot, "projectRoot must not be null");
        this.pomPath = Objects.requireNonNull(pomPath, "pomPath must not be null");
        this.mainSourceRoot = Objects.requireNonNull(mainSourceRoot, "mainSourceRoot must not be null");
    }

    public Path getProjectRoot() {
        return projectRoot;
    }

    public Path getPomPath() {
        return pomPath;
    }

    public Path getMainSourceRoot() {
        return mainSourceRoot;
    }
}
