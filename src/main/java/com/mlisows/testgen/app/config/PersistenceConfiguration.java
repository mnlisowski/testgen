package com.mlisows.testgen.app.config;

import com.mlisows.testgen.app.job.UnfinishedJobsRecovery;
import com.mlisows.testgen.app.project.ConfiguredProjectsInitializer;
import com.mlisows.testgen.app.project.ProjectRegistry;
import com.mlisows.testgen.infrastructure.persistence.job.GenerationJobEntity;
import com.mlisows.testgen.infrastructure.persistence.job.GenerationJobRepository;
import com.mlisows.testgen.infrastructure.persistence.job.GenerationResultRepository;
import com.mlisows.testgen.infrastructure.persistence.job.JpaGenerationJobStore;
import com.mlisows.testgen.infrastructure.persistence.profile.JpaObservedProfileStore;
import com.mlisows.testgen.infrastructure.persistence.profile.ObservedProfileEntity;
import com.mlisows.testgen.infrastructure.persistence.profile.ObservedProfileRepository;
import com.mlisows.testgen.infrastructure.persistence.project.ProjectEntity;
import com.mlisows.testgen.infrastructure.persistence.project.ProjectRepository;
import com.mlisows.testgen.infrastructure.runtime.TextObservedProfileReader;
import com.mlisows.testgen.usecase.ports.GenerationJobStore;
import com.mlisows.testgen.usecase.ports.ObservedProfileStore;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@EntityScan(basePackageClasses = {ProjectEntity.class, ObservedProfileEntity.class, GenerationJobEntity.class})
@EnableJpaRepositories(basePackageClasses = {
        ProjectRepository.class,
        ObservedProfileRepository.class,
        GenerationJobRepository.class
})
public class PersistenceConfiguration {
    @Bean
    public ConfiguredProjectsInitializer configuredProjectsInitializer(
            ProjectRegistry projectRegistry,
            ProjectRepository projectRepository
    ) {
        return new ConfiguredProjectsInitializer(projectRegistry, projectRepository);
    }

    @Bean
    public ObservedProfileStore observedProfileStore(
            ObservedProfileRepository profileRepository,
            ProjectRepository projectRepository,
            TextObservedProfileReader profileReader
    ) {
        return new JpaObservedProfileStore(profileRepository, projectRepository, profileReader, Clock.systemUTC());
    }

    @Bean
    public GenerationJobStore generationJobStore(
            GenerationJobRepository jobRepository,
            GenerationResultRepository resultRepository,
            ProjectRepository projectRepository,
            ObservedProfileRepository profileRepository,
            ObservedProfileStore profileStore,
            TransactionTemplate transactionTemplate
    ) {
        return new JpaGenerationJobStore(jobRepository, resultRepository, projectRepository, profileRepository,
                profileStore, transactionTemplate, Clock.systemUTC());
    }

    @Bean
    public UnfinishedJobsRecovery unfinishedJobsRecovery(GenerationJobStore jobStore) {
        return new UnfinishedJobsRecovery(jobStore);
    }
}
