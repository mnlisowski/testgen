package com.mlisows.testgen.infrastructure.execution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaSourceCompilerTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCompileJavaSourcesToClassesDirectory() throws Exception {
        Path sourceRoot = tempDir.resolve("src");
        Path classesRoot = tempDir.resolve("classes");
        Path sourcePath = sourceRoot.resolve("sample/Calculator.java");

        Files.createDirectories(sourcePath.getParent());
        Files.writeString(sourcePath, """
                  package sample;

                  public class Calculator {
                      public int calculate(int amount) {
                          com.mlisows.testgen.infrastructure.runtime.BranchRecorder.hit("sample.Calculator|calculate|5|IF|TRUE");
                          return amount;
                      }
                  }
                  """);

        Path result = new JavaSourceCompiler().compile(sourceRoot, classesRoot);

        assertEquals(classesRoot, result);
        assertTrue(Files.exists(classesRoot.resolve("sample/Calculator.class")));
    }

    @Test
    void shouldFailWhenSourceDoesNotCompile() throws Exception {
        Path sourceRoot = tempDir.resolve("src");
        Path classesRoot = tempDir.resolve("classes");
        Path sourcePath = sourceRoot.resolve("sample/Broken.java");

        Files.createDirectories(sourcePath.getParent());
        Files.writeString(sourcePath, """
                  package sample;

                  public class Broken {
                  """);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new JavaSourceCompiler().compile(sourceRoot, classesRoot)
        );

        assertTrue(exception.getMessage().contains("Cannot compile source directory"));
    }
}
