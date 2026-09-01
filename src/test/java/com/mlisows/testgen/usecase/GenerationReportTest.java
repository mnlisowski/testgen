package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.BranchId;
import com.mlisows.testgen.domain.BranchKind;
import com.mlisows.testgen.domain.BranchType;
import com.mlisows.testgen.domain.ClassAnalysisResult;
import com.mlisows.testgen.domain.CoverageGoal;
import com.mlisows.testgen.domain.ExecutionOutcome;
import com.mlisows.testgen.domain.GeneratedArgument;
import com.mlisows.testgen.domain.GeneratedSetupObject;
import com.mlisows.testgen.domain.GenerationRequirement;
import com.mlisows.testgen.domain.MethodGenerationPlan;
import com.mlisows.testgen.domain.TestCandidate;
import com.mlisows.testgen.domain.TestCandidateExecutionResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerationReportTest {

    @Test
    void shouldSummarizeGenerationInputsAndCandidateEvaluation() {
        BranchId falseBranch = branch(BranchType.FALSE);
        BranchId trueBranch = branch(BranchType.TRUE);
        ClassAnalysisResult analysisResult = new ClassAnalysisResult(
                "sample.Calculator",
                List.of(
                        new CoverageGoal(falseBranch, "amount <= 100"),
                        new CoverageGoal(trueBranch, "amount > 100")
                )
        );

        MethodGenerationPlan supportedPlan = new MethodGenerationPlan(
                "sample.Calculator",
                "calculate",
                List.of(
                        GenerationRequirement.NO_ARG_CONSTRUCTOR,
                        GenerationRequirement.PRIMITIVE_ARGUMENT
                )
        );
        MethodGenerationPlan skippedPlan = new MethodGenerationPlan(
                "sample.ReportService",
                "export",
                List.of(
                        GenerationRequirement.NO_ARG_CONSTRUCTOR,
                        GenerationRequirement.INTERFACE_MOCK,
                        GenerationRequirement.MAP_FIXTURE,
                        GenerationRequirement.MAP_FIXTURE
                )
        );

        TestCandidateExecutionResult selectedResult = TestCandidateExecutionResult.returned(
                candidate("-1"),
                List.of(falseBranch),
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
                List.of(selectedResult)
        );

        GenerationReport report = GenerationReport.from(
                List.of(analysisResult),
                List.of(supportedPlan, skippedPlan),
                evaluation
        );

        assertEquals(1, report.getAnalyzedClassCount());
        assertEquals(2, report.getAnalyzedMethodCount());
        assertEquals(1, report.getSupportedMethodCount());
        assertEquals(2, report.getCoverageGoalCount());
        assertEquals(List.of(falseBranch.asString(), trueBranch.asString()), report.getCoverageGoalBranchIds());
        assertEquals(1, report.getCoveredBranchCount());
        assertEquals(2, report.getExecutedCandidateCount());
        assertEquals(1, report.getSelectedCandidateCount());
        assertEquals(1, report.countExecutedByOutcome(ExecutionOutcome.RETURNED));
        assertEquals(1, report.countExecutedByOutcome(ExecutionOutcome.FAILED_TO_EXECUTE));
        assertEquals(List.of(
                BranchKind.IF,
                BranchKind.FOR,
                BranchKind.WHILE,
                BranchKind.SWITCH
        ), report.getSupportedBranchKinds());
        assertTrue(report.getUnsupportedJavaConstructs().contains("switch expressions"));

        GenerationReport.SkippedMethod skippedMethod = report.getSkippedMethods().get(0);
        assertEquals("sample.ReportService", skippedMethod.getClassName());
        assertEquals("export", skippedMethod.getMethodName());
        assertEquals(List.of(
                GenerationRequirement.INTERFACE_MOCK,
                GenerationRequirement.MAP_FIXTURE
        ), skippedMethod.getUnsupportedRequirements());
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

    private static BranchId branch(BranchType branchType) {
        return new BranchId(
                "sample.Calculator",
                "calculate",
                10,
                BranchKind.IF,
                branchType,
                ""
        );
    }
}
