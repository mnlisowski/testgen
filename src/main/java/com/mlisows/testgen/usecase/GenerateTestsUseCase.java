package com.mlisows.testgen.usecase;

import com.mlisows.testgen.domain.ClassAnalysisResult;
import com.mlisows.testgen.usecase.ports.CodeAnalyzer;

import java.nio.file.Path;
import java.util.Objects;

public final class GenerateTestsUseCase {
    private final CodeAnalyzer codeAnalyzer;

    public GenerateTestsUseCase(CodeAnalyzer codeAnalyzer) {
        this.codeAnalyzer = Objects.requireNonNull(codeAnalyzer, "codeAnalyzer must not be null");
    }

    public ClassAnalysisResult execute(Path sourcePath) {
        Objects.requireNonNull(sourcePath, "sourcePath must not be null");

        return codeAnalyzer.analyze(sourcePath);
    }
}

