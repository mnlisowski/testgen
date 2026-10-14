package com.mlisows.testgen.usecase.generation;

import com.mlisows.testgen.usecase.JUnitCandidateTestWriter;

import com.mlisows.testgen.domain.execution.ExecutionOutcome;
import com.mlisows.testgen.domain.execution.TestCandidateExecutionResult;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class GeneratedTestRenderer {
    private final JUnitCandidateTestWriter testWriter;

    public GeneratedTestRenderer() {
        this(new JUnitCandidateTestWriter());
    }

    public GeneratedTestRenderer(JUnitCandidateTestWriter testWriter) {
        this.testWriter = Objects.requireNonNull(testWriter, "testWriter");
    }

    public List<GeneratedTestFile> render(List<TestCandidateExecutionResult> selectedResults) {
        return selectedResultClassNames(selectedResults).stream()
                .map(className -> new GeneratedTestFile(
                        Path.of(className.replace('.', '/') + "Test.java"),
                        testWriter.write(className, selectedResults)))
                .toList();
    }

    private List<String> selectedResultClassNames(List<TestCandidateExecutionResult> selectedResults) {
        return selectedResults.stream()
                .filter(this::canWriteCandidateTest)
                .map(result -> result.getCandidate().getClassName())
                .distinct()
                .sorted()
                .toList();
    }

    private boolean canWriteCandidateTest(TestCandidateExecutionResult result) {
        return result.getOutcome() == ExecutionOutcome.RETURNED
                || result.getOutcome() == ExecutionOutcome.THREW_EXCEPTION;
    }

}
