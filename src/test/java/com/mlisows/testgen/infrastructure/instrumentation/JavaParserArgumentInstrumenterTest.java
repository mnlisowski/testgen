package com.mlisows.testgen.infrastructure.instrumentation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaParserArgumentInstrumenterTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldInstrumentMethodsAndConstructors() throws Exception {
        Path sourcePath = tempDir.resolve("OrderService.java");
        Path outputPath = tempDir.resolve("instrumented/OrderService.java");

        Files.writeString(sourcePath, """
                package sample;

                public class OrderService {
                    private final int threshold;

                    public OrderService(int threshold) {
                        this.threshold = threshold;
                    }

                    public int calculate(int amount, String status) {
                        return amount + threshold;
                    }
                }
                """);

        new JavaParserArgumentInstrumenter().instrument(sourcePath, outputPath);

        String instrumentedSource = withoutWhitespace(Files.readString(outputPath));

        assertTrue(instrumentedSource.contains(withoutWhitespace(
                "ArgumentRecorder.record(\"sample.OrderService.<init>\", new String[]{\"threshold\"}, new String[]{\"int\"}, new Object[]{threshold});"
        )));

        assertTrue(instrumentedSource.contains(withoutWhitespace(
                "ArgumentRecorder.record(\"sample.OrderService.calculate\", new String[]{\"amount\", \"status\"}, new String[]{\"int\", \"String\"}, new Object[]{amount, status});"
        )));


    }

    @Test
    void shouldInsertConstructorRecorderAfterExplicitConstructorCall() throws Exception {
        Path sourcePath = tempDir.resolve("OrderService.java");
        Path outputPath = tempDir.resolve("instrumented/OrderService.java");

        Files.writeString(sourcePath, """
                package sample;

                public class OrderService {
                    public OrderService() {
                        this(100);
                    }

                    public OrderService(int threshold) {
                    }
                }
                """);

        new JavaParserArgumentInstrumenter().instrument(sourcePath, outputPath);

        String instrumentedSource = withoutWhitespace(Files.readString(outputPath));

        assertTrue(instrumentedSource.indexOf(withoutWhitespace("this(100);"))
                < instrumentedSource.indexOf(withoutWhitespace(
                "ArgumentRecorder.record(\"sample.OrderService.<init>\", new String[]{}, new String[]{}, new Object[]{});"
        )));

    }

    private String withoutWhitespace(String value) {
        return value.replaceAll("\\s+", "");
    }
}
