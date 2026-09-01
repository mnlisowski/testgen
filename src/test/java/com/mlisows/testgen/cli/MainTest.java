package com.mlisows.testgen.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldPrintGenerationReportForMavenProject() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));

            Main.main(new String[]{
                    "src/test/resources/sample-maven-project",
                    tempDir.resolve("work").toString()
            });
        } finally {
            System.setOut(originalOut);
        }

        String report = output.toString(StandardCharsets.UTF_8);

        assertTrue(report.contains("Test generation report"));
        assertTrue(report.contains("Classes analyzed: 4"));
        assertTrue(report.contains("Supported branch kinds: IF, FOR, WHILE, SWITCH"));
        assertTrue(report.contains("Methods analyzed: 8"));
        assertTrue(report.contains("Detected goals: 15"));
        assertTrue(report.contains("Covered goals: 12"));
        assertTrue(report.contains("Failed to execute: 0"));
        assertTrue(report.contains("Unsupported Java constructs"));
    }
}
