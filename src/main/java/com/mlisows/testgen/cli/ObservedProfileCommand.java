package com.mlisows.testgen.cli;

import com.mlisows.testgen.infrastructure.execution.JavaSourceCompiler;
import com.mlisows.testgen.infrastructure.instrumentation.JavaParserArgumentInstrumenter;
import com.mlisows.testgen.infrastructure.instrumentation.SourceDirectoryInstrumenter;
import com.mlisows.testgen.infrastructure.project.InstrumentedProjectWorkspace;
import com.mlisows.testgen.infrastructure.project.InstrumentedProjectWorkspacePreparer;
import com.mlisows.testgen.infrastructure.project.MavenProjectClasspath;
import com.mlisows.testgen.infrastructure.project.MavenProjectLayout;
import com.mlisows.testgen.infrastructure.project.MavenProjectLayoutDetector;
import com.mlisows.testgen.infrastructure.runtime.ObservedProfileMainRunner;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

final class ObservedProfileCommand {
    private final MavenProjectLayoutDetector layoutDetector;
    private final MavenProjectClasspath classpathResolver;
    private final ObservedProfileMainRunner mainRunner;

    ObservedProfileCommand() {
        this(
                new MavenProjectLayoutDetector(),
                new MavenProjectClasspath(),
                new ObservedProfileMainRunner()
        );
    }

    ObservedProfileCommand(
            MavenProjectLayoutDetector layoutDetector,
            MavenProjectClasspath classpathResolver,
            ObservedProfileMainRunner mainRunner
    ) {
        this.layoutDetector = Objects.requireNonNull(layoutDetector, "layoutDetector must not be null");
        this.classpathResolver = Objects.requireNonNull(classpathResolver, "classpathResolver must not be null");
        this.mainRunner = Objects.requireNonNull(mainRunner, "mainRunner must not be null");
    }

    void runProfile(String[] args) {
        if (args.length < 3 || args.length > 5) {
            System.out.println("Usage: testgen profile <maven-project-root> <main-class> [work-root] [output-profile]");
            return;
        }

        Path projectRoot = Path.of(args[1]);
        String mainClassName = args[2];
        Path workRoot = args.length >= 4
                ? Path.of(args[3])
                : projectRoot.resolve("target/testgen-profile-work");
        Path outputProfile = args.length == 5
                ? Path.of(args[4])
                : workRoot.resolve("observed-profile.txt");

        generateObservedProfile(projectRoot, workRoot, mainClassName, new String[]{}, outputProfile);

        System.out.println("Observed profile written to: " + outputProfile);
    }

    void runPrepare(String[] args) {
        if (args.length < 2 || args.length > 3) {
            System.out.println("Usage: testgen prepare-profile <maven-project-root> [work-root]");
            return;
        }

        Path projectRoot = Path.of(args[1]);
        Path workRoot = args.length == 3
                ? Path.of(args[2])
                : projectRoot.resolve("target/testgen-profile-work");

        InstrumentedProjectWorkspace workspace = prepareObservedProfileWorkspace(projectRoot, workRoot);
        Path profileOutputDir = workRoot.resolve("profile-output");
        String runtimeClasspath = JavaSourceCompiler.joinClasspaths(
                workspace.getClassesRoot().toString(),
                workspace.getClasspath()
        );

        System.out.println("Instrumented sources written to: " + workspace.getSourceRoot());
        System.out.println("Instrumented classes written to: " + workspace.getClassesRoot());
        System.out.println("Runtime profile output directory: " + profileOutputDir);
        System.out.println("Run instrumented application with:");
        System.out.println("java -Dtestgen.profile.dir=" + profileOutputDir + " -cp \"" + runtimeClasspath + "\" <main-class>");
    }

    Path generateObservedProfile(
            Path projectRoot,
            Path workRoot,
            String mainClassName,
            String[] applicationArgs,
            Path outputProfile
    ) {
        Objects.requireNonNull(projectRoot, "projectRoot must not be null");
        Objects.requireNonNull(workRoot, "workRoot must not be null");
        Objects.requireNonNull(mainClassName, "mainClassName must not be null");
        Objects.requireNonNull(applicationArgs, "applicationArgs must not be null");
        Objects.requireNonNull(outputProfile, "outputProfile must not be null");

        InstrumentedProjectWorkspace workspace = prepareObservedProfileWorkspace(projectRoot, workRoot);

        mainRunner.run(
                workspace,
                mainClassName,
                applicationArgs,
                outputProfile
        );

        return outputProfile;
    }

    private InstrumentedProjectWorkspace prepareObservedProfileWorkspace(Path projectRoot, Path workRoot) {
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
}
