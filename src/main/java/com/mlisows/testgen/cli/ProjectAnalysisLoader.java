package com.mlisows.testgen.cli;

import com.mlisows.testgen.domain.ClassAnalysisResult;
import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.MethodGenerationPlan;
import com.mlisows.testgen.domain.ProjectClassStructureIndex;
import com.mlisows.testgen.domain.ProjectTypeIndex;
import com.mlisows.testgen.domain.TypeInfo;
import com.mlisows.testgen.domain.TypeKind;
import com.mlisows.testgen.infrastructure.parser.JavaParserClassStructureAnalyzer;
import com.mlisows.testgen.infrastructure.parser.JavaParserCodeAnalyzer;
import com.mlisows.testgen.infrastructure.parser.JavaParserTypeIndexAnalyzer;
import com.mlisows.testgen.infrastructure.project.MavenProjectLayout;
import com.mlisows.testgen.infrastructure.project.MavenProjectLayoutDetector;
import com.mlisows.testgen.usecase.MethodGenerationPlanner;
import com.mlisows.testgen.usecase.ports.ClassStructureAnalyzer;
import com.mlisows.testgen.usecase.ports.CodeAnalyzer;
import com.mlisows.testgen.usecase.ports.TypeIndexAnalyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

final class ProjectAnalysisLoader {
    private final MavenProjectLayoutDetector layoutDetector;
    private final TypeIndexAnalyzer typeIndexAnalyzer;
    private final CodeAnalyzer codeAnalyzer;
    private final ClassStructureAnalyzer classStructureAnalyzer;
    private final MethodGenerationPlanner methodGenerationPlanner;

    ProjectAnalysisLoader() {
        this(
                new MavenProjectLayoutDetector(),
                new JavaParserTypeIndexAnalyzer(),
                new JavaParserCodeAnalyzer(),
                new JavaParserClassStructureAnalyzer(),
                new MethodGenerationPlanner()
        );
    }

    ProjectAnalysisLoader(
            MavenProjectLayoutDetector layoutDetector,
            TypeIndexAnalyzer typeIndexAnalyzer,
            CodeAnalyzer codeAnalyzer,
            ClassStructureAnalyzer classStructureAnalyzer,
            MethodGenerationPlanner methodGenerationPlanner
    ) {
        this.layoutDetector = Objects.requireNonNull(layoutDetector, "layoutDetector must not be null");
        this.typeIndexAnalyzer = Objects.requireNonNull(typeIndexAnalyzer, "typeIndexAnalyzer must not be null");
        this.codeAnalyzer = Objects.requireNonNull(codeAnalyzer, "codeAnalyzer must not be null");
        this.classStructureAnalyzer = Objects.requireNonNull(
                classStructureAnalyzer,
                "classStructureAnalyzer must not be null"
        );
        this.methodGenerationPlanner = Objects.requireNonNull(
                methodGenerationPlanner,
                "methodGenerationPlanner must not be null"
        );
    }

    ProjectAnalysis load(Path projectRoot) {
        Objects.requireNonNull(projectRoot, "projectRoot must not be null");

        MavenProjectLayout layout = layoutDetector.detect(projectRoot);
        List<Path> sourcePaths = sourcePaths(layout.getMainSourceRoot());
        ProjectTypeIndex typeIndex = typeIndexAnalyzer.analyze(sourcePaths);
        List<Path> classSourcePaths = classSourcePaths(sourcePaths, typeIndex);

        Map<Path, ClassAnalysisResult> analysisResultsByPath = analyzeSources(classSourcePaths);
        List<ClassStructure> classStructures = classStructures(classSourcePaths);
        ProjectClassStructureIndex classIndex = new ProjectClassStructureIndex(classStructures);
        List<MethodGenerationPlan> methodPlans = methodPlans(classStructures, typeIndex);

        return new ProjectAnalysis(
                layout,
                analysisResultsByPath,
                classStructures,
                typeIndex,
                classIndex,
                methodPlans
        );
    }

    private Map<Path, ClassAnalysisResult> analyzeSources(List<Path> classSourcePaths) {
        Map<Path, ClassAnalysisResult> analysisResultsByPath = new LinkedHashMap<>();

        for (Path sourcePath : classSourcePaths) {
            analysisResultsByPath.put(sourcePath, codeAnalyzer.analyze(sourcePath));
        }

        return analysisResultsByPath;
    }

    private List<ClassStructure> classStructures(List<Path> classSourcePaths) {
        return classSourcePaths.stream()
                .map(classStructureAnalyzer::analyze)
                .toList();
    }

    private List<MethodGenerationPlan> methodPlans(
            List<ClassStructure> classStructures,
            ProjectTypeIndex typeIndex
    ) {
        List<MethodGenerationPlan> methodPlans = new ArrayList<>();

        for (ClassStructure classStructure : classStructures) {
            methodPlans.addAll(methodGenerationPlanner.plan(classStructure, typeIndex));
        }

        return List.copyOf(methodPlans);
    }

    private List<Path> sourcePaths(Path sourceRoot) {
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

    private List<Path> classSourcePaths(List<Path> sourcePaths, ProjectTypeIndex typeIndex) {
        return sourcePaths.stream()
                .filter(sourcePath -> typeIndex.findByName(sourceTypeName(sourcePath))
                        .map(TypeInfo::getKind)
                        .filter(kind -> kind == TypeKind.CLASS)
                        .isPresent())
                .toList();
    }

    private String sourceTypeName(Path sourcePath) {
        String fileName = sourcePath.getFileName().toString();

        if (!fileName.endsWith(".java")) {
            return fileName;
        }

        return fileName.substring(0, fileName.length() - ".java".length());
    }
}
