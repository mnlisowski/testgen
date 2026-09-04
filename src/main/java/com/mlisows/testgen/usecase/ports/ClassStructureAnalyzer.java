package com.mlisows.testgen.usecase.ports;

import com.mlisows.testgen.domain.structure.ClassStructure;

import java.nio.file.Path;

public interface ClassStructureAnalyzer {
    ClassStructure analyze(Path sourcePath);
}

