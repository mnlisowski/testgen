package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.BranchKind;
import com.mlisows.testgen.domain.ClassAnalysisResult;
import com.mlisows.testgen.domain.CoverageGoal;
import com.mlisows.testgen.domain.ExecutionOutcome;
import com.mlisows.testgen.domain.GenerationRequirement;
import com.mlisows.testgen.domain.MethodGenerationPlan;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class GenerationReport {
    private static final List<BranchKind> SUPPORTED_BRANCH_KINDS = List.of(
            BranchKind.IF,
            BranchKind.FOR,
            BranchKind.WHILE,
            BranchKind.SWITCH
    );
    private static final List<String> UNSUPPORTED_JAVA_CONSTRUCTS = List.of(
            "do-while branches",
            "switch expressions",
            "try/catch/finally branches",
            "lambda and stream predicate branches",
            "short-circuit subconditions",
            "ternary expressions"
    );

    private final List<ClassAnalysisResult> analysisResults;
    private final List<MethodGenerationPlan> methodPlans;
    private final CandidateCoverageEvaluation coverageEvaluation;

    private GenerationReport(
            List<ClassAnalysisResult> analysisResults,
            List<MethodGenerationPlan> methodPlans,
            CandidateCoverageEvaluation coverageEvaluation
    ) {
        this.analysisResults = List.copyOf(Objects.requireNonNull(
                analysisResults,
                "analysisResults must not be null"
        ));
        this.methodPlans = List.copyOf(Objects.requireNonNull(methodPlans, "methodPlans must not be null"));
        this.coverageEvaluation = Objects.requireNonNull(
                coverageEvaluation,
                "coverageEvaluation must not be null"
        );
    }

    public static GenerationReport from(
            List<ClassAnalysisResult> analysisResults,
            List<MethodGenerationPlan> methodPlans,
            CandidateCoverageEvaluation coverageEvaluation
    ) {
        return new GenerationReport(analysisResults, methodPlans, coverageEvaluation);
    }

    public int getAnalyzedClassCount() {
        return analysisResults.size();
    }

    public int getAnalyzedMethodCount() {
        return methodPlans.size();
    }

    public int getSupportedMethodCount() {
        return (int) methodPlans.stream()
                .filter(plan -> unsupportedRequirements(plan).isEmpty())
                .count();
    }

    public List<SkippedMethod> getSkippedMethods() {
        List<SkippedMethod> skippedMethods = new ArrayList<>();

        for (MethodGenerationPlan plan : methodPlans) {
            List<GenerationRequirement> unsupportedRequirements = unsupportedRequirements(plan);

            if (!unsupportedRequirements.isEmpty()) {
                skippedMethods.add(new SkippedMethod(
                        plan.getClassName(),
                        plan.getMethodName(),
                        unsupportedRequirements
                ));
            }
        }

        return List.copyOf(skippedMethods);
    }

    public int getCoverageGoalCount() {
        return analysisResults.stream()
                .map(ClassAnalysisResult::getCoverageGoals)
                .mapToInt(List::size)
                .sum();
    }

    public List<String> getCoverageGoalBranchIds() {
        return analysisResults.stream()
                .flatMap(result -> result.getCoverageGoals().stream())
                .map(CoverageGoal::getBranchId)
                .map(branchId -> branchId.asString())
                .toList();
    }

    public int getCoveredBranchCount() {
        return coverageEvaluation.getCoveredBranchIds().size();
    }

    public Set<String> getCoveredBranchIds() {
        return coverageEvaluation.getCoveredBranchIds();
    }

    public int getExecutedCandidateCount() {
        return coverageEvaluation.getExecutedCandidateCount();
    }

    public int getSelectedCandidateCount() {
        return coverageEvaluation.getSelectedCandidateCount();
    }

    public long countExecutedByOutcome(ExecutionOutcome outcome) {
        return coverageEvaluation.countExecutedByOutcome(outcome);
    }

    public List<BranchKind> getSupportedBranchKinds() {
        return SUPPORTED_BRANCH_KINDS;
    }

    public List<String> getUnsupportedJavaConstructs() {
        return UNSUPPORTED_JAVA_CONSTRUCTS;
    }

    private List<GenerationRequirement> unsupportedRequirements(MethodGenerationPlan plan) {
        Set<GenerationRequirement> unsupportedRequirements = new LinkedHashSet<>();

        for (GenerationRequirement requirement : plan.getRequirements()) {
            if (!isSupportedRequirement(requirement)) {
                unsupportedRequirements.add(requirement);
            }
        }

        return List.copyOf(unsupportedRequirements);
    }

    private boolean isSupportedRequirement(GenerationRequirement requirement) {
        return requirement == GenerationRequirement.NO_ARG_CONSTRUCTOR
                || requirement == GenerationRequirement.PRIMITIVE_ARGUMENT
                || requirement == GenerationRequirement.STRING_ARGUMENT
                || requirement == GenerationRequirement.ENUM_ARGUMENT
                || requirement == GenerationRequirement.OBJECT_FIXTURE;
    }

    public static final class SkippedMethod {
        private final String className;
        private final String methodName;
        private final List<GenerationRequirement> unsupportedRequirements;

        private SkippedMethod(
                String className,
                String methodName,
                List<GenerationRequirement> unsupportedRequirements
        ) {
            this.className = Objects.requireNonNull(className, "className must not be null");
            this.methodName = Objects.requireNonNull(methodName, "methodName must not be null");
            this.unsupportedRequirements = List.copyOf(Objects.requireNonNull(
                    unsupportedRequirements,
                    "unsupportedRequirements must not be null"
            ));
        }

        public String getClassName() {
            return className;
        }

        public String getMethodName() {
            return methodName;
        }

        public List<GenerationRequirement> getUnsupportedRequirements() {
            return unsupportedRequirements;
        }
    }
}
