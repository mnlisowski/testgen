package com.mlisows.testgen.infrastructure.parser;

import com.mlisows.testgen.domain.BranchKind;
import com.mlisows.testgen.domain.BranchType;
import com.mlisows.testgen.domain.ClassAnalysisResult;
import com.mlisows.testgen.domain.CoverageGoal;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaParserCodeAnalyzerTest {


    @Test
    void shouldDetectCoverageGoalsForLoopStatements() {
        JavaParserCodeAnalyzer analyzer = new JavaParserCodeAnalyzer();

        ClassAnalysisResult result = analyzer.analyze(Path.of(
                "src/test/resources/sample/LoopExamples.java"
        ));

        assertEquals("sample.LoopExamples", result.getClassName());
        assertEquals(4, result.getCoverageGoals().size());

        long whileGoalsCount = result.getCoverageGoals().stream()
                .filter(goal -> goal.getBranchId().getBranchKind() == BranchKind.WHILE)
                .count();

        long forGoalsCount = result.getCoverageGoals().stream()
                .filter(goal -> goal.getBranchId().getBranchKind() == BranchKind.FOR)
                .count();

        assertEquals(2, whileGoalsCount);
        assertEquals(2, forGoalsCount);
    }

    @Test
    void shouldDetectCoverageGoalsForMultipleIfStatements() {
        JavaParserCodeAnalyzer analyzer = new JavaParserCodeAnalyzer();

        ClassAnalysisResult result = analyzer.analyze(Path.of(
                "src/test/resources/sample/MultiBranchCalculator.java"
        ));

        assertEquals("sample.MultiBranchCalculator", result.getClassName());
        assertEquals(6, result.getCoverageGoals().size());

        long calculateGoalsCount = result.getCoverageGoals().stream()
                .filter(goal -> goal.getBranchId().getMethodName().equals("calculate"))
                .count();

        long isPositiveGoalsCount = result.getCoverageGoals().stream()
                .filter(goal -> goal.getBranchId().getMethodName().equals("isPositive"))
                .count();

        assertEquals(4, calculateGoalsCount);
        assertEquals(2, isPositiveGoalsCount);
    }

    @Test
    void shouldDetectTrueAndFalseCoverageGoalsForIfStatement() {
        JavaParserCodeAnalyzer analyzer = new JavaParserCodeAnalyzer();

        ClassAnalysisResult result = analyzer.analyze(Path.of(
                "src/test/resources/sample/SimpleDiscountCalculator.java"
        ));

        assertEquals("sample.SimpleDiscountCalculator", result.getClassName());
        assertEquals(2, result.getCoverageGoals().size());

        List<BranchType> branchTypes = result.getCoverageGoals().stream()
                .map(CoverageGoal::getBranchId)
                .map(branchId -> branchId.getBranchType())
                .toList();

        assertEquals(List.of(BranchType.TRUE, BranchType.FALSE), branchTypes);

        for (CoverageGoal goal : result.getCoverageGoals()) {
            assertEquals("amount > 100", goal.getCondition());
            assertEquals("calculate", goal.getBranchId().getMethodName());
        }
    }
}
