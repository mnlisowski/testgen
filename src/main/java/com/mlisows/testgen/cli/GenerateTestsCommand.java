package com.mlisows.testgen.cli;

import com.mlisows.testgen.domain.ArgumentValueHint;
import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.MethodGenerationPlan;
import com.mlisows.testgen.domain.MethodModel;
import com.mlisows.testgen.domain.ObservedProfile;
import com.mlisows.testgen.domain.TestCandidateExecutionResult;
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
import com.mlisows.testgen.usecase.MethodGenerationPlanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
        CandidateGenerationUseCase candidateGenerationUseCase = candidateGenerationUseCase(workspace);
        MethodGenerationPlanner planner = new MethodGenerationPlanner();
        List<ArgumentValueHint> argumentValueHints = projectAnalysis.argumentValueHints(observedProfile);
        List<TestCandidateExecutionResult> executedResults = new ArrayList<>();
        List<TestCandidateExecutionResult> selectedResults = new ArrayList<>();

        for (ClassStructure classStructure : projectAnalysis.getClassStructures()) {
            for (MethodModel method : classStructure.getMethods()) {
                Optional<MethodGenerationPlan> plan = methodPlan(
                        projectAnalysis.getMethodPlans(),
                        classStructure.getClassName(),
                        method.getName()
                );

                if (plan.isEmpty() || !planner.isSupported(plan.get())) {
                    continue;
                }

                CandidateCoverageEvaluation evaluation = candidateGenerationUseCase.evaluate(
                        classStructure,
                        method,
                        projectAnalysis.getTypeIndex(),
                        projectAnalysis.getClassIndex(),
                        argumentValueHints
                );

                executedResults.addAll(evaluation.getExecutedResults());
                selectedResults.addAll(evaluation.getSelectedResults());
            }
        }

        return new CandidateCoverageEvaluation(executedResults, selectedResults);
    }

    private CandidateGenerationUseCase candidateGenerationUseCase(InstrumentedProjectWorkspace workspace) {
        return new CandidateGenerationUseCase(
                new CandidateSpaceFactory(),
                new CandidateVariantGenerator(MAX_VALUES_PER_SLOT, MAX_CANDIDATES_PER_METHOD),
                new CandidateCoverageEvaluator(new ReflectionTestCandidateExecutor(workspace))
        );
    }

    private Optional<MethodGenerationPlan> methodPlan(
            List<MethodGenerationPlan> methodPlans,
            String className,
            String methodName
    ) {
        return methodPlans.stream()
                .filter(plan -> plan.getClassName().equals(className))
                .filter(plan -> plan.getMethodName().equals(methodName))
                .findFirst();
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
