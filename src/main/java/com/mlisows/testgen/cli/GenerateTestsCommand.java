package com.mlisows.testgen.cli;

import com.mlisows.testgen.domain.ObservedProfile;
import com.mlisows.testgen.infrastructure.execution.JavaSourceCompiler;
import com.mlisows.testgen.infrastructure.execution.ReflectionTestCandidateExecutor;
import com.mlisows.testgen.infrastructure.instrumentation.JavaParserBranchInstrumenter;
import com.mlisows.testgen.infrastructure.instrumentation.SourceDirectoryInstrumenter;
import com.mlisows.testgen.infrastructure.project.InstrumentedProjectWorkspace;
import com.mlisows.testgen.infrastructure.project.InstrumentedProjectWorkspacePreparer;
import com.mlisows.testgen.infrastructure.project.MavenProjectClasspath;
import com.mlisows.testgen.infrastructure.runtime.TextObservedProfileReader;
import com.mlisows.testgen.usecase.ports.ObservedProfileReader;
import com.mlisows.testgen.infrastructure.writer.GeneratedCandidateTestFileWriter;
import com.mlisows.testgen.usecase.CandidateCoverageEvaluation;
import com.mlisows.testgen.usecase.CandidateCoverageEvaluator;
import com.mlisows.testgen.usecase.CandidateGenerationUseCase;
import com.mlisows.testgen.usecase.CandidateSpaceFactory;
import com.mlisows.testgen.usecase.CandidateVariantGenerator;
import com.mlisows.testgen.usecase.GenerationReport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

final class GenerateTestsCommand {
    private static final int MAX_VALUES_PER_SLOT = 10;
    private static final int MAX_CANDIDATES_PER_METHOD = 50;
    private static final String GENERATED_TESTS_DIRECTORY = "generated-tests";

    private final ProjectAnalysisLoader analysisLoader;
    private final MavenProjectClasspath classpathResolver;
    private final GeneratedCandidateTestFileWriter testFileWriter;
    private final ObservedProfileReader observedProfileReader;

    GenerateTestsCommand() {
        this(
                new ProjectAnalysisLoader(),
                new MavenProjectClasspath(),
                new GeneratedCandidateTestFileWriter(),
                new TextObservedProfileReader()
        );
    }

    GenerateTestsCommand(
            ProjectAnalysisLoader analysisLoader,
            MavenProjectClasspath classpathResolver,
            GeneratedCandidateTestFileWriter testFileWriter,
            ObservedProfileReader observedProfileReader
    ) {
        this.analysisLoader = Objects.requireNonNull(analysisLoader, "analysisLoader must not be null");
        this.classpathResolver = Objects.requireNonNull(classpathResolver, "classpathResolver must not be null");
        this.testFileWriter = Objects.requireNonNull(testFileWriter, "testFileWriter must not be null");
        this.observedProfileReader = Objects.requireNonNull(
                observedProfileReader,
                "observedProfileReader must not be null"
        );
    }

    void run(String[] args) {
        if (args.length < 1 || args.length > 3) {
            System.out.println("Usage: testgen <maven-project-root> [work-root] [observed-profile]");
            return;
        }

        Path projectRoot = Path.of(args[0]);
        Path workRoot = args.length >= 2
                ? Path.of(args[1])
                : projectRoot.resolve("target/testgen-work");
        ObservedProfile observedProfile = args.length == 3
                ? observedProfileReader.read(Path.of(args[2]))
                : emptyObservedProfile();

        GenerationReport report = generateReport(projectRoot, workRoot, observedProfile);
        String reportText = report.toText();
        Path reportPath = workRoot.resolve("report.txt");

        writeText(reportPath, reportText);

        System.out.println(reportText);
        System.out.println("Generation report written to: " + reportPath);
        System.out.println("Generated tests written to: " + workRoot.resolve(GENERATED_TESTS_DIRECTORY));
    }

    GenerationReport generateReport(Path projectRoot, Path workRoot) {
        return generateReport(projectRoot, workRoot, emptyObservedProfile());
    }

    GenerationReport generateReport(
            Path projectRoot,
            Path workRoot,
            ObservedProfile observedProfile
    ) {
        Objects.requireNonNull(projectRoot, "projectRoot must not be null");
        Objects.requireNonNull(workRoot, "workRoot must not be null");
        Objects.requireNonNull(observedProfile, "observedProfile must not be null");

        ProjectAnalysis projectAnalysis = analysisLoader.load(projectRoot);
        InstrumentedProjectWorkspace workspace = prepareBranchCoverageWorkspace(projectAnalysis, workRoot);
        CandidateCoverageEvaluation coverageEvaluation = evaluateCandidates(
                projectAnalysis,
                workspace,
                observedProfile
        );

        testFileWriter.write(
                workRoot.resolve(GENERATED_TESTS_DIRECTORY),
                coverageEvaluation.getSelectedResults()
        );

        return GenerationReport.from(
                projectAnalysis.getAnalysisResults(),
                projectAnalysis.getMethodPlans(),
                coverageEvaluation
        );
    }

    private InstrumentedProjectWorkspace prepareBranchCoverageWorkspace(ProjectAnalysis projectAnalysis, Path workRoot) {
        String classpath = classpathResolver.resolve(projectAnalysis.getLayout());

        return new InstrumentedProjectWorkspacePreparer(
                new SourceDirectoryInstrumenter(new JavaParserBranchInstrumenter()),
                new JavaSourceCompiler(classpath),
                classpath
        ).prepare(
                projectAnalysis.getLayout(),
                workRoot,
                projectAnalysis.coverageGoalsBySourcePath()
        );
    }

    private CandidateCoverageEvaluation evaluateCandidates(
            ProjectAnalysis projectAnalysis,
            InstrumentedProjectWorkspace workspace,
            ObservedProfile observedProfile
    ) {
        return candidateGenerationUseCase(workspace).evaluateProject(
                projectAnalysis.getClassStructures(),
                projectAnalysis.getMethodPlans(),
                projectAnalysis.getTypeIndex(),
                projectAnalysis.getClassIndex(),
                projectAnalysis.argumentValueHints(observedProfile)
        );
    }

    private CandidateGenerationUseCase candidateGenerationUseCase(InstrumentedProjectWorkspace workspace) {
        return new CandidateGenerationUseCase(
                new CandidateSpaceFactory(),
                new CandidateVariantGenerator(MAX_VALUES_PER_SLOT, MAX_CANDIDATES_PER_METHOD),
                new CandidateCoverageEvaluator(new ReflectionTestCandidateExecutor(workspace))
        );
    }

    private ObservedProfile emptyObservedProfile() {
        return new ObservedProfile(List.of(), List.of());
    }

    private void writeText(Path path, String text) {
        try {
            Path parent = path.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(path, text);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write file: " + path, exception);
        }
    }
}
