package com.mlisows.testgen.app.profile;

import com.mlisows.testgen.usecase.profile.StoredObservedProfile;

import java.time.Instant;
import java.util.UUID;

public record ObservedProfileResponse(
        UUID profileId,
        String projectId,
        Instant createdAt,
        int hintCount,
        int invocationCount
) {
    public static ObservedProfileResponse from(StoredObservedProfile storedProfile) {
        return new ObservedProfileResponse(storedProfile.id(), storedProfile.projectId(), storedProfile.createdAt(),
                storedProfile.profile().getArgumentValueHints().size(),
                storedProfile.profile().getObservedInvocations().size());
    }
}
