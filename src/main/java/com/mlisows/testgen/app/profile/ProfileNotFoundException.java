package com.mlisows.testgen.app.profile;

import java.util.UUID;

public final class ProfileNotFoundException extends RuntimeException {
    public ProfileNotFoundException(UUID profileId) {
        super("Profile not found: " + profileId);
    }
}
