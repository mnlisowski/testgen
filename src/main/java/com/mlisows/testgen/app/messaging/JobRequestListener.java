package com.mlisows.testgen.app.messaging;

import com.mlisows.testgen.infrastructure.messaging.JobRequestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

/**
 * Receives job requests from Kafka. At this stage it only logs them; jobs are still executed
 * by the in-memory queue.
 */
public class JobRequestListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobRequestListener.class);

    // The topic name comes from configuration; ${...} is resolved by Spring when the listener starts.
    // The consumer group is set by spring.kafka.consumer.group-id.
    @KafkaListener(topics = "${testgen.messaging.job-requests-topic}")
    public void onJobRequest(JobRequestMessage message) {
        LOGGER.info("Received request for job {} (event {}, schema version {})",
                message.jobId(), message.eventId(), message.schemaVersion());
    }
}
