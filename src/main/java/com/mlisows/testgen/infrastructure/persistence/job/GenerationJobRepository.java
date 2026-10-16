package com.mlisows.testgen.infrastructure.persistence.job;

import com.mlisows.testgen.usecase.job.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface GenerationJobRepository extends JpaRepository<GenerationJobEntity, UUID> {
    // JPQL refers to the entity and its fields, not to table and column names.
    // Selecting only the id avoids loading whole jobs.
    @Query("SELECT job.id FROM GenerationJobEntity job WHERE job.status = :status ORDER BY job.createdAt")
    List<UUID> findIdsByStatus(@Param("status") JobStatus status);
}
