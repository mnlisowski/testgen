package com.mlisows.testgen.app;

import com.mlisows.testgen.app.project.ProjectRegistry;
import com.mlisows.testgen.domain.generation.GenerationSettings;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TestgenApplicationTest {
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
    void shouldStartHttpServerWithoutBusinessEndpoints() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/")).GET().build();
            var response = client.send(request, HttpResponse.BodyHandlers.discarding());

            assertEquals(404, response.statusCode());
        }
    }
}
