package com.mlisows.testgen.infrastructure.process;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mlisows.testgen.usecase.generation.GeneratedTestFile;
import com.mlisows.testgen.usecase.generation.GenerationMetrics;
import com.mlisows.testgen.usecase.generation.GenerationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GenerationProcessProtocolTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void shouldTransferOnlyStoredResultData() throws Exception {
        GenerationResult result = new GenerationResult(
                List.of(new GeneratedTestFile(Path.of("sample/ExampleTest.java"), "class ExampleTest {}")),
                "Raport z generowania\n",
                new GenerationMetrics(1, 50, 1, 2, 1),
                Set.of("sample.Example#branch:1")
        );
        GenerationProcessProtocol protocol = new GenerationProcessProtocol();
        Path resultPath = temporaryDirectory.resolve("result.json");

        protocol.writeResult(resultPath, result);
        GenerationResult restoredResult = protocol.readResult(resultPath);

        assertEquals(result, restoredResult);
        JsonObject json = JsonParser.parseString(Files.readString(resultPath)).getAsJsonObject();
        assertEquals(Set.of("version", "tests", "reportText", "metrics", "coveredGoalIds"), json.keySet());
    }

    @Test
    void shouldRejectResultWithMissingReportText() throws Exception {
        Path resultPath = temporaryDirectory.resolve("result.json");
        Files.writeString(resultPath, """
                {"version":2,"tests":[],"metrics":{"analyzedClasses":0,"executedCandidates":0,
                "selectedCandidates":0,"coverageGoals":0,"coveredGoals":0},"coveredGoalIds":[]}
                """);

        assertThrows(RuntimeException.class, () -> new GenerationProcessProtocol().readResult(resultPath));
    }

    @Test
    void shouldRejectOldProtocolVersion() throws Exception {
        Path resultPath = temporaryDirectory.resolve("result.json");
        Files.writeString(resultPath, "{\"version\":1}");

        assertThrows(IllegalArgumentException.class,
                () -> new GenerationProcessProtocol().readResult(resultPath));
    }
}
