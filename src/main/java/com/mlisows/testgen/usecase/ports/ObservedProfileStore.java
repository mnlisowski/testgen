package com.mlisows.testgen.usecase.ports;

import com.mlisows.testgen.domain.profile.ObservedProfile;
import com.mlisows.testgen.usecase.profile.StoredObservedProfile;

import java.util.Optional;
import java.util.UUID;

public interface ObservedProfileStore {
    StoredObservedProfile create(String projectId, ObservedProfile profile);

    Optional<StoredObservedProfile> findById(UUID profileId);
}
