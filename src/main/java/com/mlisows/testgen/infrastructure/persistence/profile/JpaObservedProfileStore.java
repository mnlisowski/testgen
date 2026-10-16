package com.mlisows.testgen.infrastructure.persistence.profile;

import com.mlisows.testgen.domain.profile.ObservedProfile;
import com.mlisows.testgen.infrastructure.persistence.project.ProjectEntity;
import com.mlisows.testgen.infrastructure.persistence.project.ProjectRepository;
import com.mlisows.testgen.infrastructure.runtime.TextObservedProfileReader;
import com.mlisows.testgen.usecase.ports.ObservedProfileStore;
import com.mlisows.testgen.usecase.profile.StoredObservedProfile;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Stores the original profile text in PostgreSQL and parses it again when the profile is read.
 */
public class JpaObservedProfileStore implements ObservedProfileStore {
    private final ObservedProfileRepository profileRepository;
    private final ProjectRepository projectRepository;
    private final TextObservedProfileReader profileReader;
    private final Clock clock;

    public JpaObservedProfileStore(
            ObservedProfileRepository profileRepository,
            ProjectRepository projectRepository,
            TextObservedProfileReader profileReader,
            Clock clock
    ) {
        this.profileRepository = Objects.requireNonNull(profileRepository, "profileRepository");
        this.projectRepository = Objects.requireNonNull(projectRepository, "projectRepository");
        this.profileReader = Objects.requireNonNull(profileReader, "profileReader");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    @Transactional
    public StoredObservedProfile create(String projectId, String profileText, ObservedProfile profile) {
        Objects.requireNonNull(profileText, "profileText");
        Objects.requireNonNull(profile, "profile");

        // A reference is enough to set the foreign key; it does not query the projects table.
        ProjectEntity project = projectRepository.getReferenceById(projectId);
        // PostgreSQL stores microseconds, so the returned value must match a later read from the database.
        Instant createdAt = clock.instant().truncatedTo(ChronoUnit.MICROS);

        ObservedProfileEntity profileEntity = new ObservedProfileEntity(
                project, profileText, createdAt);
        ObservedProfileEntity savedEntity = profileRepository.save(profileEntity);

        return new StoredObservedProfile(savedEntity.getId(), projectId, createdAt, profile);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredObservedProfile> findById(UUID profileId) {
        Objects.requireNonNull(profileId, "profileId");

        return profileRepository.findById(profileId).map(this::toStoredProfile);
    }

    private StoredObservedProfile toStoredProfile(ObservedProfileEntity profileEntity) {
        ObservedProfile profile = profileReader.parse(profileEntity.getContent());

        return new StoredObservedProfile(
                profileEntity.getId(),
                profileEntity.getProject().getId(),
                profileEntity.getCreatedAt(),
                profile
        );
    }
}
