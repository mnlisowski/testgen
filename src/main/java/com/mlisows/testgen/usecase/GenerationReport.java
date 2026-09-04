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

    public int getCandidateSpaceMethodCount() {
        return coverageEvaluation.getCandidateSpaceMethodCount();
    }

    public int getCandidateSpaceGoalCount() {
        Set<String> candidateSpaceMethodSignatures = coverageEvaluation.getCandidateSpaceMethodSignatures();

        return (int) analysisResults.stream()
                .flatMap(result -> result.getCoverageGoals().stream())
                .map(CoverageGoal::getBranchId)
                .filter(branchId -> candidateSpaceMethodSignatures.contains(branchId.methodSignature()))
                .count();
    }

    public int getCoveredCandidateSpaceGoalCount() {
        Set<String> candidateSpaceMethodSignatures = coverageEvaluation.getCandidateSpaceMethodSignatures();
        Set<String> coveredBranchIds = coverageEvaluation.getCoveredBranchIds();

        return (int) analysisResults.stream()
                .flatMap(result -> result.getCoverageGoals().stream())
                .map(CoverageGoal::getBranchId)
                .filter(branchId -> candidateSpaceMethodSignatures.contains(branchId.methodSignature()))
                .filter(branchId -> coveredBranchIds.contains(branchId.asString()))
                .count();
    }

    public int getFailedCandidateSpaceMethodCount() {
        return coverageEvaluation.getFailedCandidateSpaceMethodCount();
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

    public String toText() {
        StringBuilder report = new StringBuilder();

        appendLine(report, "Test generation report");
        appendLine(report, "");

        appendLine(report, "Analysis");
        appendLine(report, "Classes analyzed: " + getAnalyzedClassCount());
        appendLine(report, "Methods analyzed: " + getAnalyzedMethodCount());
        appendLine(report, "Methods passing planner rules: " + getSupportedMethodCount());
        appendLine(report, "Skipped by planner: " + getSkippedMethods().size());
        appendLine(report, "Methods with candidate space: " + getCandidateSpaceMethodCount());
        appendLine(report, "Methods without candidate space: " + getFailedCandidateSpaceMethodCount());
        appendLine(report, "");

        appendLine(report, "Branch goals");
        appendLine(report, "Supported branch kinds: " + formatEnums(getSupportedBranchKinds()));
        appendLine(report, "Detected goals: " + getCoverageGoalCount());
        appendLine(report, "Goals in methods with candidate space: " + getCandidateSpaceGoalCount());
        appendLine(report, "Covered goals in methods with candidate space: " + getCoveredCandidateSpaceGoalCount());
        appendLine(report, "Covered goals total: " + getCoveredBranchCount());
        appendLine(report, "");

        appendLine(report, "Candidates");
        appendLine(report, "Executed: " + getExecutedCandidateCount());
        appendLine(report, "Selected: " + getSelectedCandidateCount());
        appendLine(report, "Returned: " + countExecutedByOutcome(ExecutionOutcome.RETURNED));
        appendLine(report, "Threw exception: " + countExecutedByOutcome(ExecutionOutcome.THREW_EXCEPTION));
        appendLine(report, "Failed to execute: " + countExecutedByOutcome(ExecutionOutcome.FAILED_TO_EXECUTE));
        appendLine(report, "Timed out: " + countExecutedByOutcome(ExecutionOutcome.TIMED_OUT));
        appendLine(report, "");

        appendLine(report, "Skipped methods");
        if (getSkippedMethods().isEmpty()) {
            appendLine(report, "None");
        } else {
            for (SkippedMethod skippedMethod : getSkippedMethods()) {
                appendLine(report, skippedMethod.getClassName()
                        + "."
                        + skippedMethod.getMethodName()
                        + ": "
                        + formatEnums(skippedMethod.getUnsupportedRequirements()));
            }
        }
        appendLine(report, "");

        appendLine(report, "Unsupported Java constructs");
        for (String unsupportedJavaConstruct : getUnsupportedJavaConstructs()) {
            appendLine(report, unsupportedJavaConstruct);
        }

        return report.toString();
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

    private void appendLine(StringBuilder builder, String line) {
        builder.append(line).append(System.lineSeparator());
    }

    private String formatEnums(List<? extends Enum<?>> values) {
        return String.join(", ", values.stream()
                .map(Enum::name)
                .toList());
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
