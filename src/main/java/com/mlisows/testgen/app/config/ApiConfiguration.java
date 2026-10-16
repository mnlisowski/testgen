package com.mlisows.testgen.app.config;

import com.mlisows.testgen.app.project.ConfiguredProjectsInitializer;
import com.mlisows.testgen.app.project.ProjectRegistry;
import com.mlisows.testgen.infrastructure.messaging.JobRequestMessage;
import com.mlisows.testgen.infrastructure.messaging.KafkaJobRequestPublisher;
import com.mlisows.testgen.infrastructure.persistence.project.ProjectRepository;
import com.mlisows.testgen.usecase.ports.JobRequestPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;

/**
 * Beans of the API: it accepts requests over HTTP, stores jobs and publishes job requests.
 * Active with the {@code api} Spring profile. Controllers and services of the API are marked
 * with the same profile on their classes.
 */
@Configuration(proxyBeanMethods = false)
@Profile("api")
public class ApiConfiguration {
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

    // Jobs reference projects by foreign key; they are created only through the API.
    @Bean
    public ConfiguredProjectsInitializer configuredProjectsInitializer(
            ProjectRegistry projectRegistry,
            ProjectRepository projectRepository
    ) {
        return new ConfiguredProjectsInitializer(projectRegistry, projectRepository);
    }
}
