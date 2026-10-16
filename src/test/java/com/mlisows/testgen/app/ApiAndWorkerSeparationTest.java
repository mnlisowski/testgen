package com.mlisows.testgen.app;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mlisows.testgen.app.job.GenerationJobService;
import com.mlisows.testgen.app.messaging.JobRequestListener;
import com.mlisows.testgen.usecase.ports.GenerationRunner;
import com.mlisows.testgen.usecase.ports.JobRequestPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Starts the API and the worker as two separate Spring applications, as in Docker Compose.
 * They share only PostgreSQL and Kafka; a job submitted to the API must be executed by the worker.
 */
@Testcontainers
class ApiAndWorkerSeparationTest {
    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Container
    static final KafkaContainer kafka = new KafkaContainer("apache/kafka:4.1.0");

    @TempDir
    static Path workDirectory;

    @Test
    void shouldExecuteJobSubmittedToApiInSeparateWorker() throws Exception {
        try (ConfigurableApplicationContext worker = startApplication("worker",
                     "--spring.main.web-application-type=none");
             ConfigurableApplicationContext api = startApplication("api", "--server.port=0")) {

            // Each process has only its own part of the application.
            assertTrue(api.getBeansOfType(JobRequestListener.class).isEmpty());
            assertTrue(api.getBeansOfType(GenerationRunner.class).isEmpty());
            assertTrue(worker.getBeansOfType(GenerationJobService.class).isEmpty());
            assertTrue(worker.getBeansOfType(JobRequestPublisher.class).isEmpty());
            assertFalse(worker.getBeansOfType(JobRequestListener.class).isEmpty());

            int apiPort = Integer.parseInt(api.getEnvironment().getProperty("local.server.port"));
            try (HttpClient client = HttpClient.newHttpClient()) {
                HttpResponse<String> accepted = submit(client, apiPort);
                assertEquals(202, accepted.statusCode(), accepted.body());

                String statusLocation = accepted.headers().firstValue("Location").orElseThrow();
                JsonObject finishedJob = awaitFinished(client, apiPort, statusLocation);

                assertEquals("SUCCEEDED", finishedJob.get("status").getAsString(), finishedJob.toString());
            }
        }
    }

    private ConfigurableApplicationContext startApplication(String profile, String... extraArguments) {
        List<String> arguments = new ArrayList<>(List.of(
                "--spring.datasource.url=" + postgres.getJdbcUrl(),
                "--spring.datasource.username=" + postgres.getUsername(),
                "--spring.datasource.password=" + postgres.getPassword(),
                "--spring.kafka.bootstrap-servers=" + kafka.getBootstrapServers(),
                "--testgen.projects.fit-bench.path=src/test/resources/sample-maven-project",
                "--testgen.work-root=" + workDirectory
        ));
        arguments.addAll(List.of(extraArguments));

        // An active profile replaces the default "api,worker", so this process has only one part.
        return new SpringApplicationBuilder(TestgenApplication.class)
                .profiles(profile)
                .run(arguments.toArray(String[]::new));
    }

    private HttpResponse<String> submit(HttpClient client, int apiPort) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(apiPort, "/api/generations"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"projectId\":\"fit-bench\",\"seed\":7}"))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private JsonObject awaitFinished(HttpClient client, int apiPort, String location) throws Exception {
        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(90);
        while (System.nanoTime() < deadlineNanos) {
            HttpRequest request = HttpRequest.newBuilder(uri(apiPort, location)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), response.body());

            JsonObject job = JsonParser.parseString(response.body()).getAsJsonObject();
            String status = job.get("status").getAsString();
            if (!status.equals("QUEUED") && !status.equals("RUNNING")) {
                return job;
            }
            Thread.sleep(200);
        }
        throw new AssertionError("Job did not finish");
    }

    private URI uri(int port, String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
