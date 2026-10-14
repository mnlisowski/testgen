package com.mlisows.testgen.app.profile;

import java.util.UUID;

public final class ProfileProjectMismatchException extends RuntimeException {
    public ProfileProjectMismatchException(UUID profileId, String projectId) {
        super("Profile " + profileId + " does not belong to project " + projectId);
    }
}
