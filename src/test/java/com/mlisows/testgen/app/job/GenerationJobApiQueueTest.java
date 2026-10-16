package com.mlisows.testgen.app.job;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mlisows.testgen.usecase.CandidateCoverageEvaluation;
import com.mlisows.testgen.usecase.GenerationReport;
import com.mlisows.testgen.usecase.generation.GenerationResult;
import com.mlisows.testgen.usecase.ports.GenerationRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "testgen.queue-capacity=1")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Testcontainers
class GenerationJobApiQueueTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Value("${local.server.port}")
    private int port;

    @MockitoBean
    private GenerationRunner runner;

    @Test
    void shouldReturn503WhenQueueIsFullAndRetainRejectedJobAsFailed() throws Exception {
        var started = new CountDownLatch(1);
        var allowCompletion = new CountDownLatch(1);
        when(runner.run(any(), any())).thenAnswer(invocation -> {
            started.countDown();
            if (!allowCompletion.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("Test did not release the runner");
            }
            return GenerationResult.from(List.of(),
                    GenerationReport.from(List.of(), List.of(), CandidateCoverageEvaluation.empty()));
        });

        try (var client = HttpClient.newHttpClient()) {
            try {
                HttpResponse<String> first = submit(client);
                assertEquals(202, first.statusCode());
                assertTrue(started.await(5, TimeUnit.SECONDS));
                HttpResponse<String> waiting = submit(client);
                assertEquals(202, waiting.statusCode());

                HttpResponse<String> rejected = submit(client);
                assertEquals(503, rejected.statusCode(), rejected.body());
                JsonObject problem = JsonParser.parseString(rejected.body()).getAsJsonObject();
                assertEquals(503, problem.get("status").getAsInt());
                JsonObject rejectedJob = getJob(client, "/api/jobs/" + problem.get("jobId").getAsString());
                assertEquals("FAILED", rejectedJob.get("status").getAsString());
                JsonObject queuedJob = getJob(client, waiting.headers().firstValue("Location").orElseThrow());
                assertEquals("QUEUED", queuedJob.get("status").getAsString());

                allowCompletion.countDown();
                awaitFailedOrSucceeded(client, first.headers().firstValue("Location").orElseThrow(), "SUCCEEDED");
                awaitFailedOrSucceeded(client, waiting.headers().firstValue("Location").orElseThrow(), "SUCCEEDED");
            } finally {
                allowCompletion.countDown();
            }
        }
    }

    @Test
    void shouldExposeFailureWithoutLeakingInternalPaths() throws Exception {
        when(runner.run(any(), any())).thenThrow(new IOException("Private path: /srv/private/work/process.log"));
        try (var client = HttpClient.newHttpClient()) {
            HttpResponse<String> accepted = submit(client);
            assertEquals(202, accepted.statusCode());
            JsonObject failedJob = awaitFailedOrSucceeded(
                    client, accepted.headers().firstValue("Location").orElseThrow(), "FAILED");
            assertFalse(failedJob.get("error").isJsonNull());
            assertFalse(failedJob.toString().contains("/srv/private"));
        }
    }

    private HttpResponse<String> submit(HttpClient client) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri("/api/generations"))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"projectId\":\"fit-bench\"}")).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private JsonObject getJob(HttpClient client, String location) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(location)).timeout(Duration.ofSeconds(5)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private JsonObject awaitFailedOrSucceeded(HttpClient client, String location, String expected) throws Exception {
        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadlineNanos) {
            JsonObject job = getJob(client, location);
            String status = job.get("status").getAsString();
            if (!status.equals("QUEUED") && !status.equals("RUNNING")) {
                assertEquals(expected, status, job.toString());
                return job;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("Job did not finish");
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
