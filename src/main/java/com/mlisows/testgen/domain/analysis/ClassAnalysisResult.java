package com.mlisows.testgen.domain.analysis;

import com.mlisows.testgen.domain.ArgumentValueHint;

import java.util.List;
import java.util.Objects;

public final class ClassAnalysisResult {
    private final String className;
    private final List<CoverageGoal> coverageGoals;
    private final List<ArgumentValueHint> argumentValueHints;

    public ClassAnalysisResult(String className, List<CoverageGoal> coverageGoals) {
        this(className, coverageGoals, List.of());
    }

    public ClassAnalysisResult(
            String className,
            List<CoverageGoal> coverageGoals,
            List<ArgumentValueHint> argumentValueHints
    ) {
        this.className = Objects.requireNonNull(className, "className must not be null");
        this.coverageGoals = List.copyOf(Objects.requireNonNull(coverageGoals, "coverageGoals must not be null"));
        this.argumentValueHints = List.copyOf(Objects.requireNonNull(
                argumentValueHints,
                "argumentValueHints must not be null"
        ));
    }

    public String getClassName() {
        return className;
    }

    public List<CoverageGoal> getCoverageGoals() {
        return coverageGoals;
    }

    public List<ArgumentValueHint> getArgumentValueHints() {
        return argumentValueHints;
    }
}
