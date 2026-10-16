package com.mlisows.testgen.infrastructure.persistence.job;

import com.mlisows.testgen.usecase.generation.GeneratedTestFile;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.nio.file.Path;

/** One row of generated_test_files: a test class path and its source code. */
@Embeddable
public class GeneratedTestFileEmbeddable {
    @Column(name = "relative_path", nullable = false, columnDefinition = "TEXT")
    private String relativePath;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    protected GeneratedTestFileEmbeddable() {
    }

    public GeneratedTestFileEmbeddable(GeneratedTestFile testFile) {
        this.relativePath = testFile.relativePath().toString();
        this.content = testFile.content();
    }

    public GeneratedTestFile toTestFile() {
        return new GeneratedTestFile(Path.of(relativePath), content);
    }
}
