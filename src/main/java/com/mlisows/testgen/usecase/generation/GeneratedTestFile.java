package com.mlisows.testgen.usecase.generation;

import java.nio.file.Path;
import java.util.Objects;

public record GeneratedTestFile(Path relativePath, String content) {
    public GeneratedTestFile {
        Objects.requireNonNull(relativePath, "relativePath");
        Objects.requireNonNull(content, "content");
        relativePath = relativePath.normalize();
        if (relativePath.isAbsolute() || relativePath.startsWith("..") || relativePath.toString().isEmpty()) {
            throw new IllegalArgumentException("Test path must be relative to the output directory");
        }
    }
}
