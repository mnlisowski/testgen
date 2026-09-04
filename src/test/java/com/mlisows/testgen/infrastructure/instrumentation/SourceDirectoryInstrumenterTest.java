package com.mlisows.testgen.infrastructure.instrumentation;

import com.mlisows.testgen.domain.analysis.ClassAnalysisResult;
import com.mlisows.testgen.infrastructure.parser.JavaParserCodeAnalyzer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceDirectoryInstrumenterTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldInstrumentJavaFilesWithCoverageGoalsAndCopyOtherFiles() throws Exception {
        Path sourceRoot = tempDir.resolve("src/main/java");
        Path outputRoot = tempDir.resolve("instrumented-src");

        Path calculatorPath = sourceRoot.resolve("sample/Calculator.java");
        Path enumPath = sourceRoot.resolve("sample/CustomerType.java");

        Files.createDirectories(calculatorPath.getParent());

        Files.writeString(calculatorPath, """
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

        Files.writeString(enumPath, """
                  package sample;

                  public enum CustomerType {
                      REGULAR,
                      PREMIUM
                  }
                  """);

        ClassAnalysisResult analysisResult = new JavaParserCodeAnalyzer().analyze(calculatorPath);

        new SourceDirectoryInstrumenter(new JavaParserBranchInstrumenter()).instrument(
                sourceRoot,
                outputRoot,
                Map.of(calculatorPath, analysisResult.getCoverageGoals())
        );

        Path instrumentedCalculatorPath = outputRoot.resolve("sample/Calculator.java");
        Path copiedEnumPath = outputRoot.resolve("sample/CustomerType.java");

        String instrumentedCalculator = Files.readString(instrumentedCalculatorPath);
        String copiedEnum = Files.readString(copiedEnumPath);

        assertTrue(instrumentedCalculator.contains("BranchRecorder.hit"));
        assertEquals(Files.readString(enumPath), copiedEnum);
    }
}
