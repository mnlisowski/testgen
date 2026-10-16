package com.mlisows.testgen.app.messaging;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mlisows.testgen.app.job.GenerationJobExecutor;
import com.mlisows.testgen.app.job.GenerationJobService;
import com.mlisows.testgen.app.job.SubmitGenerationRequest;
import com.mlisows.testgen.usecase.generation.GenerationMetrics;
import com.mlisows.testgen.usecase.generation.GenerationResult;
import com.mlisows.testgen.usecase.job.GenerationJob;
import com.mlisows.testgen.usecase.job.JobStatus;
import com.mlisows.testgen.usecase.ports.GenerationJobStore;
import com.mlisows.testgen.usecase.ports.GenerationRunner;
import com.mlisows.testgen.usecase.ports.JobRequestPublisher;
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
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

    // The generator itself is not under test; the mock finishes every job immediately.
    @MockitoBean
    private GenerationRunner generationRunner;

    @Autowired
    private GenerationJobService generationJobService;

    @Autowired
    private GenerationJobStore jobStore;

    @Autowired
    private JobRequestPublisher jobRequestPublisher;

    @Autowired
    private GenerationJobExecutor jobExecutor;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListeners;

    @Test
    void shouldPublishJobRequestAndExecuteJobFromKafka() throws Exception {
        when(generationRunner.run(any(), any())).thenReturn(emptyResult());

        GenerationJob job = submitJob();

        ConsumerRecord<String, String> record = readRecordWithKey(job.id().toString());
        JsonObject message = JsonParser.parseString(record.value()).getAsJsonObject();
        assertEquals(1, message.get("schemaVersion").getAsInt());
        assertEquals(job.id().toString(), message.get("jobId").getAsString());
        assertFalse(message.get("eventId").getAsString().isBlank());
        // Plain JSON: no Java class name is sent in message headers.
        assertEquals(0, record.headers().toArray().length);

        awaitStatus(job.id(), JobStatus.SUCCEEDED);
        verify(generationRunner).run(eq(job.id()), any());
    }

    @Test
    void shouldSkipDuplicateRequestForFinishedJob(CapturedOutput output) throws Exception {
        when(generationRunner.run(any(), any())).thenReturn(emptyResult());
        GenerationJob job = submitJob();
        awaitStatus(job.id(), JobStatus.SUCCEEDED);

        // The same job requested again, e.g. by a repeated publication after a timeout.
        jobRequestPublisher.publish(job.id());

        awaitOutput(output, "Skipped request for job " + job.id());
        verify(generationRunner, times(1)).run(eq(job.id()), any());
    }

    @Test
    void shouldStopExecutorBeforeKafkaListenerContainers() {
        // Lifecycle beans stop in descending phase order, so the executor interrupts a running
        // job first and the listener waiting for it can return before its container stops.
        assertTrue(jobExecutor.getPhase() > kafkaListeners.getPhase());
    }

    private GenerationJob submitJob() {
        return generationJobService.submit(
                new SubmitGenerationRequest("fit-bench", null, null, null, null, null, null));
    }

    /** Reads the topic with an independent consumer group, so it does not affect the application. */
    private ConsumerRecord<String, String> readRecordWithKey(String key) {
        Map<String, Object> consumerSettings = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-reader-" + UUID.randomUUID(),
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

    private void awaitStatus(UUID jobId, JobStatus expectedStatus) throws InterruptedException {
        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (jobStore.findById(jobId).orElseThrow().status() != expectedStatus
                && System.nanoTime() < deadlineNanos) {
            Thread.sleep(50);
        }
        assertEquals(expectedStatus, jobStore.findById(jobId).orElseThrow().status());
    }

    private void awaitOutput(CapturedOutput output, String expectedText) throws InterruptedException {
        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (!output.getOut().contains(expectedText) && System.nanoTime() < deadlineNanos) {
            Thread.sleep(100);
        }
        assertTrue(output.getOut().contains(expectedText), "Listener did not log: " + expectedText);
    }

    private GenerationResult emptyResult() {
        return new GenerationResult(List.of(), "report", new GenerationMetrics(0, 0, 0, 0, 0), Set.of());
    }
}
