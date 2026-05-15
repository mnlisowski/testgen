package com.mlisows.testgen.domain;

import java.util.List;
import java.util.Objects;

public final class ClassAnalysisResult {
    private final String className;
    private final List<CoverageGoal> coverageGoals;

    public ClassAnalysisResult(String className, List<CoverageGoal> coverageGoals) {
        this.className = Objects.requireNonNull(className, "className must not be null");
        this.coverageGoals = List.copyOf(Objects.requireNonNull(coverageGoals, "coverageGoals must not be null"));
    }

    public String getClassName() {
        return className;
    }

    public List<CoverageGoal> getCoverageGoals() {
        return coverageGoals;
    }
}
