package com.mlisows.testgen.app.profile;

import com.mlisows.testgen.app.job.ProjectNotFoundException;
import com.mlisows.testgen.app.project.ProjectRegistry;
import com.mlisows.testgen.domain.profile.ObservedProfile;
import com.mlisows.testgen.infrastructure.runtime.TextObservedProfileReader;
import com.mlisows.testgen.usecase.ports.ObservedProfileStore;
import com.mlisows.testgen.usecase.profile.StoredObservedProfile;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("api")
@Service
public class ObservedProfileService {
    private final ProjectRegistry projects;
    private final TextObservedProfileReader profileReader;
    private final ObservedProfileStore profileStore;

    public ObservedProfileService(ProjectRegistry projects, TextObservedProfileReader profileReader,
                                  ObservedProfileStore profileStore) {
        this.projects = projects;
        this.profileReader = profileReader;
        this.profileStore = profileStore;
    }

    public StoredObservedProfile importProfile(String projectId, String text) {
        if (projects.findProjectRoot(projectId).isEmpty()) {
            throw new ProjectNotFoundException(projectId);
        }

        ObservedProfile profile;
        try {
            profile = profileReader.parse(text);
        } catch (IllegalArgumentException exception) {
            throw new InvalidObservedProfileException(exception.getMessage());
        }
        if (profile.getArgumentValueHints().isEmpty() && profile.getObservedInvocations().isEmpty()) {
            throw new InvalidObservedProfileException("Profile must contain at least one hint or invocation");
        }
        return profileStore.create(projectId, text, profile);
    }
}
