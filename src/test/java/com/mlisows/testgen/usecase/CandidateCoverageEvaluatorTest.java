package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.BranchId;
import com.mlisows.testgen.domain.BranchKind;
import com.mlisows.testgen.domain.BranchType;
import com.mlisows.testgen.domain.GeneratedArgument;
import com.mlisows.testgen.domain.GeneratedSetupObject;
import com.mlisows.testgen.domain.TestCandidate;
import com.mlisows.testgen.domain.TestCandidateExecutionResult;
import com.mlisows.testgen.usecase.ports.TestCandidateExecutor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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
