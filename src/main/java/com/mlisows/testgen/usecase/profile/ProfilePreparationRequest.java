package com.mlisows.testgen.usecase.profile;

import java.nio.file.Path;
import java.util.Objects;

public record ProfilePreparationRequest(Path projectRoot, Path workRoot) {
    public ProfilePreparationRequest {
        Objects.requireNonNull(projectRoot, "projectRoot");
        Objects.requireNonNull(workRoot, "workRoot");
    }
}
