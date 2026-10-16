package com.mlisows.testgen.app.config;

import com.mlisows.testgen.app.job.GenerationJobExecutor;
import com.mlisows.testgen.app.messaging.JobRequestListener;
import com.mlisows.testgen.infrastructure.messaging.JobRequestMessage;
import com.mlisows.testgen.infrastructure.messaging.KafkaJobRequestPublisher;
import com.mlisows.testgen.usecase.ports.JobRequestPublisher;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;

/**
 * Kafka topics used by the server. Spring Boot's {@code KafkaAdmin} creates every {@link NewTopic}
 * bean at startup if the topic does not exist yet; an existing topic is left unchanged.
 */
@Configuration(proxyBeanMethods = false)
public class MessagingConfiguration {
    // A single local broker can keep only one copy of each partition.
    private static final int LOCAL_REPLICATION_FACTOR = 1;

    @Bean
    public NewTopic jobRequestsTopic(TestgenProperties properties) {
        TestgenProperties.Messaging messaging = properties.messaging();

        return TopicBuilder.name(messaging.jobRequestsTopic())
                .partitions(messaging.jobRequestsPartitions())
                .replicas(LOCAL_REPLICATION_FACTOR)
                .build();
    }

    // KafkaTemplate is created by Spring Boot from the spring.kafka.producer settings.
    @Bean
    public JobRequestPublisher jobRequestPublisher(
            KafkaTemplate<String, JobRequestMessage> kafkaTemplate,
            TestgenProperties properties
    ) {
        TestgenProperties.Messaging messaging = properties.messaging();

        return new KafkaJobRequestPublisher(kafkaTemplate, messaging.jobRequestsTopic(),
                Duration.ofSeconds(messaging.publishTimeoutSeconds()));
    }

    @Bean
    public JobRequestListener jobRequestListener(GenerationJobExecutor jobExecutor) {
        return new JobRequestListener(jobExecutor);
    }
}
