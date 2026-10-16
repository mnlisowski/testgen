package com.mlisows.testgen.infrastructure.messaging;

import com.mlisows.testgen.usecase.job.JobRequestPublicationException;
import com.mlisows.testgen.usecase.ports.JobRequestPublisher;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class KafkaJobRequestPublisher implements JobRequestPublisher {
    private final KafkaTemplate<String, JobRequestMessage> kafkaTemplate;
    private final String topic;
    private final Duration confirmationTimeout;

    public KafkaJobRequestPublisher(
            KafkaTemplate<String, JobRequestMessage> kafkaTemplate,
            String topic,
            Duration confirmationTimeout
    ) {
        this.kafkaTemplate = Objects.requireNonNull(kafkaTemplate, "kafkaTemplate");
        this.topic = Objects.requireNonNull(topic, "topic");
        this.confirmationTimeout = Objects.requireNonNull(confirmationTimeout, "confirmationTimeout");
    }

    @Override
    public void publish(UUID jobId) {
        Objects.requireNonNull(jobId, "jobId");
        JobRequestMessage message = JobRequestMessage.forJob(jobId);

        // The job id is the message key: Kafka sends all messages with the same key
        // to the same partition, so repeated requests for one job keep their order.
        String key = jobId.toString();

        try {
            // send() is asynchronous; get() waits until the broker confirms that it stored the message.
            kafkaTemplate.send(topic, key, message)
                    .get(confirmationTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (KafkaException exception) {
            // Thrown by send() itself, before any Future exists, e.g. when the broker is unreachable
            // and the producer cannot read topic metadata within max.block.ms. KafkaTemplate wraps
            // the client's error in Spring's KafkaException ("Send failed").
            throw new JobRequestPublicationException(jobId, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new JobRequestPublicationException(jobId, exception);
        } catch (ExecutionException | TimeoutException exception) {
            // After a timeout the broker may still store the message later. A duplicate request
            // is harmless, because a worker starts a job only if it is still QUEUED.
            throw new JobRequestPublicationException(jobId, exception);
        }
    }
}
