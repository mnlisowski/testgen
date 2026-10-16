package com.mlisows.testgen.app.messaging;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mlisows.testgen.app.job.GenerationJobService;
import com.mlisows.testgen.app.job.SubmitGenerationRequest;
import com.mlisows.testgen.usecase.generation.GenerationMetrics;
import com.mlisows.testgen.usecase.generation.GenerationResult;
import com.mlisows.testgen.usecase.job.GenerationJob;
import com.mlisows.testgen.usecase.ports.GenerationRunner;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class JobRequestMessagingTest {
    private static final String TOPIC = "testgen.jobs.requests";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Container
    @ServiceConnection
    static final KafkaContainer kafka = new KafkaContainer("apache/kafka:4.1.0");

    // Jobs are still executed by the in-memory queue; the mock finishes them immediately.
    @MockitoBean
    private GenerationRunner generationRunner;

    @Autowired
    private GenerationJobService generationJobService;

    @Test
    void shouldPublishJobRequestAndReceiveItInListener(CapturedOutput output) throws Exception {
        when(generationRunner.run(any(), any())).thenReturn(
                new GenerationResult(List.of(), "report", new GenerationMetrics(0, 0, 0, 0, 0), Set.of()));

        GenerationJob job = generationJobService.submit(
                new SubmitGenerationRequest("fit-bench", null, null, null, null, null, null));

        ConsumerRecord<String, String> record = readRecordWithKey(job.id().toString());
        JsonObject message = JsonParser.parseString(record.value()).getAsJsonObject();
        assertEquals(1, message.get("schemaVersion").getAsInt());
        assertEquals(job.id().toString(), message.get("jobId").getAsString());
        assertFalse(message.get("eventId").getAsString().isBlank());
        // Plain JSON: no Java class name is sent in message headers.
        assertEquals(0, record.headers().toArray().length);

        awaitOutput(output, "Received request for job " + job.id());
    }

    /** Reads the topic with an independent consumer group, so it does not affect the application. */
    private ConsumerRecord<String, String> readRecordWithKey(String key) {
        Map<String, Object> consumerSettings = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-reader",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        );

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerSettings)) {
            consumer.subscribe(List.of(TOPIC));
            long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
            while (System.nanoTime() < deadlineNanos) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    if (record.key().equals(key)) {
                        return record;
                    }
                }
            }
        }
        throw new AssertionError("No message with key " + key + " on topic " + TOPIC);
    }

    private void awaitOutput(CapturedOutput output, String expectedText) throws InterruptedException {
        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (!output.getOut().contains(expectedText) && System.nanoTime() < deadlineNanos) {
            Thread.sleep(100);
        }
        assertTrue(output.getOut().contains(expectedText), "Listener did not log: " + expectedText);
    }
}
