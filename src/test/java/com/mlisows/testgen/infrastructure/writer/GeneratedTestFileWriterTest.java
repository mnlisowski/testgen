package com.mlisows.testgen.infrastructure.writer;

import com.mlisows.testgen.domain.GeneratedTestSuite;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratedTestFileWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldWriteGeneratedTestCodeToFile() throws Exception {
        GeneratedTestSuite testSuite = new GeneratedTestSuite(
                "sample.Calculator",
                List.of()
        );

        GeneratedTestFileWriter writer = new GeneratedTestFileWriter(tempDir);
        String code = "class CalculatorTest {\n}\n";

        Path writtenPath = writer.write(testSuite, code);

        assertEquals(tempDir.resolve("sample/CalculatorTest.java"), writtenPath);
        assertTrue(Files.exists(writtenPath));
        assertEquals(code, Files.readString(writtenPath));
    }
}
