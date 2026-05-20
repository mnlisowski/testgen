package com.mlisows.testgen.infrastructure.writer;

import com.mlisows.testgen.domain.GeneratedTestSuite;
import com.mlisows.testgen.usecase.ports.TestFileWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class GeneratedTestFileWriter implements TestFileWriter {
    private final Path outputRoot;

    public GeneratedTestFileWriter(Path outputRoot) {
        this.outputRoot = Objects.requireNonNull(outputRoot, "outputRoot must not be null");
    }

    @Override
    public Path write(GeneratedTestSuite testSuite, String code) {
        Objects.requireNonNull(testSuite, "testSuite must not be null");
        Objects.requireNonNull(code, "code must not be null");

        Path outputPath = outputPathFor(testSuite);

        try {
            Files.createDirectories(outputPath.getParent());
            Files.writeString(outputPath, code);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write generated test file: " + outputPath, exception);
        }

        return outputPath;
    }

    private Path outputPathFor(GeneratedTestSuite testSuite) {
        String relativePath = testSuite.getClassName().replace('.', '/');
        return outputRoot.resolve(relativePath + "Test.java");
    }
}
