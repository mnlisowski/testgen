package com.mlisows.testgen.domain.execution;

import com.mlisows.testgen.domain.generation.GeneratedArgument;
import com.mlisows.testgen.domain.generation.GeneratedSetupObject;
import com.mlisows.testgen.domain.generation.TestCandidate;

import com.mlisows.testgen.domain.analysis.BranchId;
import com.mlisows.testgen.domain.analysis.BranchKind;
import com.mlisows.testgen.domain.analysis.BranchType;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestCandidateExecutionResultTest {

    @Test
    void shouldCreateReturnedExecutionResult() {
        TestCandidate candidate = candidate();
        BranchId branchId = branchId();

        TestCandidateExecutionResult result = TestCandidateExecutionResult.returned(
                candidate,
                List.of(branchId),
                "10"
        );

        assertEquals(candidate, result.getCandidate());
        assertEquals(ExecutionOutcome.RETURNED, result.getOutcome());
        assertEquals(List.of(branchId), result.getCoveredBranches());
        assertEquals("10", result.getReturnValue().orElseThrow());
        assertTrue(result.getExceptionType().isEmpty());
        assertTrue(result.getExceptionMessage().isEmpty());
    }

    @Test
    void shouldCreateThrownExceptionExecutionResult() {
        TestCandidate candidate = candidate();
        BranchId branchId = branchId();

        TestCandidateExecutionResult result = TestCandidateExecutionResult.threwException(
                candidate,
                List.of(branchId),
                "java.lang.IllegalArgumentException",
                "amount must be positive"
        );

        assertEquals(candidate, result.getCandidate());
        assertEquals(ExecutionOutcome.THREW_EXCEPTION, result.getOutcome());
        assertEquals(List.of(branchId), result.getCoveredBranches());
        assertTrue(result.getReturnValue().isEmpty());
        assertEquals("java.lang.IllegalArgumentException", result.getExceptionType().orElseThrow());
        assertEquals("amount must be positive", result.getExceptionMessage().orElseThrow());
    }

    @Test
    void shouldCreateTimedOutExecutionResult() {
        TestCandidateExecutionResult result = TestCandidateExecutionResult.timedOut(
                candidate(),
                List.of(branchId())
        );

        assertEquals(ExecutionOutcome.TIMED_OUT, result.getOutcome());
        assertTrue(result.getReturnValue().isEmpty());
        assertTrue(result.getExceptionType().isEmpty());
        assertTrue(result.getExceptionMessage().isEmpty());
    }

    @Test
    void shouldCreateFailedExecutionResult() {
        TestCandidateExecutionResult result = TestCandidateExecutionResult.failedToExecute(
                candidate(),
                List.of(),
                "java.lang.ClassNotFoundException",
                "sample.Calculator"
        );

        assertEquals(ExecutionOutcome.FAILED_TO_EXECUTE, result.getOutcome());
        assertEquals("java.lang.ClassNotFoundException", result.getExceptionType().orElseThrow());
        assertEquals("sample.Calculator", result.getExceptionMessage().orElseThrow());
    }

    @Test
    void shouldDefensivelyCopyCoveredBranches() {
        List<BranchId> coveredBranches = new ArrayList<>();
        coveredBranches.add(branchId());

        TestCandidateExecutionResult result = TestCandidateExecutionResult.returned(
                candidate(),
                coveredBranches,
                "10"
        );

        coveredBranches.clear();

        assertEquals(1, result.getCoveredBranches().size());
    }

    private static TestCandidate candidate() {
        return new TestCandidate(
                "sample.Calculator",
                "calculate",
                "int",
                List.of(new GeneratedSetupObject("Calculator", "calculator", List.of())),
                "calculator",
                List.of(new GeneratedArgument("int", "-1"))
        );
    }

    private static BranchId branchId() {
        return new BranchId(
                "sample.Calculator",
                "calculate",
                10,
                BranchKind.IF,
                BranchType.TRUE,
                ""
        );
    }
}
