package com.mlisows.testgen.usecase.ports;

import com.mlisows.testgen.domain.analysis.CoverageGoal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface CandidateExecutorFactory {
    TestCandidateExecutor prepare(Path projectRoot, Path workRoot, Map<Path, List<CoverageGoal>> goals);
}
