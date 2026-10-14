package com.mlisows.testgen.app.job;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mlisows.testgen.usecase.job.GenerationJob;
import com.mlisows.testgen.usecase.ports.GenerationJobStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "testgen.projects.fit-bench.path=src/test/resources/sample-maven-project"
})
class GenerationJobApiTest {
    @TempDir
    static Path workDirectory;

    @DynamicPropertySource
    static void configureWorkDirectory(DynamicPropertyRegistry properties) {
        properties.add("testgen.work-root", () -> workDirectory.toString());
    }

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private GenerationJobStore jobStore;

    @Test
    void shouldSubmitRealGenerationAndExposeStatusAndSummary() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            HttpResponse<String> accepted = submit(client, """
                    {"projectId":"fit-bench","seed":42,"initialCandidates":0}
                    """);
            assertEquals(202, accepted.statusCode(), accepted.body());
            JsonObject acceptedBody = JsonParser.parseString(accepted.body()).getAsJsonObject();
            assertEquals("QUEUED", acceptedBody.get("status").getAsString());
            JsonObject settings = acceptedBody.getAsJsonObject("settings");
            assertEquals(42, settings.get("seed").getAsInt());
            assertEquals(0, settings.get("initialCandidates").getAsInt());
            assertEquals(50, settings.get("maxCandidates").getAsInt());

            String statusLocation = accepted.headers().firstValue("Location").orElseThrow();
            assertEquals("/api/jobs/" + acceptedBody.get("id").getAsString(), statusLocation);
            JsonObject status = awaitFinished(client, statusLocation);

            assertEquals("SUCCEEDED", status.get("status").getAsString(), status.toString());
            assertTrue(status.getAsJsonObject("summary").get("selectedCandidates").getAsInt() > 0);
            assertFalse(status.get("startedAt").isJsonNull());
            assertFalse(status.get("finishedAt").isJsonNull());
            assertFalse(status.has("request"));
            assertFalse(status.toString().contains(workDirectory.toString()));

            UUID jobId = UUID.fromString(status.get("id").getAsString());
            GenerationJob completedJob = jobStore.findById(jobId).orElseThrow();
            assertFalse(completedJob.result().orElseThrow().tests().isEmpty());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}", "{", "null",
            "{\"projectId\":\"fit-bench\",\"profileId\":\"unsupported\"}",
            "{\"projectId\":\" \"}",
            "{\"projectId\":\"fit-bench\",\"maxCandidates\":0}",
            "{\"projectId\":\"fit-bench\",\"initialCandidates\":-1}",
            "{\"projectId\":\"fit-bench\",\"mutatedInitialCandidates\":-1}",
            "{\"projectId\":\"fit-bench\",\"maxValuesPerSlot\":0}",
            "{\"projectId\":\"fit-bench\",\"seed\":\"invalid\"}"
    })
    void shouldReturnBadRequestForInvalidInput(String input) throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = submit(client, input);
            assertEquals(400, response.statusCode(), response.body());
            assertTrue(response.headers().firstValue("Content-Type").orElse("")
                    .startsWith("application/problem+json"));
        }
    }

    @Test
    void shouldReturnNotFoundForUnknownProjectAndJob() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            assertEquals(404, submit(client, "{\"projectId\":\"unknown\"}").statusCode());
            assertEquals(404, get(client, "/api/jobs/" + UUID.randomUUID()).statusCode());
            assertEquals(400, get(client, "/api/jobs/not-a-uuid").statusCode());
        }
    }

    private HttpResponse<String> submit(HttpClient client, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri("/api/generations"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .timeout(Duration.ofSeconds(10)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private JsonObject awaitFinished(HttpClient client, String location) throws Exception {
        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
        while (System.nanoTime() < deadlineNanos) {
            HttpResponse<String> response = get(client, location);
            assertEquals(200, response.statusCode(), response.body());
            JsonObject job = JsonParser.parseString(response.body()).getAsJsonObject();
            String status = job.get("status").getAsString();
            if (!status.equals("QUEUED") && !status.equals("RUNNING")) {
                return job;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Job did not finish");
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
