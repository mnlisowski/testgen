package com.mlisows.testgen.infrastructure.parser;

import com.mlisows.testgen.domain.BranchKind;
import com.mlisows.testgen.domain.BranchType;
import com.mlisows.testgen.domain.ClassAnalysisResult;
import com.mlisows.testgen.domain.CoverageGoal;
import com.mlisows.testgen.domain.ArgumentValueHint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaParserCodeAnalyzerTest {

    @TempDir
    Path tempDir;

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

    @Test
    void shouldDetectCoverageGoalsForSwitchStatement() {
        JavaParserCodeAnalyzer analyzer = new JavaParserCodeAnalyzer();

        ClassAnalysisResult result = analyzer.analyze(Path.of(
                "src/test/resources/sample/SwitchDiscountCalculator.java"
        ));

        assertEquals("sample.SwitchDiscountCalculator", result.getClassName());
        assertEquals(4, result.getCoverageGoals().size());

        long caseGoalsCount = result.getCoverageGoals().stream()
                .filter(goal -> goal.getBranchId().getBranchKind() == BranchKind.SWITCH)
                .filter(goal -> goal.getBranchId().getBranchType() == BranchType.CASE)
                .count();

        long defaultGoalsCount = result.getCoverageGoals().stream()
                .filter(goal -> goal.getBranchId().getBranchKind() == BranchKind.SWITCH)
                .filter(goal -> goal.getBranchId().getBranchType() == BranchType.DEFAULT)
                .count();

        assertEquals(3, caseGoalsCount);
        assertEquals(1, defaultGoalsCount);
    }

    @Test
    void shouldAnalyzeSourceClassMatchingFileNameAndIgnoreInnerClassMethods() throws Exception {
        Path sourcePath = tempDir.resolve("Calculator.java");

        Files.writeString(sourcePath, """
                package sample;

                class Helper {
                    int ignored(int amount) {
                        if (amount > 0) {
                            return 1;
                        }
                        return 0;
                    }
                }

                public class Calculator {
                    int calculate(int amount) {
                        if (amount > 100) {
                            return 10;
                        }
                        return 0;
                    }

                    class Inner {
                        int ignoredInner(int amount) {
                            if (amount > 5) {
                                return 1;
                            }
                            return 0;
                        }
                    }
                }
                """);

        ClassAnalysisResult result = new JavaParserCodeAnalyzer().analyze(sourcePath);

        assertEquals("sample.Calculator", result.getClassName());
        assertEquals(2, result.getCoverageGoals().size());

        List<String> methodNames = result.getCoverageGoals().stream()
                .map(goal -> goal.getBranchId().getMethodName())
                .distinct()
                .toList();

        assertEquals(List.of("calculate"), methodNames);
    }

    @Test
    void shouldCollectBoundaryHintsForDirectNumericParameterCondition() throws Exception {
        Path sourcePath = tempDir.resolve("Calculator.java");

        Files.writeString(sourcePath, """
                package sample;

                public class Calculator {
                    int calculate(int amount) {
                        if (amount > 100) {
                            return 10;
                        }
                        return 0;
                    }
                }
                """);

        ClassAnalysisResult result = new JavaParserCodeAnalyzer().analyze(sourcePath);

        assertEquals(
                List.of("99", "100", "101"),
                valuesForSlot(result, "sample.Calculator.calculate(int).amount")
        );
    }

    @Test
    void shouldCollectStringHintsForDirectMethodCallCondition() throws Exception {
        Path sourcePath = tempDir.resolve("Calculator.java");

        Files.writeString(sourcePath, """
                package sample;

                public class Calculator {
                    int calculate(String status) {
                        if (status.equals("VIP")) {
                            return 10;
                        }
                        if ("BLOCKED".equals(status)) {
                            return -1;
                        }
                        return 0;
                    }
                }
                """);

        ClassAnalysisResult result = new JavaParserCodeAnalyzer().analyze(sourcePath);

        assertEquals(
                List.of("\"VIP\"", "\"BLOCKED\""),
                valuesForSlot(result, "sample.Calculator.calculate(String).status")
        );
    }

    @Test
    void shouldCollectSwitchCaseHintsForSelectorParameter() throws Exception {
        Path sourcePath = tempDir.resolve("Calculator.java");

        Files.writeString(sourcePath, """
                package sample;

                public class Calculator {
                    int calculate(int level) {
                        switch (level) {
                            case 1:
                                return 10;
                            case 2:
                                return 20;
                            default:
                                return 0;
                        }
                    }
                }
                """);

        ClassAnalysisResult result = new JavaParserCodeAnalyzer().analyze(sourcePath);

        assertEquals(
                List.of("1", "2"),
                valuesForSlot(result, "sample.Calculator.calculate(int).level")
        );
    }

    private static List<String> valuesForSlot(ClassAnalysisResult result, String slotId) {
        return result.getArgumentValueHints().stream()
                .filter(hint -> hint.slotId().equals(slotId))
                .map(ArgumentValueHint::getValue)
                .toList();
    }


}
