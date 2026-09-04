package com.mlisows.testgen.infrastructure.writer;

import com.mlisows.testgen.domain.execution.ExecutionOutcome;
import com.mlisows.testgen.domain.execution.TestCandidateExecutionResult;
import com.mlisows.testgen.usecase.JUnitCandidateTestWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class GeneratedCandidateTestFileWriter {
    private final JUnitCandidateTestWriter testWriter;

    public GeneratedCandidateTestFileWriter() {
        this(new JUnitCandidateTestWriter());
    }

    public GeneratedCandidateTestFileWriter(JUnitCandidateTestWriter testWriter) {
        this.testWriter = Objects.requireNonNull(testWriter, "testWriter must not be null");
    }

    public void write(Path outputRoot, List<TestCandidateExecutionResult> selectedResults) {
        Objects.requireNonNull(outputRoot, "outputRoot must not be null");
        Objects.requireNonNull(selectedResults, "selectedResults must not be null");

        for (String className : selectedResultClassNames(selectedResults)) {
            String code = testWriter.write(className, selectedResults);
            writeText(generatedTestPath(outputRoot, className), code);
        }
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

    private Path generatedTestPath(Path outputRoot, String className) {
        return outputRoot.resolve(className.replace('.', '/') + "Test.java");
    }

    private void writeText(Path path, String text) {
        try {
            Path parent = path.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(path, text);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write file: " + path, exception);
        }
    }
}
