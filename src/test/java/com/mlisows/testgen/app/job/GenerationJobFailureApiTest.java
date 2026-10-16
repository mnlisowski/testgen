package com.mlisows.testgen.app.job;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mlisows.testgen.usecase.ports.GenerationRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class GenerationJobFailureApiTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Container
    @ServiceConnection
    static final KafkaContainer kafka = new KafkaContainer("apache/kafka:4.1.0");

    @Value("${local.server.port}")
    private int port;

    @MockitoBean
    private GenerationRunner runner;

    @Test
    void shouldExposeFailureWithoutLeakingInternalPaths() throws Exception {
        when(runner.run(any(), any())).thenThrow(new IOException("Private path: /srv/private/work/process.log"));

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> accepted = submit(client);
            assertEquals(202, accepted.statusCode());

            JsonObject failedJob = awaitFinished(client, accepted.headers().firstValue("Location").orElseThrow());

            assertEquals("FAILED", failedJob.get("status").getAsString(), failedJob.toString());
            assertFalse(failedJob.get("error").isJsonNull());
            assertFalse(failedJob.toString().contains("/srv/private"));
        }
    }

    private HttpResponse<String> submit(HttpClient client) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri("/api/generations"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"projectId\":\"fit-bench\"}"))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private JsonObject awaitFinished(HttpClient client, String location) throws Exception {
        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (System.nanoTime() < deadlineNanos) {
            HttpRequest request = HttpRequest.newBuilder(uri(location)).timeout(Duration.ofSeconds(5)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), response.body());

            JsonObject job = JsonParser.parseString(response.body()).getAsJsonObject();
            String status = job.get("status").getAsString();
            if (!status.equals("QUEUED") && !status.equals("RUNNING")) {
                return job;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Job did not finish");
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
