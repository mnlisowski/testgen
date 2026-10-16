package com.mlisows.testgen.infrastructure.persistence.project;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<ProjectEntity, String> {
}
