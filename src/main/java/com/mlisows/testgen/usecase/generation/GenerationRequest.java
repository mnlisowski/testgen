package com.mlisows.testgen.usecase.generation;

import com.mlisows.testgen.domain.generation.GenerationSettings;

import com.mlisows.testgen.domain.profile.ObservedProfile;
import java.nio.file.Path;
import java.util.Objects;

public record GenerationRequest(Path projectRoot, Path workRoot, GenerationSettings settings,
                                ObservedProfile observedProfile) {
    public GenerationRequest {
        Objects.requireNonNull(projectRoot, "projectRoot");
        Objects.requireNonNull(workRoot, "workRoot");
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(observedProfile, "observedProfile");
    }
}
