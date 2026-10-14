package com.mlisows.testgen.infrastructure.profile;

import com.mlisows.testgen.domain.profile.ObservedProfile;
import com.mlisows.testgen.usecase.ports.ObservedProfileStore;
import com.mlisows.testgen.usecase.profile.StoredObservedProfile;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryObservedProfileStore implements ObservedProfileStore {
    private final ConcurrentHashMap<UUID, StoredObservedProfile> profiles = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryObservedProfileStore(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public StoredObservedProfile create(String projectId, ObservedProfile profile) {
        while (true) {
            StoredObservedProfile storedProfile = new StoredObservedProfile(
                    UUID.randomUUID(), projectId, clock.instant(), profile);
            if (profiles.putIfAbsent(storedProfile.id(), storedProfile) == null) {
                return storedProfile;
            }
        }
    }

    @Override
    public Optional<StoredObservedProfile> findById(UUID profileId) {
        return Optional.ofNullable(profiles.get(Objects.requireNonNull(profileId, "profileId")));
    }
}
