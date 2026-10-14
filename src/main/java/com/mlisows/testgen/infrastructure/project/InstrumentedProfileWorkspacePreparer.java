package com.mlisows.testgen.infrastructure.project;

import com.mlisows.testgen.infrastructure.execution.JavaSourceCompiler;
import com.mlisows.testgen.infrastructure.instrumentation.JavaParserArgumentInstrumenter;
import com.mlisows.testgen.infrastructure.instrumentation.SourceDirectoryInstrumenter;
import com.mlisows.testgen.usecase.profile.ProfilePreparationRequest;
import com.mlisows.testgen.usecase.profile.ProfilePreparationResult;
import com.mlisows.testgen.usecase.ports.ProfileWorkspacePreparer;
import java.util.Map;

public final class InstrumentedProfileWorkspacePreparer implements ProfileWorkspacePreparer {
    @Override
    public ProfilePreparationResult prepare(ProfilePreparationRequest request) {
        MavenProjectLayout layout = new MavenProjectLayoutDetector().detect(request.projectRoot());
        String classpath = new MavenProjectClasspath().resolve(layout);
        var preparer = new InstrumentedProjectWorkspacePreparer(
                new SourceDirectoryInstrumenter(new JavaParserArgumentInstrumenter()),
                new JavaSourceCompiler(classpath), classpath);
        var workspace = preparer.prepare(layout, request.workRoot(), Map.of());
        return new ProfilePreparationResult(workspace.getSourceRoot(), workspace.getClassesRoot(),
                request.workRoot().resolve("observed-profile.txt"),
                JavaSourceCompiler.joinClasspaths(workspace.getClassesRoot().toString(), workspace.getClasspath()));
    }
}
