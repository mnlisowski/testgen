package com.mlisows.testgen.usecase.profile;

import com.mlisows.testgen.domain.profile.ObservedProfile;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record StoredObservedProfile(UUID id, String projectId, Instant createdAt, ObservedProfile profile) {
    public StoredObservedProfile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(profile, "profile");
        if (projectId.isBlank()) {
            throw new IllegalArgumentException("projectId must not be blank");
        }
    }
}
