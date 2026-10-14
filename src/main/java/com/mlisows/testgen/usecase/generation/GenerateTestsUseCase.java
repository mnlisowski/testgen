package com.mlisows.testgen.usecase.generation;

import com.mlisows.testgen.domain.analysis.ClassAnalysisResult;
import com.mlisows.testgen.domain.analysis.CoverageGoal;
import com.mlisows.testgen.domain.generation.ArgumentValueHint;
import com.mlisows.testgen.domain.generation.GenerationSettings;
import com.mlisows.testgen.domain.generation.MethodGenerationPlan;
import com.mlisows.testgen.domain.profile.ObservedProfile;
import com.mlisows.testgen.domain.structure.ClassStructure;
import com.mlisows.testgen.domain.structure.ProjectClassStructureIndex;
import com.mlisows.testgen.domain.structure.ProjectTypeIndex;
import com.mlisows.testgen.domain.structure.TypeInfo;
import com.mlisows.testgen.domain.structure.TypeKind;
import com.mlisows.testgen.usecase.CandidateCoverageEvaluation;
import com.mlisows.testgen.usecase.CandidateCoverageEvaluator;
import com.mlisows.testgen.usecase.CandidateSpaceFactory;
import com.mlisows.testgen.usecase.CandidateVariantGenerator;
import com.mlisows.testgen.usecase.EvaluateProjectCandidatesUseCase;
import com.mlisows.testgen.usecase.GenerationReport;
import com.mlisows.testgen.usecase.MethodGenerationPlanner;
import com.mlisows.testgen.usecase.ports.CandidateExecutorFactory;
import com.mlisows.testgen.usecase.ports.ClassStructureAnalyzer;
import com.mlisows.testgen.usecase.ports.CodeAnalyzer;
import com.mlisows.testgen.usecase.ports.ProjectSourceLocator;
import com.mlisows.testgen.usecase.ports.TestCandidateExecutor;
import com.mlisows.testgen.usecase.ports.TypeIndexAnalyzer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Generates results without persisting reports or test files. Calls must be serialized because of runtime recorders. */
public final class GenerateTestsUseCase {
    private final ProjectSourceLocator sourceLocator;
    private final TypeIndexAnalyzer typeIndexAnalyzer;
    private final CodeAnalyzer codeAnalyzer;
    private final ClassStructureAnalyzer classStructureAnalyzer;
    private final CandidateExecutorFactory executorFactory;

    public GenerateTestsUseCase(
            ProjectSourceLocator sourceLocator,
            TypeIndexAnalyzer typeIndexAnalyzer,
            CodeAnalyzer codeAnalyzer,
            ClassStructureAnalyzer classStructureAnalyzer,
            CandidateExecutorFactory executorFactory
    ) {
        this.sourceLocator = Objects.requireNonNull(sourceLocator, "sourceLocator");
        this.typeIndexAnalyzer = Objects.requireNonNull(typeIndexAnalyzer, "typeIndexAnalyzer");
        this.codeAnalyzer = Objects.requireNonNull(codeAnalyzer, "codeAnalyzer");
        this.classStructureAnalyzer = Objects.requireNonNull(classStructureAnalyzer, "classStructureAnalyzer");
        this.executorFactory = Objects.requireNonNull(executorFactory, "executorFactory");
    }

    public GenerationResult generate(GenerationRequest request) {
        Objects.requireNonNull(request, "request");
        GenerationSettings settings = request.settings();
        CandidateVariantGenerator candidateVariantGenerator = new CandidateVariantGenerator(
                settings.initialCandidates(), settings.mutatedInitialCandidates(), settings.maxValuesPerSlot(),
                settings.maxCandidates(), settings.seed());

        List<Path> javaSources = sourceLocator.findJavaSources(request.projectRoot());

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

        ObservedProfile observedProfile = request.observedProfile();

        Map<Path, List<CoverageGoal>> coverageGoalsByPath = coverageGoalsByPath(analysisResultsByPath);

        TestCandidateExecutor candidateExecutor = executorFactory.prepare(request.projectRoot(), request.workRoot(), coverageGoalsByPath);

        List<ArgumentValueHint> argumentValueHints = new ArrayList<>();

        for (ClassAnalysisResult analysisResult : analysisResultsByPath.values()) {
            argumentValueHints.addAll(analysisResult.getArgumentValueHints());
        }

        CandidateSpaceFactory candidateSpaceFactory = new CandidateSpaceFactory();
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
                argumentValueHints,
                observedProfile.getArgumentValueHints()
        );

        return GenerationResult.from(
                new GeneratedTestRenderer().render(coverageEvaluation.getSelectedResults()),
                GenerationReport.from(List.copyOf(analysisResultsByPath.values()), methodPlans, coverageEvaluation));
    }

    private Map<Path, ClassAnalysisResult> analyzeClasses(List<Path> classSources) {
        Map<Path, ClassAnalysisResult> analysisResultsByPath = new LinkedHashMap<>();

        for (Path classSource : classSources) {
            ClassAnalysisResult analysisResult = codeAnalyzer.analyze(classSource);
            analysisResultsByPath.put(classSource, analysisResult);
        }

        return analysisResultsByPath;
    }

    private List<ClassStructure> analyzeClassStructures(List<Path> classSources) {
        List<ClassStructure> classStructures = new ArrayList<>();

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
