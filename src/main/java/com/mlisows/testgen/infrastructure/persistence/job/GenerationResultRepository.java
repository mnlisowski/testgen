package com.mlisows.testgen.infrastructure.persistence.job;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GenerationResultRepository extends JpaRepository<GenerationResultEntity, UUID> {
}
