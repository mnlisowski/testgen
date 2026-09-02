package com.mlisows.testgen.cli;

import com.mlisows.testgen.domain.ArgumentValueHint;
import com.mlisows.testgen.domain.ClassAnalysisResult;
import com.mlisows.testgen.domain.ClassStructure;
import com.mlisows.testgen.domain.CoverageGoal;
import com.mlisows.testgen.domain.MethodGenerationPlan;
import com.mlisows.testgen.domain.ObservedProfile;
import com.mlisows.testgen.domain.ProjectClassStructureIndex;
import com.mlisows.testgen.domain.ProjectTypeIndex;
import com.mlisows.testgen.infrastructure.project.MavenProjectLayout;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class ProjectAnalysis {
    private final MavenProjectLayout layout;
    private final Map<Path, ClassAnalysisResult> analysisResultsByPath;
    private final List<ClassStructure> classStructures;
    private final ProjectTypeIndex typeIndex;
    private final ProjectClassStructureIndex classIndex;
    private final List<MethodGenerationPlan> methodPlans;

    ProjectAnalysis(
            MavenProjectLayout layout,
            Map<Path, ClassAnalysisResult> analysisResultsByPath,
            List<ClassStructure> classStructures,
            ProjectTypeIndex typeIndex,
            ProjectClassStructureIndex classIndex,
            List<MethodGenerationPlan> methodPlans
    ) {
        this.layout = Objects.requireNonNull(layout, "layout must not be null");
        this.analysisResultsByPath = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(
                analysisResultsByPath,
                "analysisResultsByPath must not be null"
        )));
        this.classStructures = List.copyOf(Objects.requireNonNull(
                classStructures,
                "classStructures must not be null"
        ));
        this.typeIndex = Objects.requireNonNull(typeIndex, "typeIndex must not be null");
        this.classIndex = Objects.requireNonNull(classIndex, "classIndex must not be null");
        this.methodPlans = List.copyOf(Objects.requireNonNull(methodPlans, "methodPlans must not be null"));
    }

    MavenProjectLayout getLayout() {
        return layout;
    }

    List<ClassAnalysisResult> getAnalysisResults() {
        return List.copyOf(analysisResultsByPath.values());
    }

    List<ClassStructure> getClassStructures() {
        return classStructures;
    }

    ProjectTypeIndex getTypeIndex() {
        return typeIndex;
    }

    ProjectClassStructureIndex getClassIndex() {
        return classIndex;
    }

    List<MethodGenerationPlan> getMethodPlans() {
        return methodPlans;
    }

    Map<Path, List<CoverageGoal>> coverageGoalsBySourcePath() {
        Map<Path, List<CoverageGoal>> coverageGoalsBySourcePath = new LinkedHashMap<>();

        for (Map.Entry<Path, ClassAnalysisResult> entry : analysisResultsByPath.entrySet()) {
            coverageGoalsBySourcePath.put(entry.getKey(), entry.getValue().getCoverageGoals());
        }

        return coverageGoalsBySourcePath;
    }

    List<ArgumentValueHint> argumentValueHints(ObservedProfile observedProfile) {
        Objects.requireNonNull(observedProfile, "observedProfile must not be null");

        List<ArgumentValueHint> hints = new ArrayList<>();
        hints.addAll(staticArgumentValueHints());
        hints.addAll(observedProfile.getArgumentValueHints());

        return List.copyOf(hints);
    }

    private List<ArgumentValueHint> staticArgumentValueHints() {
        return analysisResultsByPath.values().stream()
                .flatMap(result -> result.getArgumentValueHints().stream())
                .toList();
    }
}
