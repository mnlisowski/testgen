package com.mlisows.testgen.domain.execution;

import com.mlisows.testgen.domain.analysis.BranchId;
import com.mlisows.testgen.domain.generation.TestCandidate;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class TestCandidateExecutionResult {
    private final TestCandidate candidate;
    private final ExecutionOutcome outcome;
    private final List<BranchId> coveredBranches;
    private final String returnValue;
    private final String exceptionType;
    private final String exceptionMessage;

    private TestCandidateExecutionResult(
            TestCandidate candidate,
            ExecutionOutcome outcome,
            List<BranchId> coveredBranches,
            String returnValue,
            String exceptionType,
            String exceptionMessage
    ) {
        this.candidate = Objects.requireNonNull(candidate, "candidate must not be null");
        this.outcome = Objects.requireNonNull(outcome, "outcome must not be null");
        this.coveredBranches = List.copyOf(Objects.requireNonNull(coveredBranches, "coveredBranches must not be null"));
        this.returnValue = returnValue;
        this.exceptionType = exceptionType;
        this.exceptionMessage = exceptionMessage;
    }

    public static TestCandidateExecutionResult returned(
            TestCandidate candidate,
            List<BranchId> coveredBranches,
            String returnValue
    ) {
        return new TestCandidateExecutionResult(
                candidate,
                ExecutionOutcome.RETURNED,
                coveredBranches,
                returnValue,
                null,
                null
        );
    }

    public static TestCandidateExecutionResult threwException(
            TestCandidate candidate,
            List<BranchId> coveredBranches,
            String exceptionType,
            String exceptionMessage
    ) {
        return new TestCandidateExecutionResult(
                candidate,
                ExecutionOutcome.THREW_EXCEPTION,
                coveredBranches,
                null,
                Objects.requireNonNull(exceptionType, "exceptionType must not be null"),
                exceptionMessage
        );
    }

    public static TestCandidateExecutionResult timedOut(
            TestCandidate candidate,
            List<BranchId> coveredBranches
    ) {
        return new TestCandidateExecutionResult(
                candidate,
                ExecutionOutcome.TIMED_OUT,
                coveredBranches,
                null,
                null,
                null
        );
    }

    public static TestCandidateExecutionResult failedToExecute(
            TestCandidate candidate,
            List<BranchId> coveredBranches,
            String exceptionType,
            String exceptionMessage
    ) {
        return new TestCandidateExecutionResult(
                candidate,
                ExecutionOutcome.FAILED_TO_EXECUTE,
                coveredBranches,
                null,
                exceptionType,
                exceptionMessage
        );
    }

    public TestCandidate getCandidate() {
        return candidate;
    }

    public ExecutionOutcome getOutcome() {
        return outcome;
    }

    public List<BranchId> getCoveredBranches() {
        return coveredBranches;
    }

    public Optional<String> getReturnValue() {
        return Optional.ofNullable(returnValue);
    }

    public Optional<String> getExceptionType() {
        return Optional.ofNullable(exceptionType);
    }

    public Optional<String> getExceptionMessage() {
        return Optional.ofNullable(exceptionMessage);
    }
}
