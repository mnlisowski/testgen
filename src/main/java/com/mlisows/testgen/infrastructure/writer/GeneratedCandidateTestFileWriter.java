package com.mlisows.testgen.infrastructure.writer;

import com.mlisows.testgen.domain.execution.TestCandidateExecutionResult;
import com.mlisows.testgen.usecase.JUnitCandidateTestWriter;
import com.mlisows.testgen.usecase.generation.GeneratedTestRenderer;
import com.mlisows.testgen.usecase.generation.GeneratedTestFile;

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

        writeFiles(outputRoot, new GeneratedTestRenderer(testWriter).render(selectedResults));
    }

    public void writeFiles(Path outputRoot, List<GeneratedTestFile> files) {
        Objects.requireNonNull(outputRoot, "outputRoot");
        for (GeneratedTestFile file : files) {
            writeText(outputRoot.resolve(file.relativePath()), file.content());
        }
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
