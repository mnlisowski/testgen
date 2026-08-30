package com.mlisows.testgen.usecase.ports;

import com.mlisows.testgen.domain.TestCandidate;
import com.mlisows.testgen.domain.TestCandidateExecutionResult;

public interface TestCandidateExecutor {
    TestCandidateExecutionResult execute(TestCandidate candidate);
}
