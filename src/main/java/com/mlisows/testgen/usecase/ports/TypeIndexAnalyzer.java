package com.mlisows.testgen.usecase.ports;

import com.mlisows.testgen.domain.structure.ProjectTypeIndex;

import java.nio.file.Path;
import java.util.List;

public interface TypeIndexAnalyzer {
    ProjectTypeIndex analyze(List<Path> sourcePaths);
}
