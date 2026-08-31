package com.mlisows.testgen.infrastructure.instrumentation;

import com.mlisows.testgen.domain.ClassAnalysisResult;
import com.mlisows.testgen.infrastructure.parser.JavaParserCodeAnalyzer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaParserBranchInstrumentationPipelineTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldInstrumentIfBranchesDetectedByAnalyzer() throws Exception {
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

        ClassAnalysisResult analysisResult = new JavaParserCodeAnalyzer().analyze(sourcePath);

        new JavaParserBranchInstrumenter().instrument(
                sourcePath,
                outputPath,
                analysisResult.getCoverageGoals()
        );

        String instrumentedSource = Files.readString(outputPath);

        assertTrue(instrumentedSource.contains(
                "com.mlisows.testgen.infrastructure.runtime.BranchRecorder.hit(\"sample.Calculator.calculate.L5.IF.TRUE\")"
        ));
        assertTrue(instrumentedSource.contains(
                "com.mlisows.testgen.infrastructure.runtime.BranchRecorder.hit(\"sample.Calculator.calculate.L5.IF.FALSE\")"
        ));
    }
}
