package com.mlisows.testgen.app;

import com.google.gson.JsonParser;
import com.mlisows.testgen.app.project.ProjectRegistry;
import com.mlisows.testgen.domain.generation.GenerationSettings;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TestgenApplicationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    private ProjectRegistry projects;

    @Autowired
    private GenerationSettings defaults;

    @Test
    void shouldLoadBundledYamlConfiguration() {
        assertEquals(java.util.List.of("fit-bench"), projects.projectIds());
        assertEquals(GenerationSettings.defaults(), defaults);
    }

    @Value("${local.server.port}")
    private int port;

    @Test
    void shouldReturnNotFoundForUnmappedRoot() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/")).GET().build();
            var response = client.send(request, HttpResponse.BodyHandlers.discarding());

            assertEquals(404, response.statusCode());
        }
    }

    @Test
    void shouldExposeConfiguredProjectsAsJsonWithoutLocalPaths() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder(
                    URI.create("http://localhost:" + port + "/api/projects"))
                    .header("Accept", "application/json")
                    .GET().build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertTrue(response.headers().firstValue("Content-Type").orElse("")
                    .startsWith("application/json"));
            assertEquals(
                    JsonParser.parseString("[{\"id\":\"fit-bench\"}]"),
                    JsonParser.parseString(response.body()));
        }
    }

}
