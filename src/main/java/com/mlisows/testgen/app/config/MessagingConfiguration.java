package com.mlisows.testgen.app.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka topics, used by both the API and the worker. Spring Boot's {@code KafkaAdmin} creates every
 * {@link NewTopic} bean at startup if the topic does not exist yet; an existing topic is left
 * unchanged, so it does not matter which process starts first.
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
}
