package com.mlisows.testgen.usecase.ports;

import com.mlisows.testgen.domain.profile.ObservedProfile;
import com.mlisows.testgen.usecase.profile.StoredObservedProfile;

import java.util.Optional;
import java.util.UUID;

public interface ObservedProfileStore {
    /**
     * Stores an imported profile. The original text is kept unchanged, so a persistent store
     * can keep the uploaded file as it was, while the parsed profile is used by the generator.
     */
    StoredObservedProfile create(String projectId, String profileText, ObservedProfile profile);

    Optional<StoredObservedProfile> findById(UUID profileId);
}
