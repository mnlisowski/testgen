package com.mlisows.testgen.usecase.ports;

import com.mlisows.testgen.domain.analysis.CoverageGoal;

import java.nio.file.Path;
import java.util.List;

public interface SourceInstrumenter {
    default boolean shouldInstrument(Path sourcePath, List<CoverageGoal> coverageGoals) {
        return !coverageGoals.isEmpty();
    }

    Path instrument(Path sourcePath, Path outputPath, List<CoverageGoal> coverageGoals);
}
