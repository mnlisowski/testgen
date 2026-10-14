package com.mlisows.testgen.usecase.profile;

import java.nio.file.Path;
import java.util.Objects;

public record ProfilePreparationResult(Path sourceRoot, Path classesRoot, Path profilePath,
                                       String runtimeClasspath) {
    public ProfilePreparationResult {
        Objects.requireNonNull(sourceRoot, "sourceRoot");
        Objects.requireNonNull(classesRoot, "classesRoot");
        Objects.requireNonNull(profilePath, "profilePath");
        Objects.requireNonNull(runtimeClasspath, "runtimeClasspath");
    }
}
