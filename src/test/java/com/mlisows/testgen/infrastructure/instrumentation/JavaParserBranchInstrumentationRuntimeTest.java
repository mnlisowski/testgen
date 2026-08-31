package com.mlisows.testgen.infrastructure.instrumentation;

import com.mlisows.testgen.domain.BranchId;
import com.mlisows.testgen.domain.BranchType;
import com.mlisows.testgen.domain.ClassAnalysisResult;
import com.mlisows.testgen.domain.CoverageGoal;
import com.mlisows.testgen.infrastructure.parser.JavaParserCodeAnalyzer;
import com.mlisows.testgen.infrastructure.runtime.BranchRecorder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JavaParserBranchInstrumentationRuntimeTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldRecordHitsWhenInstrumentedSourceIsExecuted() throws Exception {
        Path sourcePath = tempDir.resolve("Calculator.java");
        Path instrumentedPath = tempDir.resolve("instrumented/Calculator.java");
        Path classesPath = tempDir.resolve("classes");

        Files.createDirectories(classesPath);
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
                instrumentedPath,
                analysisResult.getCoverageGoals()
        );

        compile(instrumentedPath, classesPath);

        String trueBranchId = branchId(analysisResult, BranchType.TRUE);
        String falseBranchId = branchId(analysisResult, BranchType.FALSE);

        BranchRecorder.reset();

        try (URLClassLoader classLoader = new URLClassLoader(
                new URL[]{classesPath.toUri().toURL()},
                getClass().getClassLoader()
        )) {
            Class<?> calculatorClass = Class.forName("sample.Calculator", true, classLoader);
            Object calculator = calculatorClass.getDeclaredConstructor().newInstance();
            Method calculate = calculatorClass.getMethod("calculate", int.class);

            assertEquals(10, calculate.invoke(calculator, 150));
            assertEquals(0, calculate.invoke(calculator, 50));

            Map<String, Integer> hits = BranchRecorder.snapshot();

            assertEquals(1, hits.get(trueBranchId));
            assertEquals(1, hits.get(falseBranchId));
        } finally {
            BranchRecorder.reset();
        }
    }

    private static void compile(Path sourcePath, Path classesPath) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "JDK compiler must be available");

        ByteArrayOutputStream compilerOutput = new ByteArrayOutputStream();

        int exitCode = compiler.run(
                null,
                compilerOutput,
                compilerOutput,
                "-classpath",
                System.getProperty("java.class.path"),
                "-d",
                classesPath.toString(),
                sourcePath.toString()
        );

        assertEquals(0, exitCode, compilerOutput.toString(StandardCharsets.UTF_8));
    }

    private static String branchId(ClassAnalysisResult analysisResult, BranchType branchType) {
        return analysisResult.getCoverageGoals().stream()
                .map(CoverageGoal::getBranchId)
                .filter(branchId -> branchId.getBranchType() == branchType)
                .findFirst()
                .map(BranchId::asString)
                .orElseThrow();
    }
}
