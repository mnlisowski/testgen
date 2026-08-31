package com.mlisows.testgen.usecase.ports;

import com.mlisows.testgen.domain.CoverageGoal;

import java.nio.file.Path;
import java.util.List;

public interface SourceInstrumenter {
    Path instrument(Path sourcePath, Path outputPath, List<CoverageGoal> coverageGoals);
}
