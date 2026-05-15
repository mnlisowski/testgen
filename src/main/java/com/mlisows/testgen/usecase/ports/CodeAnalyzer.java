package com.mlisows.testgen.usecase.ports;

import com.mlisows.testgen.domain.ClassAnalysisResult;

import java.nio.file.Path;

public interface CodeAnalyzer {
    ClassAnalysisResult analyze(Path sourcePath);
}
