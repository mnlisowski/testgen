package com.mlisows.testgen.infrastructure.persistence.job;

import com.mlisows.testgen.usecase.job.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // One query for the page: only the columns shown in the history, without the profile content,
    // the report or the test sources. Spring Data adds LIMIT/OFFSET from the Pageable and runs
    // the count query to calculate the total number of jobs.
    @Query(
            value = """
                    SELECT new com.mlisows.testgen.infrastructure.persistence.job.GenerationJobSummaryRow(
                        job.id, job.project.id, profile.id, job.status,
                        job.settings.seed, job.settings.maxCandidates, job.settings.initialCandidates,
                        job.settings.mutatedInitialCandidates, job.settings.maxValuesPerSlot,
                        job.createdAt, job.startedAt, job.finishedAt, job.error,
                        result.metrics.analyzedClasses, result.metrics.executedCandidates,
                        result.metrics.selectedCandidates, result.metrics.coverageGoals,
                        result.metrics.coveredGoals)
                    FROM GenerationJobEntity job
                    LEFT JOIN job.profile profile
                    LEFT JOIN GenerationResultEntity result ON result.jobId = job.id
                    WHERE job.project.id = :projectId
                    ORDER BY job.createdAt DESC, job.id DESC
                    """,
            countQuery = "SELECT count(job) FROM GenerationJobEntity job WHERE job.project.id = :projectId"
    )
    Page<GenerationJobSummaryRow> findSummariesByProjectId(@Param("projectId") String projectId, Pageable pageable);
}
