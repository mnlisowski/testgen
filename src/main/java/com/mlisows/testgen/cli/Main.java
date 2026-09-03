package com.mlisows.testgen.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.mlisows.testgen.domain.ClassAnalysisResult;
import com.mlisows.testgen.domain.ProjectTypeIndex;
import com.mlisows.testgen.domain.TypeInfo;
import com.mlisows.testgen.domain.TypeKind;
import com.mlisows.testgen.infrastructure.parser.JavaParserCodeAnalyzer;
import com.mlisows.testgen.infrastructure.parser.JavaParserTypeIndexAnalyzer;
import com.mlisows.testgen.infrastructure.project.MavenProjectLayout;
import com.mlisows.testgen.infrastructure.project.MavenProjectLayoutDetector;
import com.mlisows.testgen.usecase.ports.CodeAnalyzer;

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

        analyzeBranchesAndStaticArgumentHints();
        analyzeClassStructures();
        planSupportedMethods();
        readObservedProfile(observedProfilePath);
        prepareBranchCoverageWorkspace(workRoot);
        generateCandidateSpaces();
        generateCandidateVariants();
        executeCandidatesAndMeasureCoverage();
        writeGeneratedTests(workRoot);
        writeReport(workRoot);
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

