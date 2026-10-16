package com.mlisows.testgen.infrastructure.messaging;

import com.mlisows.testgen.usecase.job.JobRequestPublicationException;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaJobRequestPublisherTest {
    private static final String TOPIC = "testgen.jobs.requests";

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, JobRequestMessage> kafkaTemplate = mock(KafkaTemplate.class);

    private final KafkaJobRequestPublisher publisher =
            new KafkaJobRequestPublisher(kafkaTemplate, TOPIC, Duration.ofMillis(200));

    @Test
    void shouldSendRequestWithJobIdAsKey() {
        UUID jobId = UUID.randomUUID();
        when(kafkaTemplate.send(eq(TOPIC), eq(jobId.toString()), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publish(jobId);

        verify(kafkaTemplate).send(eq(TOPIC), eq(jobId.toString()), any(JobRequestMessage.class));
    }

    @Test
    void shouldReportFailureThrownBySendWhenBrokerIsUnreachable() {
        UUID jobId = UUID.randomUUID();
        // The same exception KafkaTemplate throws when the producer cannot reach the broker.
        when(kafkaTemplate.send(any(), any(), any())).thenThrow(new KafkaException("Send failed",
                new TimeoutException("Topic testgen.jobs.requests not present in metadata after 5000 ms.")));

        JobRequestPublicationException exception =
                assertThrows(JobRequestPublicationException.class, () -> publisher.publish(jobId));

        assertEquals(jobId, exception.jobId());
    }

    @Test
    void shouldReportFailureReturnedInFuture() {
        UUID jobId = UUID.randomUUID();
        CompletableFuture<SendResult<String, JobRequestMessage>> failedSend =
                CompletableFuture.failedFuture(new TimeoutException("Expiring record"));
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(failedSend);

        assertThrows(JobRequestPublicationException.class, () -> publisher.publish(jobId));
    }

    @Test
    void shouldReportMissingConfirmationWithinTimeout() {
        UUID jobId = UUID.randomUUID();
        // A future that never completes: the broker did not confirm the message in time.
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(new CompletableFuture<>());

        assertThrows(JobRequestPublicationException.class, () -> publisher.publish(jobId));
    }
}
