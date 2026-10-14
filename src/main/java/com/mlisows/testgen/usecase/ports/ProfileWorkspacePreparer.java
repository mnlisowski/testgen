package com.mlisows.testgen.usecase.ports;

import com.mlisows.testgen.usecase.profile.ProfilePreparationRequest;
import com.mlisows.testgen.usecase.profile.ProfilePreparationResult;

public interface ProfileWorkspacePreparer {
    ProfilePreparationResult prepare(ProfilePreparationRequest request);
}
