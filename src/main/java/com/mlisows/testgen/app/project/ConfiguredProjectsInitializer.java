package com.mlisows.testgen.app.project;

import com.mlisows.testgen.infrastructure.persistence.project.ProjectEntity;
import com.mlisows.testgen.infrastructure.persistence.project.ProjectRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.transaction.annotation.Transactional;

public class ConfiguredProjectsInitializer implements ApplicationRunner {
    private final ProjectRegistry projectRegistry;
    private final ProjectRepository projectRepository;

    public ConfiguredProjectsInitializer(ProjectRegistry projectRegistry, ProjectRepository projectRepository) {
        this.projectRegistry = projectRegistry;
        this.projectRepository = projectRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        for (String projectId : projectRegistry.projectIds()) {
            if (!projectRepository.existsById(projectId)) {
                projectRepository.save(new ProjectEntity(projectId, projectId));
            }
        }
    }
}
