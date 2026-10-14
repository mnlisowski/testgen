package com.mlisows.testgen.usecase.profile;

import com.mlisows.testgen.usecase.ports.ProfileWorkspacePreparer;
import java.util.Objects;

/** Prepares a local instrumented workspace; running the application remains the caller's responsibility. */
public final class PrepareProfileUseCase {
    private final ProfileWorkspacePreparer workspacePreparer;

    public PrepareProfileUseCase(ProfileWorkspacePreparer workspacePreparer) {
        this.workspacePreparer = Objects.requireNonNull(workspacePreparer, "workspacePreparer");
    }

    public ProfilePreparationResult prepare(ProfilePreparationRequest request) {
        return workspacePreparer.prepare(Objects.requireNonNull(request, "request"));
    }
}
