package com.mlisows.testgen.infrastructure.project;

import java.nio.file.Path;
import java.util.Objects;

public final class InstrumentedProjectWorkspace {
    private final Path sourceRoot;
    private final Path classesRoot;
    private final String classpath;

    public InstrumentedProjectWorkspace(Path sourceRoot, Path classesRoot, String classpath) {
        this.sourceRoot = Objects.requireNonNull(sourceRoot, "sourceRoot must not be null");
        this.classesRoot = Objects.requireNonNull(classesRoot, "classesRoot must not be null");
        this.classpath = Objects.requireNonNull(classpath, "classpath must not be null");
    }

    public Path getSourceRoot() {
        return sourceRoot;
    }

    public Path getClassesRoot() {
        return classesRoot;
    }

    public String getClasspath() {
        return classpath;
    }
}
