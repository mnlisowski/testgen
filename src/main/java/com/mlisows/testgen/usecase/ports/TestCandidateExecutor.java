package com.mlisows.testgen.usecase.ports;

import com.mlisows.testgen.domain.generation.TestCandidate;
import com.mlisows.testgen.domain.execution.TestCandidateExecutionResult;

public interface TestCandidateExecutor {
    TestCandidateExecutionResult execute(TestCandidate candidate);
}
