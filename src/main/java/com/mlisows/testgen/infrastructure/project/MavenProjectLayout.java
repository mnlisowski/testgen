package com.mlisows.testgen.infrastructure.project;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public final class MavenProjectLayout {
    private final Path projectRoot;
    private final Path pomPath;
    private final Path mainSourceRoot;
    private final Optional<Path> mainResourceRoot;

    public MavenProjectLayout(Path projectRoot, Path pomPath, Path mainSourceRoot) {
        this(projectRoot, pomPath, mainSourceRoot, Optional.empty());
    }

    public MavenProjectLayout(
            Path projectRoot,
            Path pomPath,
            Path mainSourceRoot,
            Optional<Path> mainResourceRoot
    ) {
        this.projectRoot = Objects.requireNonNull(projectRoot, "projectRoot must not be null");
        this.pomPath = Objects.requireNonNull(pomPath, "pomPath must not be null");
        this.mainSourceRoot = Objects.requireNonNull(mainSourceRoot, "mainSourceRoot must not be null");
        this.mainResourceRoot = Objects.requireNonNull(mainResourceRoot, "mainResourceRoot must not be null");
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

    public Optional<Path> getMainResourceRoot() {
        return mainResourceRoot;
    }
}
