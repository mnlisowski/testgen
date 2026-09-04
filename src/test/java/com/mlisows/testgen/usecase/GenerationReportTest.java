package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.analysis.BranchId;
import com.mlisows.testgen.domain.analysis.BranchKind;
import com.mlisows.testgen.domain.analysis.BranchType;
import com.mlisows.testgen.domain.analysis.ClassAnalysisResult;
import com.mlisows.testgen.domain.analysis.CoverageGoal;
import com.mlisows.testgen.domain.execution.ExecutionOutcome;
import com.mlisows.testgen.domain.generation.GeneratedArgument;
import com.mlisows.testgen.domain.generation.GeneratedSetupObject;
import com.mlisows.testgen.domain.generation.GenerationRequirement;
import com.mlisows.testgen.domain.generation.MethodGenerationPlan;
import com.mlisows.testgen.domain.generation.TestCandidate;
import com.mlisows.testgen.domain.execution.TestCandidateExecutionResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerationReportTest {

    @Test
    void shouldSummarizeGenerationInputsAndCandidateEvaluation() {
        BranchId falseBranch = branch("sample.Calculator", "calculate(int)", BranchType.FALSE);
        BranchId trueBranch = branch("sample.Calculator", "calculate(int)", BranchType.TRUE);
        BranchId indirectlyCoveredBranch = branch("sample.Helper", "normalize(int)", BranchType.TRUE);
        ClassAnalysisResult calculatorAnalysisResult = new ClassAnalysisResult(
                "sample.Calculator",
                List.of(
                        new CoverageGoal(falseBranch, "amount <= 100"),
                        new CoverageGoal(trueBranch, "amount > 100")
                )
        );
        ClassAnalysisResult helperAnalysisResult = new ClassAnalysisResult(
                "sample.Helper",
                List.of(new CoverageGoal(indirectlyCoveredBranch, "value > 0"))
        );

        MethodGenerationPlan supportedPlan = new MethodGenerationPlan(
                "sample.Calculator",
                "calculate",
                List.of(),
                List.of(
                        GenerationRequirement.NO_ARG_CONSTRUCTOR,
                        GenerationRequirement.PRIMITIVE_ARGUMENT
                )
        );
        MethodGenerationPlan skippedPlan = new MethodGenerationPlan(
                "sample.ReportService",
                "export",
                List.of(),
                List.of(
                        GenerationRequirement.NO_ARG_CONSTRUCTOR,
                        GenerationRequirement.INTERFACE_MOCK,
                        GenerationRequirement.MAP_FIXTURE,
                        GenerationRequirement.MAP_FIXTURE
                )
        );

        TestCandidateExecutionResult selectedResult = TestCandidateExecutionResult.returned(
                candidate("-1"),
                List.of(falseBranch, indirectlyCoveredBranch),
                "0"
        );
        TestCandidateExecutionResult failedResult = TestCandidateExecutionResult.failedToExecute(
                candidate("101"),
                List.of(),
                "java.lang.IllegalStateException",
                "cannot execute candidate"
        );
        CandidateCoverageEvaluation evaluation = new CandidateCoverageEvaluation(
                List.of(selectedResult, failedResult),
                List.of(selectedResult),
                Set.of("sample.Calculator.calculate(int)"),
                1
        );

        GenerationReport report = GenerationReport.from(
                List.of(calculatorAnalysisResult, helperAnalysisResult),
                List.of(supportedPlan, skippedPlan),
                evaluation
        );

        assertEquals(2, report.getAnalyzedClassCount());
        assertEquals(2, report.getAnalyzedMethodCount());
        assertEquals(1, report.getSupportedMethodCount());
        assertEquals(3, report.getCoverageGoalCount());
        assertEquals(2, report.getCandidateSpaceGoalCount());
        assertEquals(1, report.getCoveredCandidateSpaceGoalCount());
        assertEquals(List.of(falseBranch.asString(), trueBranch.asString(), indirectlyCoveredBranch.asString()), report.getCoverageGoalBranchIds());
        assertEquals(2, report.getCoveredBranchCount());
        assertEquals(2, report.getExecutedCandidateCount());
        assertEquals(1, report.getSelectedCandidateCount());
        assertEquals(1, report.getCandidateSpaceMethodCount());
        assertEquals(1, report.getFailedCandidateSpaceMethodCount());
        assertEquals(1, report.countExecutedByOutcome(ExecutionOutcome.RETURNED));
        assertEquals(1, report.countExecutedByOutcome(ExecutionOutcome.FAILED_TO_EXECUTE));
        assertEquals(List.of(
                BranchKind.IF,
                BranchKind.FOR,
                BranchKind.WHILE,
                BranchKind.SWITCH
        ), report.getSupportedBranchKinds());
        assertTrue(report.getUnsupportedJavaConstructs().contains("wyrażenia switch"));

        GenerationReport.SkippedMethod skippedMethod = report.getSkippedMethods().get(0);
        assertEquals("sample.ReportService", skippedMethod.getClassName());
        assertEquals("export", skippedMethod.getMethodName());
        assertEquals(List.of(
                GenerationRequirement.INTERFACE_MOCK,
                GenerationRequirement.MAP_FIXTURE
        ), skippedMethod.getUnsupportedRequirements());

        String reportText = report.toText();

        assertTrue(reportText.contains("Raport z generowania testów"));
        assertTrue(reportText.contains("Przeanalizowane klasy: 2"));
        assertTrue(reportText.contains("Metody spełniające podstawowe ograniczenia generatora: 1"));
        assertTrue(reportText.contains("Metody, dla których przygotowano kandydatów: 1"));
        assertTrue(reportText.contains("Metody, dla których nie udało się przygotować kandydatów: 1"));
        assertTrue(reportText.contains("Obsługiwane rodzaje gałęzi: IF, FOR, WHILE, SWITCH"));
        assertTrue(reportText.contains("Wykryte cele pokrycia gałęzi: 3"));
        assertTrue(reportText.contains("Cele w metodach, dla których przygotowano kandydatów: 2"));
        assertTrue(reportText.contains("Pokryte cele w metodach, dla których przygotowano kandydatów: 1"));
        assertTrue(reportText.contains("Wszystkie pokryte cele: 2"));
        assertTrue(reportText.contains("Liczba wykonanych kandydatów: 2"));
        assertTrue(reportText.contains("Liczba kandydatów wybranych do testów: 1"));
        assertTrue(reportText.contains("Liczba nieudanych wykonań: 1"));
        assertTrue(reportText.contains("sample.ReportService.export: INTERFACE_MOCK, MAP_FIXTURE"));
        assertTrue(reportText.contains("Wszystkie pokryte cele mogą być większe"));
        assertTrue(reportText.contains("wyrażenia switch"));
    }

    private static TestCandidate candidate(String amount) {
        return new TestCandidate(
                "sample.Calculator",
                "calculate",
                "int",
                List.of(new GeneratedSetupObject("Calculator", "calculator", List.of())),
                "calculator",
                List.of(new GeneratedArgument("int", amount))
        );
    }

    private static BranchId branch(String className, String methodName, BranchType branchType) {
        return new BranchId(
                className,
                methodName,
                10,
                BranchKind.IF,
                branchType,
                ""
        );
    }
}
