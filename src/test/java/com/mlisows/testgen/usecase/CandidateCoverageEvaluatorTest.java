package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.analysis.BranchId;
import com.mlisows.testgen.domain.analysis.BranchKind;
import com.mlisows.testgen.domain.analysis.BranchType;
import com.mlisows.testgen.domain.ExecutionOutcome;
import com.mlisows.testgen.domain.GeneratedArgument;
import com.mlisows.testgen.domain.GeneratedSetupObject;
import com.mlisows.testgen.domain.TestCandidate;
import com.mlisows.testgen.domain.TestCandidateExecutionResult;
import com.mlisows.testgen.usecase.ports.TestCandidateExecutor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CandidateCoverageEvaluatorTest {

    @Test
    void shouldKeepOnlyCandidatesThatAddNewBranchCoverage() {
        TestCandidate firstCandidate = candidate("-1");
        TestCandidate duplicateCandidate = candidate("-2");
        TestCandidate newBranchCandidate = candidate("101");

        FakeExecutor executor = new FakeExecutor(List.of(
                result(firstCandidate, branch(BranchType.FALSE)),
                result(duplicateCandidate, branch(BranchType.FALSE)),
                result(newBranchCandidate, branch(BranchType.TRUE))
        ));

        List<TestCandidateExecutionResult> selectedResults = new CandidateCoverageEvaluator(executor)
                .selectCoverageImprovingResults(List.of(
                        firstCandidate,
                        duplicateCandidate,
                        newBranchCandidate
                ));

        assertEquals(List.of(firstCandidate, newBranchCandidate), selectedResults.stream()
                .map(TestCandidateExecutionResult::getCandidate)
                .toList());
        assertEquals(List.of(firstCandidate, duplicateCandidate, newBranchCandidate), executor.executedCandidates);
    }


    @Test
    void shouldReturnEvaluationWithExecutedAndSelectedResults() {
        TestCandidate firstCandidate = candidate("-1");
        TestCandidate failedCandidate = candidate("0");
        TestCandidate exceptionCandidate = candidate("101");

        FakeExecutor executor = new FakeExecutor(List.of(
                result(firstCandidate, branch(BranchType.FALSE)),
                TestCandidateExecutionResult.failedToExecute(
                        failedCandidate,
                        List.of(),
                        "java.lang.IllegalStateException",
                        "cannot execute"
                ),
                TestCandidateExecutionResult.threwException(
                        exceptionCandidate,
                        List.of(branch(BranchType.TRUE)),
                        "java.lang.IllegalArgumentException",
                        "amount must be positive"
                )
        ));

        CandidateCoverageEvaluation evaluation = new CandidateCoverageEvaluator(executor)
                .evaluate(List.of(firstCandidate, failedCandidate, exceptionCandidate), Set.of("sample.Calculator.calculate"), 0);

        assertEquals(List.of(firstCandidate, failedCandidate, exceptionCandidate), evaluation.getExecutedResults().stream()
                .map(TestCandidateExecutionResult::getCandidate)
                .toList());
        assertEquals(List.of(firstCandidate, exceptionCandidate), evaluation.getSelectedResults().stream()
                .map(TestCandidateExecutionResult::getCandidate)
                .toList());
        assertEquals(3, evaluation.getExecutedCandidateCount());
        assertEquals(2, evaluation.getSelectedCandidateCount());
        assertEquals(1, evaluation.countExecutedByOutcome(ExecutionOutcome.RETURNED));
        assertEquals(1, evaluation.countExecutedByOutcome(ExecutionOutcome.FAILED_TO_EXECUTE));
        assertEquals(1, evaluation.countExecutedByOutcome(ExecutionOutcome.THREW_EXCEPTION));
        assertEquals(2, evaluation.getCoveredBranchIds().size());
    }

    private static TestCandidateExecutionResult result(TestCandidate candidate, BranchId branchId) {
        return TestCandidateExecutionResult.returned(candidate, List.of(branchId), "10");
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

    private static final class FakeExecutor implements TestCandidateExecutor {
        private final List<TestCandidateExecutionResult> results;
        private final List<TestCandidate> executedCandidates = new ArrayList<>();
        private int nextResultIndex;

        private FakeExecutor(List<TestCandidateExecutionResult> results) {
            this.results = results;
        }

        @Override
        public TestCandidateExecutionResult execute(TestCandidate candidate) {
            executedCandidates.add(candidate);

            TestCandidateExecutionResult result = results.get(nextResultIndex);
            nextResultIndex++;

            return result;
        }
    }
}
