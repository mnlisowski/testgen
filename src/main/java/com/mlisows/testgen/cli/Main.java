package com.mlisows.testgen.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import com.mlisows.testgen.domain.*;
import com.mlisows.testgen.infrastructure.execution.JavaSourceCompiler;
import com.mlisows.testgen.infrastructure.execution.ReflectionTestCandidateExecutor;
import com.mlisows.testgen.infrastructure.instrumentation.JavaParserBranchInstrumenter;
import com.mlisows.testgen.infrastructure.instrumentation.SourceDirectoryInstrumenter;
import com.mlisows.testgen.infrastructure.parser.JavaParserClassStructureAnalyzer;
import com.mlisows.testgen.infrastructure.parser.JavaParserCodeAnalyzer;
import com.mlisows.testgen.infrastructure.parser.JavaParserTypeIndexAnalyzer;
import com.mlisows.testgen.infrastructure.project.*;
import com.mlisows.testgen.infrastructure.runtime.TextObservedProfileReader;
import com.mlisows.testgen.infrastructure.writer.GeneratedCandidateTestFileWriter;
import com.mlisows.testgen.usecase.*;
import com.mlisows.testgen.usecase.ports.ClassStructureAnalyzer;
import com.mlisows.testgen.usecase.ports.CodeAnalyzer;
import com.mlisows.testgen.usecase.ports.ObservedProfileReader;

public final class Main {
    private static final String GENERATE_TESTS_USAGE =
            "Usage: testgen <maven-project-root> [work-root] [observed-profile]";
    private static final String PREPARE_PROFILE_USAGE =
            "usage: testgen prepare-profile <maven-project-root> <work-root>";
    private static final String DEFAULT_TEST_WORK_DIRECTORY = "target/testgen-work";
    private static final String DEFAULT_PROFILE_WORK_DIRECTORY = "target/testgen-profile-work";

    private static void Main(String[] args) {
        if (args.length > 0 && args[0].equals("prepare-profile") {
            prepareProfile(args);
            return;
        }

        generateTests(args);
    }

    private static void generateTests(String[] args) {
        if (args.length < 1 || args.length > 3) {
            System.out.println(GENERATE_TESTS_USAGE);
            return;
        }

        Path projectRoot = Path.of(args[0]);
        Path workRoot = args.length >= 2
                ? Path.of(args[1])
                : projectRoot.resolve(DEFAULT_TEST_WORK_DIRECTORY);
        Path observedProfilePath = args.length == 3
                ? Path.of(args[2])
                : null;

        System.out.println("Generating tests");
        System.out.println("Project root: " + projectRoot);
        System.out.println("Work root: " + workRoot);

        if (observedProfilePath != null) {
            System.out.println("Observed profile: " + observedProfilePath);
        }
        MavenProjectLayout projectLayout = new MavenProjectLayoutDetector().detect(projectRoot);
        Path mainSourceRoot = projectLayout.getMainSourceRoot();
        List<Path> javaSources = findJavaSources(mainSourceRoot);
        ProjectTypeIndex typeIndex = new JavaParserTypeIndexAnalyzer().analyze(javaSources);
        List<Path> classSources = findOnlyClassSources(javaSources, typeIndex);

        Map<Path, ClassAnalysisResult> analysisResultsByPath = new LinkedHashMap<>();
        CodeAnalyzer codeAnalyzer = new JavaParserCodeAnalyzer();

        for (Path classSource : classSources) {
            ClassAnalysisResult analysisResult = codeAnalyzer.analyze(classSource);
            analysisResultsByPath.put(classSource, analysisResult);
        }

        System.out.println("Classes analyzed: " + analysisResultsByPath.size());

        System.out.println("Classes analyzed: " + analysisResultsByPath.size());

        List<ClassStructure> classStructures = new ArrayList<>();
        ClassStructureAnalyzer classStructureAnalyzer = new JavaParserClassStructureAnalyzer();

        for (Path classSource : classSources) {
            ClassStructure classStructure = classStructureAnalyzer.analyze(classSource);
            classStructures.add(classStructure);
        }

        System.out.println("Class structures analyzed: " + classStructures.size());

        ProjectClassStructureIndex classIndex = new ProjectClassStructureIndex(classStructures);

        MethodGenerationPlanner methodGenerationPlanner = new MethodGenerationPlanner();
        List<MethodGenerationPlan> methodPlans = new ArrayList<>();

        for (ClassStructure classStructure : classStructures) {
            methodPlans.addAll(methodGenerationPlanner.plan(classStructure, typeIndex));
        }

        long supportedMethods = methodPlans.stream()
                .filter(methodGenerationPlanner::isSupported)
                .count();

        System.out.println("Methods analyzed: " + methodPlans.size());
        System.out.println("Supported methods: " + supportedMethods);

        ObservedProfile observedProfile;

        if (observedProfilePath == null) {
            observedProfile = new ObservedProfile(List.of(), List.of());
            System.out.println("Observed profile: skipped");
        } else {
            ObservedProfileReader observedProfileReader = new TextObservedProfileReader();
            observedProfile = observedProfileReader.read(observedProfilePath);
            System.out.println("Observed profile hints: " + observedProfile.getArgumentValueHints().size());
            System.out.println("Observed invocations: " + observedProfile.getObservedInvocations().size());
        }

        Map<Path, List<CoverageGoal>> coverageGoalsByPath = new LinkedHashMap<>();

        for (Map.Entry<Path, ClassAnalysisResult> entry : analysisResultsByPath.entrySet()) {
            coverageGoalsByPath.put(entry.getKey(), entry.getValue().getCoverageGoals());
        }

        int coverageGoalCount = 0;
        for (List<CoverageGoal> coverageGoals : coverageGoalsByPath.values()) {
            coverageGoalCount += coverageGoals.size();
        }

        String projectClasspath = new MavenProjectClasspath().resolve(projectLayout);

        InstrumentedProjectWorkspace workspace = new InstrumentedProjectWorkspacePreparer(
                new SourceDirectoryInstrumenter(new JavaParserBranchInstrumenter()),
                new JavaSourceCompiler(projectClasspath),
                projectClasspath
        ).prepare(projectLayout, workRoot, coverageGoalsByPath);

        System.out.println("Instrumented sources: " + workspace.getSourceRoot());
        System.out.println("Instrumented classes: " + workspace.getClassesRoot());

        List<ArgumentValueHint> argumentValueHints = new ArrayList<>();

        for (ClassAnalysisResult analysisResult : analysisResultsByPath.values()) {
            argumentValueHints.addAll(analysisResult.getArgumentValueHints());
        }
        argumentValueHints.addAll(observedProfile.getArgumentValueHints());


        EvaluateProjectCandidatesUseCase evaluateProjectCandidatesUseCase = new EvaluateProjectCandidatesUseCase(
                methodGenerationPlanner,
                new CandidateSpaceFactory(),
                new CandidateVariantGenerator(10, 50),
                new CandidateCoverageEvaluator(new ReflectionTestCandidateExecutor(workspace))
        );

        CandidateCoverageEvaluation coverageEvaluation = evaluateProjectCandidatesUseCase.evaluate(
                classStructures,
                methodPlans,
                typeIndex,
                classIndex,
                argumentValueHints
        );

        List<TestCandidateExecutionResult> selectedResults = coverageEvaluation.getSelectedResults();
        System.out.println("Executed candidates: " + coverageEvaluation.getExecutedResults().size());
        System.out.println("Selected candidates: " + coverageEvaluation.getSelectedResults().size());

        Path outputRoot = workRoot.resolve("generated-tests");

        new GeneratedCandidateTestFileWriter().write(outputRoot, selectedResults);

        System.out.println("Generated tests: " + outputRoot);

    }

    private static void prepareProfile(String[] args) {
        if (args.length < 2 || args.length > 3) {
            System.out.println(PREPARE_PROFILE_USAGE);
            return;
        }

        Path projectRoot = Path.of(args[1]);
        Path workRoot = args.length == 3
                ? Path.of(args[2])
                : projectRoot.resolve(DEFAULT_PROFILE_WORK_DIRECTORY);

        System.out.println("Preparing profile workspace");
        System.out.println("Project root: " + projectRoot);
        System.out.println("Work root: " + workRoot);

        detectMavenProject(projectRoot);
        findJavaSources(projectRoot);
        resolveProjectClasspath();
        instrumentSourcesForArgumentProfiling(workRoot);
        compileInstrumentedSources(workRoot);
        printProfileRunInstructions(workRoot);
    }

    private static void detectMavenProject(Path projectRoot) {
        printStep("Detect Maven project: " + projectRoot);
    }

    private static List<Path> findJavaSources(Path sourceRoot) {
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read source directory: " + sourceRoot, exception);
        }
    }

    private static List<Path> findOnlyClassSources(List<Path> javaSources, ProjectTypeIndex typeIndex) {
        return javaSources.stream()
                .filter(sourcePath -> typeIndex.findByName(sourceTypeName(sourcePath))
                        .map(TypeInfo::getKind)
                        .filter(kind -> kind == TypeKind.CLASS)
                        .isPresent())
                .toList();
    }

    private static String sourceTypeName(Path sourcePath) {
        String fileName = sourcePath.getFileName().toString();

        if (!fileName.endsWith(".java")) {
            return fileName;
        }

        return fileName.substring(0, fileName.length() - ".java".length());
    }


    private static void analyzeProjectTypes() {
        printStep("Analyze project types");
    }

    private static void analyzeBranchesAndStaticArgumentHints() {
        printStep("Analyze branch goals and static argument hints");
    }

    private static void analyzeClassStructures() {
        printStep("Analyze constructors, methods, and parameters");
    }

    private static void planSupportedMethods() {
        printStep("Plan methods that can be generated");
    }

    private static void readObservedProfile(Path observedProfilePath) {
        if (observedProfilePath == null) {
            printStep("Skip observed runtime profile");
            return;
        }

        printStep("Read observed runtime profile: " + observedProfilePath);
    }

    private static void prepareBranchCoverageWorkspace(Path workRoot) {
        printStep("Prepare instrumented workspace for branch coverage: " + workRoot);
    }

    private static void generateCandidateSpaces() {
        printStep("Create candidate value spaces");
    }

    private static void generateCandidateVariants() {
        printStep("Generate candidate variants");
    }

    private static void executeCandidatesAndMeasureCoverage() {
        printStep("Execute candidates and measure branch coverage");
    }

    private static void writeGeneratedTests(Path workRoot) {
        printStep("Write generated tests: " + workRoot);
    }

    private static void writeReport(Path workRoot) {
        printStep("Write generation report: " + workRoot);
    }

    private static void resolveProjectClasspath() {
        printStep("Resolve project classpath");
    }

    private static void instrumentSourcesForArgumentProfiling(Path workRoot) {
        printStep("Instrument sources for runtime argument profiling: " + workRoot);
    }

    private static void compileInstrumentedSources(Path workRoot) {
        printStep("Compile instrumented sources: " + workRoot);
    }

    private static void printProfileRunInstructions(Path workRoot) {
        printStep("Print command for running the instrumented application: " + workRoot);
    }

    private static void printStep(String text) {
        System.out.println("- " + text);
    }
}

