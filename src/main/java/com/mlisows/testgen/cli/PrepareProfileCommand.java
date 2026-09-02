package com.mlisows.testgen.cli;

import com.mlisows.testgen.infrastructure.execution.JavaSourceCompiler;
import com.mlisows.testgen.infrastructure.instrumentation.JavaParserArgumentInstrumenter;
import com.mlisows.testgen.infrastructure.instrumentation.SourceDirectoryInstrumenter;
import com.mlisows.testgen.infrastructure.project.InstrumentedProjectWorkspace;
import com.mlisows.testgen.infrastructure.project.InstrumentedProjectWorkspacePreparer;
import com.mlisows.testgen.infrastructure.project.MavenProjectClasspath;
import com.mlisows.testgen.infrastructure.project.MavenProjectLayout;
import com.mlisows.testgen.infrastructure.project.MavenProjectLayoutDetector;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

final class PrepareProfileCommand {
    private static final String DEFAULT_WORK_DIRECTORY = "target/testgen-profile-work";
    private static final String DEFAULT_PROFILE_OUTPUT_DIRECTORY = "profile-output";

    private final MavenProjectLayoutDetector layoutDetector;
    private final MavenProjectClasspath classpathResolver;

    PrepareProfileCommand() {
        this(new MavenProjectLayoutDetector(), new MavenProjectClasspath());
    }

    PrepareProfileCommand(
            MavenProjectLayoutDetector layoutDetector,
            MavenProjectClasspath classpathResolver
    ) {
        this.layoutDetector = Objects.requireNonNull(layoutDetector, "layoutDetector must not be null");
        this.classpathResolver = Objects.requireNonNull(classpathResolver, "classpathResolver must not be null");
    }

    void run(String[] args) {
        if (args.length < 2 || args.length > 3) {
            System.out.println("Usage: testgen prepare-profile <maven-project-root> [work-root]");
            return;
        }

        Path projectRoot = Path.of(args[1]);
        Path workRoot = workRoot(args, projectRoot);
        InstrumentedProjectWorkspace workspace = prepareWorkspace(projectRoot, workRoot);
        Path profileOutputDir = workRoot.resolve(DEFAULT_PROFILE_OUTPUT_DIRECTORY);

        printPreparedWorkspace(workspace, profileOutputDir);
    }

    private Path workRoot(String[] args, Path projectRoot) {
        if (args.length == 3) {
            return Path.of(args[2]);
        }

        return projectRoot.resolve(DEFAULT_WORK_DIRECTORY);
    }

    private InstrumentedProjectWorkspace prepareWorkspace(Path projectRoot, Path workRoot) {
        MavenProjectLayout layout = layoutDetector.detect(projectRoot);
        String classpath = classpathResolver.resolve(layout);

        return new InstrumentedProjectWorkspacePreparer(
                new SourceDirectoryInstrumenter(new JavaParserArgumentInstrumenter()),
                new JavaSourceCompiler(classpath),
                classpath
        ).prepare(
                layout,
                workRoot,
                Map.of()
        );
    }

    private void printPreparedWorkspace(InstrumentedProjectWorkspace workspace, Path profileOutputDir) {
        System.out.println("Instrumented sources written to: " + workspace.getSourceRoot());
        System.out.println("Instrumented classes written to: " + workspace.getClassesRoot());
        System.out.println("Runtime profile output directory: " + profileOutputDir);
        System.out.println("Observed profile will be written to: " + profileOutputDir.resolve("observed-profile.txt"));
        System.out.println("Run instrumented application with:");
        System.out.println("java -Dtestgen.profile.dir=" + profileOutputDir + " -cp \"" + runtimeClasspath(workspace) + "\" <main-class>");
    }

    private String runtimeClasspath(InstrumentedProjectWorkspace workspace) {
        return JavaSourceCompiler.joinClasspaths(
                workspace.getClassesRoot().toString(),
                workspace.getClasspath()
        );
    }
}
