package com.mlisows.testgen.app.config;

import com.mlisows.testgen.app.project.ConfiguredProjectsInitializer;
import com.mlisows.testgen.app.project.ProjectRegistry;
import com.mlisows.testgen.infrastructure.persistence.project.ProjectEntity;
import com.mlisows.testgen.infrastructure.persistence.project.ProjectRepository;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@Profile("postgres")
@EntityScan(basePackageClasses = ProjectEntity.class)
@EnableJpaRepositories(basePackageClasses = ProjectRepository.class)
public class PostgresPersistenceConfiguration {
    @Bean
    public ConfiguredProjectsInitializer configuredProjectsInitializer(
            ProjectRegistry projectRegistry,
            ProjectRepository projectRepository
    ) {
        return new ConfiguredProjectsInitializer(projectRegistry, projectRepository);
    }
}
