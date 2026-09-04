package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.analysis.BranchKind;
import com.mlisows.testgen.domain.analysis.ClassAnalysisResult;
import com.mlisows.testgen.domain.analysis.CoverageGoal;
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
            "gałęzie do-while",
            "wyrażenia switch",
            "gałęzie try/catch/finally",
            "gałęzie w lambdach i predykatach strumieni",
            "skracane warunki logiczne, na przykład && albo ||",
            "operator trójargumentowy"
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

        appendLine(report, "Raport z generowania testów");
        appendLine(report, "");

        appendLine(report, "Analiza projektu");
        appendLine(report, "Przeanalizowane klasy: " + getAnalyzedClassCount());
        appendLine(report, "Przeanalizowane metody: " + getAnalyzedMethodCount());
        appendLine(report, "Metody spełniające podstawowe ograniczenia generatora: " + getSupportedMethodCount());
        appendLine(report, "Metody odrzucone przez podstawowe ograniczenia generatora: " + getSkippedMethods().size());
        appendLine(report, "Metody, dla których przygotowano kandydatów: " + getCandidateSpaceMethodCount());
        appendLine(report, "Metody, dla których nie udało się przygotować kandydatów: " + getFailedCandidateSpaceMethodCount());
        appendLine(report, "");

        appendLine(report, "Cele pokrycia gałęzi");
        appendLine(report, "Obsługiwane rodzaje gałęzi: " + formatEnums(getSupportedBranchKinds()));
        appendLine(report, "Wykryte cele pokrycia gałęzi: " + getCoverageGoalCount());
        appendLine(report, "Cele w metodach, dla których przygotowano kandydatów: " + getCandidateSpaceGoalCount());
        appendLine(report, "Pokryte cele w metodach, dla których przygotowano kandydatów: " + getCoveredCandidateSpaceGoalCount());
        appendLine(report, "Wszystkie pokryte cele: " + getCoveredBranchCount());
        appendLine(report, "Wszystkie pokryte cele mogą być większe niż pokryte cele w metodach z kandydatami, ponieważ wykonanie jednej metody może pośrednio wejść także do innych metod.");
        appendLine(report, "");

        appendLine(report, "Kandydaci");
        appendLine(report, "Liczba wykonanych kandydatów: " + getExecutedCandidateCount());
        appendLine(report, "Liczba kandydatów wybranych do testów: " + getSelectedCandidateCount());
        appendLine(report, "Liczba kandydatów zakończonych normalnie: " + countExecutedByOutcome(ExecutionOutcome.RETURNED));
        appendLine(report, "Liczba kandydatów zakończonych wyjątkiem: " + countExecutedByOutcome(ExecutionOutcome.THREW_EXCEPTION));
        appendLine(report, "Liczba nieudanych wykonań: " + countExecutedByOutcome(ExecutionOutcome.FAILED_TO_EXECUTE));
        appendLine(report, "Liczba wykonań przerwanych przez limit czasu: " + countExecutedByOutcome(ExecutionOutcome.TIMED_OUT));
        appendLine(report, "");

        appendLine(report, "Pominięte metody");
        if (getSkippedMethods().isEmpty()) {
            appendLine(report, "Brak");
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

        appendLine(report, "Nieobsługiwane konstrukcje Javy");
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
