package com.mlisows.testgen.app.config;

import com.mlisows.testgen.app.project.ConfiguredProjectsInitializer;
import com.mlisows.testgen.app.project.ProjectRegistry;
import com.mlisows.testgen.infrastructure.persistence.profile.JpaObservedProfileStore;
import com.mlisows.testgen.infrastructure.persistence.profile.ObservedProfileEntity;
import com.mlisows.testgen.infrastructure.persistence.profile.ObservedProfileRepository;
import com.mlisows.testgen.infrastructure.persistence.project.ProjectEntity;
import com.mlisows.testgen.infrastructure.persistence.project.ProjectRepository;
import com.mlisows.testgen.infrastructure.runtime.TextObservedProfileReader;
import com.mlisows.testgen.usecase.ports.ObservedProfileStore;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@Profile("postgres")
@EntityScan(basePackageClasses = {ProjectEntity.class, ObservedProfileEntity.class})
@EnableJpaRepositories(basePackageClasses = {ProjectRepository.class, ObservedProfileRepository.class})
public class PostgresPersistenceConfiguration {
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
}
