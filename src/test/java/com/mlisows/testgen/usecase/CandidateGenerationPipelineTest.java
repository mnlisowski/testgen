package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.BranchId;
import com.mlisows.testgen.domain.ClassAnalysisResult;
import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.CoverageGoal;
import com.mlisows.testgen.domain.MethodModel;
import com.mlisows.testgen.domain.ProjectClassStructureIndex;
import com.mlisows.testgen.domain.ProjectTypeIndex;
import com.mlisows.testgen.domain.StaticArgumentValueHint;
import com.mlisows.testgen.domain.TestCandidateExecutionResult;
import com.mlisows.testgen.domain.TypeInfo;
import com.mlisows.testgen.domain.TypeKind;
import com.mlisows.testgen.infrastructure.execution.ReflectionTestCandidateExecutor;
import com.mlisows.testgen.infrastructure.parser.JavaParserClassStructureAnalyzer;
import com.mlisows.testgen.infrastructure.parser.JavaParserCodeAnalyzer;
import com.mlisows.testgen.infrastructure.parser.JavaParserTypeIndexAnalyzer;
import com.mlisows.testgen.infrastructure.project.InstrumentedProjectWorkspace;
import com.mlisows.testgen.infrastructure.project.InstrumentedProjectWorkspacePreparer;
import com.mlisows.testgen.infrastructure.project.MavenProjectLayout;
import com.mlisows.testgen.infrastructure.project.MavenProjectLayoutDetector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CandidateGenerationPipelineTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldGenerateAndExecuteCoverageImprovingCandidatesForMavenProjectMethod() {
        MavenProjectLayout layout = new MavenProjectLayoutDetector()
                .detect(Path.of("src/test/resources/sample-maven-project"));
        List<Path> sourcePaths = sourcePaths(layout.getMainSourceRoot());
        Path discountServicePath = layout.getMainSourceRoot().resolve("sample/maven/DiscountService.java");

        ClassAnalysisResult analysisResult = new JavaParserCodeAnalyzer().analyze(discountServicePath);
        ProjectTypeIndex typeIndex = new JavaParserTypeIndexAnalyzer().analyze(sourcePaths);
        ClassStructure discountService = new JavaParserClassStructureAnalyzer().analyze(discountServicePath);
        ProjectClassStructureIndex classIndex = new ProjectClassStructureIndex(List.of(discountService));

        InstrumentedProjectWorkspace workspace = new InstrumentedProjectWorkspacePreparer()
                .prepare(
                        layout,
                        tempDir.resolve("workspace"),
                        Map.of(discountServicePath, analysisResult.getCoverageGoals())
                );

        CandidateGenerationUseCase useCase = new CandidateGenerationUseCase(
                new CandidateSpaceFactory(),
                new CandidateVariantGenerator(10, 20),
                new CandidateCoverageEvaluator(new ReflectionTestCandidateExecutor(workspace))
        );

        List<TestCandidateExecutionResult> results = useCase.execute(
                discountService,
                method(discountService, "shippingFee"),
                typeIndex,
                classIndex,
                analysisResult.getStaticArgumentValueHints()
        );

        List<String> coveredBranchIds = results.stream()
                .flatMap(result -> result.getCoveredBranches().stream())
                .map(BranchId::asString)
                .distinct()
                .sorted()
                .toList();
        List<String> expectedShippingFeeBranchIds = analysisResult.getCoverageGoals().stream()
                .map(CoverageGoal::getBranchId)
                .filter(branchId -> branchId.getMethodName().equals("shippingFee"))
                .map(BranchId::asString)
                .sorted()
                .toList();

        assertEquals(3, results.size());
        assertEquals(expectedShippingFeeBranchIds, coveredBranchIds);
    }

    @Test
    void shouldCountBranchCoverageForWholeMavenProject() {
        MavenProjectLayout layout = new MavenProjectLayoutDetector()
                .detect(Path.of("src/test/resources/sample-maven-project"));
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

        InstrumentedProjectWorkspace workspace = new InstrumentedProjectWorkspacePreparer()
                .prepare(
                        layout,
                        tempDir.resolve("whole-project-workspace"),
                        coverageGoalsBySourcePath(analysisResultsByPath)
                );

        CandidateGenerationUseCase useCase = new CandidateGenerationUseCase(
                new CandidateSpaceFactory(),
                new CandidateVariantGenerator(10, 50),
                new CandidateCoverageEvaluator(new ReflectionTestCandidateExecutor(workspace))
        );

        List<StaticArgumentValueHint> staticHints = analysisResultsByPath.values().stream()
                .flatMap(result -> result.getStaticArgumentValueHints().stream())
                .toList();
        List<TestCandidateExecutionResult> results = new ArrayList<>();

        for (ClassStructure classStructure : classStructures) {
            for (MethodModel method : classStructure.getMethods()) {
                results.addAll(useCase.execute(
                        classStructure,
                        method,
                        typeIndex,
                        classIndex,
                        staticHints
                ));
            }
        }

        List<String> allBranchIds = analysisResultsByPath.values().stream()
                .flatMap(result -> result.getCoverageGoals().stream())
                .map(coverageGoal -> coverageGoal.getBranchId().asString())
                .distinct()
                .sorted()
                .toList();
        List<String> coveredBranchIds = results.stream()
                .flatMap(result -> result.getCoveredBranches().stream())
                .map(BranchId::asString)
                .distinct()
                .sorted()
                .toList();

        assertEquals(15, allBranchIds.size());
        assertEquals(12, coveredBranchIds.size());
        assertTrue(allBranchIds.containsAll(coveredBranchIds));
    }

    private static MethodModel method(ClassStructure classStructure, String methodName) {
        return classStructure.getMethods().stream()
                .filter(method -> method.getName().equals(methodName))
                .findFirst()
                .orElseThrow();
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

    private static Map<Path, List<CoverageGoal>> coverageGoalsBySourcePath(
            Map<Path, ClassAnalysisResult> analysisResultsByPath
    ) {
        Map<Path, List<CoverageGoal>> coverageGoalsBySourcePath = new LinkedHashMap<>();

        for (Map.Entry<Path, ClassAnalysisResult> entry : analysisResultsByPath.entrySet()) {
            coverageGoalsBySourcePath.put(entry.getKey(), entry.getValue().getCoverageGoals());
        }

        return coverageGoalsBySourcePath;
    }

    private static String sourceTypeName(Path sourcePath) {
        String fileName = sourcePath.getFileName().toString();
        return fileName.substring(0, fileName.length() - ".java".length());
    }
}
