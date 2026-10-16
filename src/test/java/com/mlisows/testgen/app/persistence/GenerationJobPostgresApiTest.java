package com.mlisows.testgen.app.persistence;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Full flow with the postgres profile: HTTP submission, real generator process, result read from the database. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "testgen.projects.fit-bench.path=src/test/resources/sample-maven-project"
})
@ActiveProfiles("postgres")
@Testcontainers
class GenerationJobPostgresApiTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @TempDir
    static Path workDirectory;

    @DynamicPropertySource
    static void configureWorkDirectory(DynamicPropertyRegistry properties) {
        properties.add("testgen.work-root", () -> workDirectory.toString());
    }

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldStoreGenerationJobAndResultInDatabase() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> accepted = submit(client, "{\"projectId\":\"fit-bench\",\"seed\":42}");
            assertEquals(202, accepted.statusCode(), accepted.body());
            String statusLocation = accepted.headers().firstValue("Location").orElseThrow();

            JsonObject finishedJob = awaitFinished(client, statusLocation);
            assertEquals("SUCCEEDED", finishedJob.get("status").getAsString(), finishedJob.toString());

            UUID jobId = UUID.fromString(finishedJob.get("id").getAsString());
            assertEquals("SUCCEEDED", jdbcTemplate.queryForObject(
                    "SELECT status FROM generation_jobs WHERE id = ?", String.class, jobId));
            assertEquals(42L, jdbcTemplate.queryForObject(
                    "SELECT seed FROM generation_jobs WHERE id = ?", Long.class, jobId));
            assertTrue(jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM generated_test_files WHERE job_id = ?", Long.class, jobId) > 0);

            HttpResponse<byte[]> testsArchive = client.send(
                    HttpRequest.newBuilder(uri(statusLocation + "/tests")).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200, testsArchive.statusCode());
            assertTrue(testsArchive.body().length > 0);
        }
    }

    private HttpResponse<String> submit(HttpClient client, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri("/api/generations"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private JsonObject awaitFinished(HttpClient client, String location) throws Exception {
        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
        while (System.nanoTime() < deadlineNanos) {
            HttpResponse<String> response = client.send(
                    HttpRequest.newBuilder(uri(location)).GET().build(), HttpResponse.BodyHandlers.ofString());
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
