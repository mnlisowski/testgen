package com.mlisows.testgen.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TestgenApplicationTest {
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
