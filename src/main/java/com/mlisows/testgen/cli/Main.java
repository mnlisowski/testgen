package com.mlisows.testgen.cli;

import com.mlisows.testgen.domain.ClassAnalysisResult;
import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.CoverageGoal;
import com.mlisows.testgen.domain.MethodGenerationPlan;
import com.mlisows.testgen.domain.MethodModel;
import com.mlisows.testgen.domain.ObservedProfile;
import com.mlisows.testgen.domain.ProjectClassStructureIndex;
import com.mlisows.testgen.domain.ProjectTypeIndex;
import com.mlisows.testgen.domain.ArgumentValueHint;
import com.mlisows.testgen.domain.TestCandidateExecutionResult;
import com.mlisows.testgen.domain.TypeInfo;
import com.mlisows.testgen.domain.TypeKind;
import com.mlisows.testgen.infrastructure.execution.JavaSourceCompiler;
import com.mlisows.testgen.infrastructure.execution.ReflectionTestCandidateExecutor;
import com.mlisows.testgen.infrastructure.instrumentation.JavaParserBranchInstrumenter;
import com.mlisows.testgen.infrastructure.instrumentation.SourceDirectoryInstrumenter;
import com.mlisows.testgen.infrastructure.parser.JavaParserClassStructureAnalyzer;
import com.mlisows.testgen.infrastructure.parser.JavaParserCodeAnalyzer;
import com.mlisows.testgen.infrastructure.parser.JavaParserTypeIndexAnalyzer;
import com.mlisows.testgen.infrastructure.project.InstrumentedProjectWorkspace;
import com.mlisows.testgen.infrastructure.project.InstrumentedProjectWorkspacePreparer;
import com.mlisows.testgen.infrastructure.project.MavenDependencyClasspathResolver;
import com.mlisows.testgen.infrastructure.project.MavenProjectLayout;
import com.mlisows.testgen.infrastructure.project.MavenProjectLayoutDetector;
import com.mlisows.testgen.infrastructure.runtime.TextObservedProfileReader;
import com.mlisows.testgen.usecase.CandidateCoverageEvaluation;
import com.mlisows.testgen.usecase.CandidateCoverageEvaluator;
import com.mlisows.testgen.usecase.CandidateGenerationUseCase;
import com.mlisows.testgen.usecase.CandidateSpaceFactory;
import com.mlisows.testgen.usecase.CandidateVariantGenerator;
import com.mlisows.testgen.usecase.GeneratableCoverageGoalSelector;
import com.mlisows.testgen.usecase.GenerationReport;
import com.mlisows.testgen.usecase.MethodGenerationPlanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class Main {
    private static final int MAX_VALUES_PER_SLOT = 10;
    private static final int MAX_CANDIDATES_PER_METHOD = 50;

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 3) {
            System.out.println("Usage: testgen <maven-project-root> [work-root] [observed-profile]");
            return;
        }

        Path projectRoot = Path.of(args[0]);
        Path workRoot = args.length >= 2
                ? Path.of(args[1])
                : projectRoot.resolve("target/testgen-work");

        ObservedProfile observedProfile = args.length == 3
                ? new TextObservedProfileReader().read(Path.of(args[2]))
                : new ObservedProfile(List.of(), List.of());

        GenerationReport report = generateReport(projectRoot, workRoot, observedProfile);
        System.out.println(report.toText());
    }

    static GenerationReport generateReport(Path projectRoot, Path workRoot) {
        return generateReport(projectRoot, workRoot, new ObservedProfile(List.of(), List.of()));
    }

    static GenerationReport generateReport(
            Path projectRoot,
            Path workRoot,
            ObservedProfile observedProfile
    ) {
        Objects.requireNonNull(projectRoot, "projectRoot must not be null");
        Objects.requireNonNull(workRoot, "workRoot must not be null");
        Objects.requireNonNull(observedProfile, "observedProfile must not be null");

        MavenProjectLayout layout = new MavenProjectLayoutDetector().detect(projectRoot);
        List<Path> sourcePaths = sourcePaths(layout.getMainSourceRoot());
        ProjectTypeIndex typeIndex = new JavaParserTypeIndexAnalyzer().analyze(sourcePaths);
        List<Path> classSourcePaths = classSourcePaths(sourcePaths, typeIndex);

        JavaParserCodeAnalyzer codeAnalyzer = new JavaParserCodeAnalyzer();
        JavaParserClassStructureAnalyzer classStructureAnalyzer = new JavaParserClassStructureAnalyzer();

        Map<Path, ClassAnalysisResult> analysisResultsByPath = new LinkedHashMap<>();
        for (Path sourcePath : classSourcePaths) {
            analysisResultsByPath.put(sourcePath, codeAnalyzer.analyze(sourcePath));
        }

        List<ClassStructure> classStructures = classSourcePaths.stream()
                .map(classStructureAnalyzer::analyze)
                .toList();
        ProjectClassStructureIndex classIndex = new ProjectClassStructureIndex(classStructures);
        List<MethodGenerationPlan> methodPlans = methodPlans(classStructures, typeIndex);

        String classpath = classpathFor(layout);
        InstrumentedProjectWorkspace workspace = new InstrumentedProjectWorkspacePreparer(
                new SourceDirectoryInstrumenter(new JavaParserBranchInstrumenter()),
                new JavaSourceCompiler(classpath),
                classpath
        ).prepare(
                layout,
                workRoot,
                coverageGoalsBySourcePath(analysisResultsByPath)
        );

        CandidateGenerationUseCase candidateGenerationUseCase = new CandidateGenerationUseCase(
                new CandidateSpaceFactory(),
                new CandidateVariantGenerator(MAX_VALUES_PER_SLOT, MAX_CANDIDATES_PER_METHOD),
                new CandidateCoverageEvaluator(new ReflectionTestCandidateExecutor(workspace))
        );

        CandidateCoverageEvaluation coverageEvaluation = evaluateCandidates(
                classStructures,
                methodPlans,
                typeIndex,
                classIndex,
                argumentValueHints(analysisResultsByPath, observedProfile),
                candidateGenerationUseCase
        );

        return GenerationReport.from(
                List.copyOf(analysisResultsByPath.values()),
                methodPlans,
                coverageEvaluation
        );
    }

    private static String classpathFor(MavenProjectLayout layout) {
        return JavaSourceCompiler.joinClasspaths(
                JavaSourceCompiler.defaultClasspath(),
                new MavenDependencyClasspathResolver().resolve(layout)
        );
    }

    private static CandidateCoverageEvaluation evaluateCandidates(
            List<ClassStructure> classStructures,
            List<MethodGenerationPlan> methodPlans,
            ProjectTypeIndex typeIndex,
            ProjectClassStructureIndex classIndex,
            List<ArgumentValueHint> argumentValueHints,
            CandidateGenerationUseCase candidateGenerationUseCase
    ) {
        GeneratableCoverageGoalSelector selector = new GeneratableCoverageGoalSelector();
        List<TestCandidateExecutionResult> executedResults = new ArrayList<>();
        List<TestCandidateExecutionResult> selectedResults = new ArrayList<>();

        for (ClassStructure classStructure : classStructures) {
            for (MethodModel method : classStructure.getMethods()) {
                Optional<MethodGenerationPlan> plan = methodPlan(
                        methodPlans,
                        classStructure.getClassName(),
                        method.getName()
                );

                if (plan.isEmpty() || !selector.isSupportedByCurrentGenerator(plan.get())) {
                    continue;
                }

                CandidateCoverageEvaluation evaluation = candidateGenerationUseCase.evaluate(
                        classStructure,
                        method,
                        typeIndex,
                        classIndex,
                        argumentValueHints
                );

                executedResults.addAll(evaluation.getExecutedResults());
                selectedResults.addAll(evaluation.getSelectedResults());
            }
        }

        return new CandidateCoverageEvaluation(executedResults, selectedResults);
    }

    private static List<MethodGenerationPlan> methodPlans(
            List<ClassStructure> classStructures,
            ProjectTypeIndex typeIndex
    ) {
        MethodGenerationPlanner planner = new MethodGenerationPlanner();
        List<MethodGenerationPlan> methodPlans = new ArrayList<>();

        for (ClassStructure classStructure : classStructures) {
            methodPlans.addAll(planner.plan(classStructure, typeIndex));
        }

        return List.copyOf(methodPlans);
    }

    private static Optional<MethodGenerationPlan> methodPlan(
            List<MethodGenerationPlan> methodPlans,
            String className,
            String methodName
    ) {
        return methodPlans.stream()
                .filter(plan -> plan.getClassName().equals(className))
                .filter(plan -> plan.getMethodName().equals(methodName))
                .findFirst();
    }

    private static List<ArgumentValueHint> argumentValueHints(
            Map<Path, ClassAnalysisResult> analysisResultsByPath,
            ObservedProfile observedProfile
    ) {
        List<ArgumentValueHint> argumentValueHints = new ArrayList<>();
        argumentValueHints.addAll(staticArgumentValueHints(analysisResultsByPath));
        argumentValueHints.addAll(observedProfile.getArgumentValueHints());

        return List.copyOf(argumentValueHints);
    }

    private static List<ArgumentValueHint> staticArgumentValueHints(
            Map<Path, ClassAnalysisResult> analysisResultsByPath
    ) {
        return analysisResultsByPath.values().stream()
                .flatMap(result -> result.getArgumentValueHints().stream())
                .toList();
    }

    private static Map<Path, List<CoverageGoal>> coverageGoalsBySourcePath(
            Map<Path, ClassAnalysisResult> analysisResultsByPath
    ) {
        Map<Path, List<CoverageGoal>> coverageGoalsBySourcePath = new LinkedHashMap<>();

        for (Map.Entry<Path, ClassAnalysisResult> entry : analysisResultsByPath.entrySet()) {
            coverageGoalsBySourcePath.put(entry.getKey(), entry.getValue().getCoverageGoals());
        }

        return coverageGoalsBySourcePath;
    }

    private static List<Path> sourcePaths(Path sourceRoot) {
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

    private static List<Path> classSourcePaths(List<Path> sourcePaths, ProjectTypeIndex typeIndex) {
        return sourcePaths.stream()
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
}
