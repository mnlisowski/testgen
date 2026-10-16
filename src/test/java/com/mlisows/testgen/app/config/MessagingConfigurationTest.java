package com.mlisows.testgen.app.config;

import org.apache.kafka.clients.admin.TopicDescription;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaAdmin;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class MessagingConfigurationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Container
    @ServiceConnection
    static final KafkaContainer kafka = new KafkaContainer("apache/kafka:4.1.0");

    @Autowired
    private KafkaAdmin kafkaAdmin;

    @Test
    void shouldCreateJobRequestsTopicAtStartup() {
        // A topic created with Kafka defaults would have one partition; three partitions show
        // that the topic was created from our NewTopic bean.
        Map<String, TopicDescription> topics = kafkaAdmin.describeTopics("testgen.jobs.requests");

        TopicDescription jobRequestsTopic = topics.get("testgen.jobs.requests");
        assertEquals(3, jobRequestsTopic.partitions().size());
    }
}
