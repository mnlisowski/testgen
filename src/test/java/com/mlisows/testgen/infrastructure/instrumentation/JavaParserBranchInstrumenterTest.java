package com.mlisows.testgen.infrastructure.instrumentation;

import com.mlisows.testgen.domain.BranchId;
import com.mlisows.testgen.domain.BranchKind;
import com.mlisows.testgen.domain.BranchType;
import com.mlisows.testgen.domain.CoverageGoal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaParserBranchInstrumenterTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldInstrumentIfBranchesUsingCoverageGoals() throws Exception {
        Path sourcePath = tempDir.resolve("Calculator.java");
        Path outputPath = tempDir.resolve("instrumented/Calculator.java");

        Files.writeString(sourcePath, """
                  package sample;

                  public class Calculator {
                      public int calculate(int amount) {
                          if (amount > 100) {
                              return 10;
                          } else {
                              return 0;
                          }
                      }
                  }
                  """);

        new JavaParserBranchInstrumenter().instrument(
                sourcePath,
                outputPath,
                List.of(
                        goal("sample.Calculator", "calculate", 5, BranchType.TRUE),
                        goal("sample.Calculator", "calculate", 5, BranchType.FALSE)
                )
        );

        String instrumentedSource = Files.readString(outputPath);

        assertTrue(instrumentedSource.contains(
                "com.mlisows.testgen.infrastructure.runtime.BranchRecorder.hit(\"sample.Calculator.calculate.L5.IF.TRUE\")"
        ));
        assertTrue(instrumentedSource.contains(
                "com.mlisows.testgen.infrastructure.runtime.BranchRecorder.hit(\"sample.Calculator.calculate.L5.IF.FALSE\")"
        ));
    }

    @Test
    void shouldAddElseBlockForIfWithoutElse() throws Exception {
        Path sourcePath = tempDir.resolve("Calculator.java");
        Path outputPath = tempDir.resolve("instrumented/Calculator.java");

        Files.writeString(sourcePath, """
                  package sample;

                  public class Calculator {
                      public int calculate(int amount) {
                          int discount = 0;
                          if (amount > 100) {
                              discount = 10;
                          }
                          return discount;
                      }
                  }
                  """);

        new JavaParserBranchInstrumenter().instrument(
                sourcePath,
                outputPath,
                List.of(
                        goal("sample.Calculator", "calculate", 6, BranchType.TRUE),
                        goal("sample.Calculator", "calculate", 6, BranchType.FALSE)
                )
        );

        String instrumentedSource = Files.readString(outputPath);

        assertTrue(instrumentedSource.contains(
                "com.mlisows.testgen.infrastructure.runtime.BranchRecorder.hit(\"sample.Calculator.calculate.L6.IF.TRUE\")"
        ));
        assertTrue(instrumentedSource.contains(
                "com.mlisows.testgen.infrastructure.runtime.BranchRecorder.hit(\"sample.Calculator.calculate.L6.IF.FALSE\")"
        ));
        assertTrue(instrumentedSource.contains("else"));
    }

    @Test
    void shouldInstrumentIfElseWithoutBlocks() throws Exception {
        Path sourcePath = tempDir.resolve("Calculator.java");
        Path outputPath = tempDir.resolve("instrumented/Calculator.java");

        Files.writeString(sourcePath, """
                  package sample;

                  public class Calculator {
                      public int calculate(int amount) {
                          if (amount > 100)
                              return 10;
                          else
                              return 0;
                      }
                  }
                  """);

        new JavaParserBranchInstrumenter().instrument(
                sourcePath,
                outputPath,
                List.of(
                        goal("sample.Calculator", "calculate", 5, BranchType.TRUE),
                        goal("sample.Calculator", "calculate", 5, BranchType.FALSE)
                )
        );

        String instrumentedSource = Files.readString(outputPath);

        assertTrue(instrumentedSource.contains("if (amount > 100) {"));
        assertTrue(instrumentedSource.contains("} else {"));
        assertTrue(instrumentedSource.contains(
                "com.mlisows.testgen.infrastructure.runtime.BranchRecorder.hit(\"sample.Calculator.calculate.L5.IF.TRUE\")"
        ));
        assertTrue(instrumentedSource.contains(
                "com.mlisows.testgen.infrastructure.runtime.BranchRecorder.hit(\"sample.Calculator.calculate.L5.IF.FALSE\")"
        ));
    }

    @Test
    void shouldLeaveIfUnchangedWhenCoverageGoalsAreMissing() throws Exception {
        Path sourcePath = tempDir.resolve("Calculator.java");
        Path outputPath = tempDir.resolve("instrumented/Calculator.java");

        Files.writeString(sourcePath, """
                  package sample;

                  public class Calculator {
                      public int calculate(int amount) {
                          if (amount > 100) {
                              return 10;
                          }
                          return 0;
                      }
                  }
                  """);

        new JavaParserBranchInstrumenter().instrument(
                sourcePath,
                outputPath,
                List.of()
        );

        String instrumentedSource = Files.readString(outputPath);

        assertFalse(instrumentedSource.contains("BranchRecorder.hit"));
    }

    @Test
    void shouldInstrumentForBranchesUsingCoverageGoals() throws Exception {
        Path sourcePath = tempDir.resolve("Calculator.java");
        Path outputPath = tempDir.resolve("instrumented/Calculator.java");

        Files.writeString(sourcePath, """
                  package sample;

                  public class Calculator {
                      public int sum(int limit) {
                          int result = 0;
                          for (int index = 0; index < limit; index++) {
                              result += index;
                          }
                          return result;
                      }
                  }
                  """);

        new JavaParserBranchInstrumenter().instrument(
                sourcePath,
                outputPath,
                List.of(
                        goal("sample.Calculator", "sum", 6, BranchKind.FOR, BranchType.TRUE),
                        goal("sample.Calculator", "sum", 6, BranchKind.FOR, BranchType.FALSE)
                )
        );

        String instrumentedSource = Files.readString(outputPath);

        assertTrue(instrumentedSource.contains(
                "com.mlisows.testgen.infrastructure.runtime.BranchRecorder.hit(\"sample.Calculator.sum.L6.FOR.TRUE\")"
        ));
        assertTrue(instrumentedSource.contains(
                "com.mlisows.testgen.infrastructure.runtime.BranchRecorder.hit(\"sample.Calculator.sum.L6.FOR.FALSE\")"
        ));
    }

    @Test
    void shouldInstrumentWhileBranchesUsingCoverageGoals() throws Exception {
        Path sourcePath = tempDir.resolve("Calculator.java");
        Path outputPath = tempDir.resolve("instrumented/Calculator.java");

        Files.writeString(sourcePath, """
                  package sample;

                  public class Calculator {
                      public int sum(int limit) {
                          int result = 0;
                          while (result < limit) {
                              result++;
                          }
                          return result;
                      }
                  }
                  """);

        new JavaParserBranchInstrumenter().instrument(
                sourcePath,
                outputPath,
                List.of(
                        goal("sample.Calculator", "sum", 6, BranchKind.WHILE, BranchType.TRUE),
                        goal("sample.Calculator", "sum", 6, BranchKind.WHILE, BranchType.FALSE)
                )
        );

        String instrumentedSource = Files.readString(outputPath);

        assertTrue(instrumentedSource.contains(
                "com.mlisows.testgen.infrastructure.runtime.BranchRecorder.hit(\"sample.Calculator.sum.L6.WHILE.TRUE\")"
        ));
        assertTrue(instrumentedSource.contains(
                "com.mlisows.testgen.infrastructure.runtime.BranchRecorder.hit(\"sample.Calculator.sum.L6.WHILE.FALSE\")"
        ));
    }

    @Test
    void shouldInstrumentSwitchBranchesUsingCoverageGoals() throws Exception {
        Path sourcePath = tempDir.resolve("Calculator.java");
        Path outputPath = tempDir.resolve("instrumented/Calculator.java");

        Files.writeString(sourcePath, """
                  package sample;

                  public class Calculator {
                      public int calculate(int level) {
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

        new JavaParserBranchInstrumenter().instrument(
                sourcePath,
                outputPath,
                List.of(
                        goal("sample.Calculator", "calculate", 5, BranchKind.SWITCH, BranchType.CASE, "1"),
                        goal("sample.Calculator", "calculate", 5, BranchKind.SWITCH, BranchType.CASE, "2"),
                        goal("sample.Calculator", "calculate", 5, BranchKind.SWITCH, BranchType.DEFAULT, "")
                )
        );

        String instrumentedSource = Files.readString(outputPath);

        assertTrue(instrumentedSource.contains(
                "com.mlisows.testgen.infrastructure.runtime.BranchRecorder.hit(\"sample.Calculator.calculate.L5.SWITCH.CASE.1\")"
        ));
        assertTrue(instrumentedSource.contains(
                "com.mlisows.testgen.infrastructure.runtime.BranchRecorder.hit(\"sample.Calculator.calculate.L5.SWITCH.CASE.2\")"
        ));
        assertTrue(instrumentedSource.contains(
                "com.mlisows.testgen.infrastructure.runtime.BranchRecorder.hit(\"sample.Calculator.calculate.L5.SWITCH.DEFAULT\")"
        ));
    }

    private static CoverageGoal goal(
            String className,
            String methodName,
            int lineNumber,
            BranchType branchType
    ) {
        return goal(className, methodName, lineNumber, BranchKind.IF, branchType);
    }

    private static CoverageGoal goal(
            String className,
            String methodName,
            int lineNumber,
            BranchKind branchKind,
            BranchType branchType
    ) {
        return goal(className, methodName, lineNumber, branchKind, branchType, "");
    }

    private static CoverageGoal goal(
            String className,
            String methodName,
            int lineNumber,
            BranchKind branchKind,
            BranchType branchType,
            String discriminator
    ) {
        return new CoverageGoal(
                new BranchId(
                        className,
                        methodName,
                        lineNumber,
                        branchKind,
                        branchType,
                        discriminator
                ),
                ""
        );
    }
}
