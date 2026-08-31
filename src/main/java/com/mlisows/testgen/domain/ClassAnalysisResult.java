package com.mlisows.testgen.domain;

import java.util.List;
import java.util.Objects;

public final class ClassAnalysisResult {
    private final String className;
    private final List<CoverageGoal> coverageGoals;
    private final List<StaticArgumentValueHint> staticArgumentValueHints;

    public ClassAnalysisResult(String className, List<CoverageGoal> coverageGoals) {
        this(className, coverageGoals, List.of());
    }

    public ClassAnalysisResult(
            String className,
            List<CoverageGoal> coverageGoals,
            List<StaticArgumentValueHint> staticArgumentValueHints
    ) {
        this.className = Objects.requireNonNull(className, "className must not be null");
        this.coverageGoals = List.copyOf(Objects.requireNonNull(coverageGoals, "coverageGoals must not be null"));
        this.staticArgumentValueHints = List.copyOf(Objects.requireNonNull(
                staticArgumentValueHints,
                "staticArgumentValueHints must not be null"
        ));
    }

    public String getClassName() {
        return className;
    }

    public List<CoverageGoal> getCoverageGoals() {
        return coverageGoals;
    }

    public List<StaticArgumentValueHint> getStaticArgumentValueHints() {
        return staticArgumentValueHints;
    }
}
