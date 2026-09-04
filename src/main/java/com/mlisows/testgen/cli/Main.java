package com.mlisows.testgen.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import com.mlisows.testgen.domain.execution.TestCandidateExecutionResult;
import com.mlisows.testgen.domain.generation.ArgumentValueHint;
import com.mlisows.testgen.domain.generation.MethodGenerationPlan;
import com.mlisows.testgen.domain.profile.ObservedProfile;
import com.mlisows.testgen.domain.structure.ClassStructure;
import com.mlisows.testgen.domain.structure.ProjectClassStructureIndex;
import com.mlisows.testgen.domain.structure.ProjectTypeIndex;
import com.mlisows.testgen.domain.structure.TypeInfo;
import com.mlisows.testgen.domain.structure.TypeKind;
import com.mlisows.testgen.domain.analysis.ClassAnalysisResult;
import com.mlisows.testgen.domain.analysis.CoverageGoal;
import com.mlisows.testgen.infrastructure.execution.JavaSourceCompiler;
import com.mlisows.testgen.infrastructure.execution.ReflectionTestCandidateExecutor;
import com.mlisows.testgen.infrastructure.instrumentation.JavaParserArgumentInstrumenter;
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

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("prepare-profile")) {
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
        Path workRoot = getWorkRoot(projectRoot, args);
        Path observedProfilePath = observedProfilePath(args);

        printGenerationStart(projectRoot, workRoot, observedProfilePath);

        MavenProjectLayoutDetector layoutDetector = new MavenProjectLayoutDetector();
        MavenProjectLayout projectLayout = layoutDetector.detect(projectRoot);

        Path mainSourceRoot = projectLayout.getMainSourceRoot();
        List<Path> javaSources = findJavaSources(mainSourceRoot);

        JavaParserTypeIndexAnalyzer typeIndexAnalyzer = new JavaParserTypeIndexAnalyzer();
        ProjectTypeIndex typeIndex = typeIndexAnalyzer.analyze(javaSources);

        List<Path> classSources = findOnlyClassSources(javaSources, typeIndex);

        Map<Path, ClassAnalysisResult> analysisResultsByPath = analyzeClasses(classSources);

        List<ClassStructure> classStructures = analyzeClassStructures(classSources);

        ProjectClassStructureIndex classIndex = new ProjectClassStructureIndex(classStructures);

        MethodGenerationPlanner methodGenerationPlanner = new MethodGenerationPlanner();
        List<MethodGenerationPlan> methodPlans = planMethods(
                classStructures,
                typeIndex,
                methodGenerationPlanner
        );

        ObservedProfile observedProfile = readObservedProfile(observedProfilePath);

        Map<Path, List<CoverageGoal>> coverageGoalsByPath = coverageGoalsByPath(analysisResultsByPath);


        String projectClasspath = new MavenProjectClasspath().resolve(projectLayout);

        InstrumentedProjectWorkspacePreparer workspacePreparer = new InstrumentedProjectWorkspacePreparer(
                new SourceDirectoryInstrumenter(new JavaParserBranchInstrumenter()),
                new JavaSourceCompiler(projectClasspath),
                projectClasspath
        );

        InstrumentedProjectWorkspace workspace = workspacePreparer.prepare(
                projectLayout,
                workRoot,
                coverageGoalsByPath
        );

        List<ArgumentValueHint> argumentValueHints = new ArrayList<>();

        for (ClassAnalysisResult analysisResult : analysisResultsByPath.values()) {
            argumentValueHints.addAll(analysisResult.getArgumentValueHints());
        }
        argumentValueHints.addAll(observedProfile.getArgumentValueHints());


        CandidateSpaceFactory candidateSpaceFactory = new CandidateSpaceFactory();
        CandidateVariantGenerator candidateVariantGenerator = new CandidateVariantGenerator(10, 50);
        ReflectionTestCandidateExecutor candidateExecutor = new ReflectionTestCandidateExecutor(workspace);
        CandidateCoverageEvaluator candidateCoverageEvaluator = new CandidateCoverageEvaluator(candidateExecutor);

        EvaluateProjectCandidatesUseCase evaluateProjectCandidatesUseCase = new EvaluateProjectCandidatesUseCase(
                methodGenerationPlanner,
                candidateSpaceFactory,
                candidateVariantGenerator,
                candidateCoverageEvaluator
        );

        CandidateCoverageEvaluation coverageEvaluation = evaluateProjectCandidatesUseCase.evaluate(
                classStructures,
                methodPlans,
                typeIndex,
                classIndex,
                argumentValueHints
        );


        List<TestCandidateExecutionResult> selectedResults = coverageEvaluation.getSelectedResults();

        Path outputRoot = workRoot.resolve("generated-tests");

        GeneratedCandidateTestFileWriter testFileWriter = new GeneratedCandidateTestFileWriter();
        testFileWriter.write(outputRoot, selectedResults);

        System.out.println("Generated tests: " + outputRoot);

        writeReport(workRoot, analysisResultsByPath, methodPlans, coverageEvaluation);
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

        MavenProjectLayoutDetector layoutDetector = new MavenProjectLayoutDetector();
        MavenProjectLayout projectLayout = layoutDetector.detect(projectRoot);

        MavenProjectClasspath projectClasspathResolver = new MavenProjectClasspath();
        String projectClasspath = projectClasspathResolver.resolve(projectLayout);

        JavaParserArgumentInstrumenter argumentInstrumenter = new JavaParserArgumentInstrumenter();
        SourceDirectoryInstrumenter sourceDirectoryInstrumenter = new SourceDirectoryInstrumenter(argumentInstrumenter);
        JavaSourceCompiler sourceCompiler = new JavaSourceCompiler(projectClasspath);

        InstrumentedProjectWorkspacePreparer workspacePreparer = new InstrumentedProjectWorkspacePreparer(
                sourceDirectoryInstrumenter,
                sourceCompiler,
                projectClasspath
        );

        InstrumentedProjectWorkspace workspace = workspacePreparer.prepare(
                projectLayout,
                workRoot,
                Map.of()
        );

        printProfileRunInstructions(workRoot, workspace);
    }

    private static Map<Path, ClassAnalysisResult> analyzeClasses(List<Path> classSources) {
        Map<Path, ClassAnalysisResult> analysisResultsByPath = new LinkedHashMap<>();
        CodeAnalyzer codeAnalyzer = new JavaParserCodeAnalyzer();

        for (Path classSource : classSources) {
            ClassAnalysisResult analysisResult = codeAnalyzer.analyze(classSource);
            analysisResultsByPath.put(classSource, analysisResult);
        }

        return analysisResultsByPath;
    }

    private static List<ClassStructure> analyzeClassStructures(List<Path> classSources) {
        List<ClassStructure> classStructures = new ArrayList<>();
        ClassStructureAnalyzer classStructureAnalyzer = new JavaParserClassStructureAnalyzer();

        for (Path classSource : classSources) {
            ClassStructure classStructure = classStructureAnalyzer.analyze(classSource);
            classStructures.add(classStructure);
        }

        return classStructures;
    }

    private static Map<Path, List<CoverageGoal>> coverageGoalsByPath(
            Map<Path, ClassAnalysisResult> analysisResultsByPath
    ) {
        Map<Path, List<CoverageGoal>> coverageGoalsByPath = new LinkedHashMap<>();

        for (Map.Entry<Path, ClassAnalysisResult> entry : analysisResultsByPath.entrySet()) {
            coverageGoalsByPath.put(entry.getKey(), entry.getValue().getCoverageGoals());
        }

        return coverageGoalsByPath;
    }

    private static void printProfileRunInstructions(
            Path workRoot,
            InstrumentedProjectWorkspace workspace
    ) {
        Path profilePath = workRoot.resolve("observed-profile.txt");
        String runClasspath = JavaSourceCompiler.joinClasspaths(
                workspace.getClassesRoot().toString(),
                workspace.getClasspath()
        );

        System.out.println("Instrumented sources: " + workspace.getSourceRoot());
        System.out.println("Instrumented classes: " + workspace.getClassesRoot());
        System.out.println("Observed profile output: " + profilePath);
        System.out.println("Run instrumented application with:");
        System.out.println("java -Dtestgen.profile.dir="
                + workRoot
                + " -cp \""
                + runClasspath
                + "\" <main-class>");
    }

    private static ObservedProfile readObservedProfile(Path observedProfilePath) {
        if (observedProfilePath == null) {
            System.out.println("Observed profile: skipped");
            return new ObservedProfile(List.of(), List.of());
        }

        ObservedProfileReader observedProfileReader = new TextObservedProfileReader();
        ObservedProfile observedProfile = observedProfileReader.read(observedProfilePath);

        System.out.println("Observed profile hints: " + observedProfile.getArgumentValueHints().size());
        System.out.println("Observed invocations: " + observedProfile.getObservedInvocations().size());

        return observedProfile;
    }

    private static Path getWorkRoot(Path projectRoot, String[] args) {
        if (args.length >= 2) {
            return Path.of(args[1]);
        }

        return projectRoot.resolve(DEFAULT_TEST_WORK_DIRECTORY);
    }

    private static Path observedProfilePath(String[] args) {
        if (args.length == 3) {
            return Path.of(args[2]);
        }

        return null;
    }

    private static void printGenerationStart(
            Path projectRoot,
            Path workRoot,
            Path observedProfilePath
    ) {
        System.out.println("Generating tests");
        System.out.println("Project root: " + projectRoot);
        System.out.println("Work root: " + workRoot);

        if (observedProfilePath != null) {
            System.out.println("Observed profile: " + observedProfilePath);
        }
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

    private static void writeReport(
            Path workRoot,
            Map<Path, ClassAnalysisResult> analysisResultsByPath,
            List<MethodGenerationPlan> methodPlans,
            CandidateCoverageEvaluation coverageEvaluation
    ) {
        GenerationReport report = GenerationReport.from(
                List.copyOf(analysisResultsByPath.values()),
                methodPlans,
                coverageEvaluation
        );

        Path reportPath = workRoot.resolve("report.txt");

        try {
            Files.createDirectories(workRoot);
            Files.writeString(reportPath, report.toText());
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write report: " + reportPath, exception);
        }

        System.out.println("Report: " + reportPath);
    }

    private static List<MethodGenerationPlan> planMethods(
            List<ClassStructure> classStructures,
            ProjectTypeIndex typeIndex,
            MethodGenerationPlanner methodGenerationPlanner
    ) {
        List<MethodGenerationPlan> methodPlans = new ArrayList<>();

        for (ClassStructure classStructure : classStructures) {
            methodPlans.addAll(methodGenerationPlanner.plan(classStructure, typeIndex));
        }

        return methodPlans;
    }

}

