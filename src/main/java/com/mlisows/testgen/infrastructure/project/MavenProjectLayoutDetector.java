package com.mlisows.testgen.infrastructure.project;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class MavenProjectLayoutDetector {

    public MavenProjectLayout detect(Path projectRoot) {
        Objects.requireNonNull(projectRoot, "projectRoot must not be null");

        if (!Files.isDirectory(projectRoot)) {
            throw new IllegalArgumentException("Project root is not a directory: " + projectRoot);
        }

        Path pomPath = projectRoot.resolve("pom.xml");
        if (!Files.isRegularFile(pomPath)) {
            throw new IllegalArgumentException("Maven pom.xml not found: " + pomPath);
        }

        Path mainSourceRoot = projectRoot.resolve("src/main/java");
        if (!Files.isDirectory(mainSourceRoot)) {
            throw new IllegalArgumentException("Main source root not found: " + mainSourceRoot);
        }

        return new MavenProjectLayout(projectRoot, pomPath, mainSourceRoot);
    }
}
