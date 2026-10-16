package com.mlisows.testgen.app.profile;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mlisows.testgen.infrastructure.process.GenerationProcessProtocol;
import com.mlisows.testgen.usecase.job.GenerationJob;
import com.mlisows.testgen.usecase.ports.GenerationJobStore;
import com.mlisows.testgen.usecase.ports.ObservedProfileStore;
import com.mlisows.testgen.usecase.profile.StoredObservedProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "testgen.projects.fit-bench.path=src/test/resources/sample-maven-project",
        "testgen.projects.other.path=src/test/resources/sample-maven-project"
})
@Testcontainers
class ObservedProfileApiTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Container
    @ServiceConnection
    static final KafkaContainer kafka = new KafkaContainer("apache/kafka:4.1.0");

    @TempDir
    static Path workDirectory;

    @DynamicPropertySource
    static void configureWorkDirectory(DynamicPropertyRegistry properties) {
        properties.add("testgen.work-root", () -> workDirectory.toString());
    }

    @Autowired
    private GenerationJobStore jobStore;

    private static final String VALID_PROFILE = """
            # runtime profile
            hint|sample.Order.label(String)|label|String|"Zażółć"|runtime-observed
            invocation|sample.Order.label(String)|String|"Zażółć"
            """;

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ObservedProfileStore profileStore;

    @Test
    void shouldImportUtf8ProfileAndAssociateItWithProject() throws Exception {
        Instant beforeUpload = Instant.now();
        HttpResponse<String> response = upload("fit-bench", "file", VALID_PROFILE.getBytes(StandardCharsets.UTF_8));

        assertEquals(201, response.statusCode(), response.body());
        JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
        UUID profileId = UUID.fromString(body.get("profileId").getAsString());
        assertEquals("fit-bench", body.get("projectId").getAsString());
        assertEquals(1, body.get("hintCount").getAsInt());
        assertEquals(1, body.get("invocationCount").getAsInt());
        assertFalse(body.has("profile"));
        StoredObservedProfile storedProfile = profileStore.findById(profileId).orElseThrow();
        assertEquals("fit-bench", storedProfile.projectId());
        assertFalse(storedProfile.createdAt().isBefore(beforeUpload));
        assertEquals(storedProfile.createdAt(), Instant.parse(body.get("createdAt").getAsString()));
        assertEquals("\"Zażółć\"", storedProfile.profile().getArgumentValueHints().getFirst().getValue());
        assertEquals("\"Zażółć\"", storedProfile.profile().getObservedInvocations()
                .getFirst().getArguments().getFirst().getValue());
    }

    @Test
    void shouldAssignDifferentIdsToSeparateUploads() throws Exception {
        HttpResponse<String> first = upload("fit-bench", "file", VALID_PROFILE.getBytes(StandardCharsets.UTF_8));
        HttpResponse<String> second = upload("fit-bench", "file", VALID_PROFILE.getBytes(StandardCharsets.UTF_8));

        assertEquals(201, first.statusCode());
        assertEquals(201, second.statusCode());
        assertNotEquals(JsonParser.parseString(first.body()).getAsJsonObject().get("profileId"),
                JsonParser.parseString(second.body()).getAsJsonObject().get("profileId"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " \n# comment only\n", "unknown|record", "hint|too|short",
            "invocation|sample.Order.label(String)|String"})
    void shouldRejectEmptyOrMalformedProfile(String text) throws Exception {
        HttpResponse<String> response = upload("fit-bench", "file", text.getBytes(StandardCharsets.UTF_8));

        assertProblem(response, 400);
    }

    @Test
    void shouldReportLineNumberOfMalformedRecord() throws Exception {
        HttpResponse<String> response = upload("fit-bench", "file",
                (VALID_PROFILE + "unknown|record\n").getBytes(StandardCharsets.UTF_8));

        assertProblem(response, 400);
        assertTrue(JsonParser.parseString(response.body()).getAsJsonObject()
                .get("detail").getAsString().contains("line 4"));
    }

    @Test
    void shouldRejectInvalidUtf8() throws Exception {
        assertProblem(upload("fit-bench", "file", new byte[]{(byte) 0xC3, (byte) 0x28}), 400);
    }

    @Test
    void shouldRejectMissingFilePart() throws Exception {
        assertProblem(upload("fit-bench", "wrong-part", VALID_PROFILE.getBytes(StandardCharsets.UTF_8)), 400);
    }

    @Test
    void shouldRejectUnknownProject() throws Exception {
        assertProblem(upload("unknown", "file", VALID_PROFILE.getBytes(StandardCharsets.UTF_8)), 404);
    }

    @Test
    void shouldRejectOversizedFile() throws Exception {
        assertProblem(upload("fit-bench", "file", new byte[1024 * 1024 + 1]), 413);
    }

    @Test
    void shouldGenerateWithUploadedProfileAndExposeItsId() throws Exception {
        String profileText = "hint|sample.maven.DiscountService.shippingFee(int)|amount|int|99|runtime-observed\n";
        HttpResponse<String> upload = upload("fit-bench", "file", profileText.getBytes(StandardCharsets.UTF_8));
        assertEquals(201, upload.statusCode(), upload.body());
        String profileId = JsonParser.parseString(upload.body()).getAsJsonObject().get("profileId").getAsString();

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> accepted = submitGeneration(client, "fit-bench", profileId);
            assertEquals(202, accepted.statusCode(), accepted.body());
            JsonObject acceptedJob = JsonParser.parseString(accepted.body()).getAsJsonObject();
            assertEquals(profileId, acceptedJob.get("profileId").getAsString());
            UUID jobId = UUID.fromString(acceptedJob.get("id").getAsString());
            JsonObject completed = awaitFinished(client, accepted.headers().firstValue("Location").orElseThrow());
            assertEquals("SUCCEEDED", completed.get("status").getAsString(), completed.toString());
            assertEquals(profileId, completed.get("profileId").getAsString());

            GenerationJob storedJob = jobStore.findById(jobId).orElseThrow();
            assertEquals(UUID.fromString(profileId), storedJob.profileId().orElseThrow());
            assertEquals("99", storedJob.request().observedProfile().getArgumentValueHints().getFirst().getValue());
            assertFalse(storedJob.result().orElseThrow().tests().isEmpty());
            Path processRequest = workDirectory.resolve(jobId.toString()).resolve("request.json");
            assertEquals("99", new GenerationProcessProtocol().readRequest(processRequest)
                    .observedProfile().getArgumentValueHints().getFirst().getValue());
        }
    }

    @Test
    void shouldRejectProfileFromAnotherProject() throws Exception {
        HttpResponse<String> upload = upload("other", "file", VALID_PROFILE.getBytes(StandardCharsets.UTF_8));
        assertEquals(201, upload.statusCode());
        String profileId = JsonParser.parseString(upload.body()).getAsJsonObject().get("profileId").getAsString();

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = submitGeneration(client, "fit-bench", profileId);
            assertProblem(response, 400);
            assertTrue(JsonParser.parseString(response.body()).getAsJsonObject()
                    .get("detail").getAsString().contains("does not belong to project"));
            assertFalse(response.headers().firstValue("Location").isPresent());
        }
    }

    @Test
    void shouldRejectUnknownProfile() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            assertProblem(submitGeneration(client, "fit-bench", UUID.randomUUID().toString()), 404);
        }
    }

    private HttpResponse<String> submitGeneration(HttpClient client, String projectId, String profileId) throws Exception {
        String body = """
                {"projectId":"%s","profileId":"%s","seed":42,"maxCandidates":50}
                """.formatted(projectId, profileId);
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/generations"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private JsonObject awaitFinished(HttpClient client, String statusLocation) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
        while (System.nanoTime() < deadline) {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + statusLocation))
                    .timeout(Duration.ofSeconds(10)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), response.body());
            JsonObject job = JsonParser.parseString(response.body()).getAsJsonObject();
            String status = job.get("status").getAsString();
            if (!status.equals("QUEUED") && !status.equals("RUNNING")) {
                return job;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Generation did not finish");
    }

    private void assertProblem(HttpResponse<String> response, int status) {
        assertEquals(status, response.statusCode(), response.body());
        assertTrue(response.headers().firstValue("Content-Type").orElse("")
                .startsWith("application/problem+json"));
        assertEquals(status, JsonParser.parseString(response.body()).getAsJsonObject().get("status").getAsInt());
    }

    private HttpResponse<String> upload(String projectId, String partName, byte[] content) throws Exception {
        String boundary = "profile-upload-boundary";
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + partName + "\"; filename=\"profile.txt\"\r\n"
                + "Content-Type: text/plain\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(content);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/projects/" + projectId + "/profiles"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        }
    }
}
