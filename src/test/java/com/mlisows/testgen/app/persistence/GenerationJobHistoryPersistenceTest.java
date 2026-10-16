package com.mlisows.testgen.app.persistence;

import com.mlisows.testgen.domain.generation.GenerationSettings;
import com.mlisows.testgen.domain.profile.ObservedProfile;
import com.mlisows.testgen.infrastructure.persistence.project.ProjectEntity;
import com.mlisows.testgen.infrastructure.persistence.project.ProjectRepository;
import com.mlisows.testgen.usecase.generation.GeneratedTestFile;
import com.mlisows.testgen.usecase.generation.GenerationMetrics;
import com.mlisows.testgen.usecase.generation.GenerationRequest;
import com.mlisows.testgen.usecase.generation.GenerationResult;
import com.mlisows.testgen.usecase.job.GenerationJob;
import com.mlisows.testgen.usecase.job.GenerationJobSummary;
import com.mlisows.testgen.usecase.job.JobPage;
import com.mlisows.testgen.usecase.job.JobStatus;
import com.mlisows.testgen.usecase.ports.GenerationJobStore;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        // Counts SQL statements, so the test can check that a page does not trigger extra queries.
        properties = "spring.jpa.properties.hibernate.generate_statistics=true"
)
@Testcontainers
class GenerationJobHistoryPersistenceTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Container
    @ServiceConnection
    static final KafkaContainer kafka = new KafkaContainer("apache/kafka:4.1.0");

    @Autowired
    private GenerationJobStore jobStore;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void shouldReturnProjectJobsNewestFirstInPagesUsingTwoQueriesPerPage() {
        projectRepository.save(new ProjectEntity("other-project", "Other project"));
        GenerationJob oldestJob = jobStore.create("fit-bench", request());
        GenerationJob succeededJob = jobStore.create("fit-bench", request());
        GenerationJob newestJob = jobStore.create("fit-bench", request());
        jobStore.create("other-project", request());
        jobStore.start(succeededJob.id());
        jobStore.succeed(succeededJob.id(), resultWithTestSource());
        List<GenerationJob> expectedOrder = newestFirst(List.of(oldestJob, succeededJob, newestJob));
        assertTrue(haveDistinctCreationTimes(expectedOrder), "the expected order relies on distinct timestamps");

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        JobPage firstPage = jobStore.findByProject("fit-bench", 0, 2);
        long statementsForFirstPage = statistics.getPrepareStatementCount();
        JobPage secondPage = jobStore.findByProject("fit-bench", 1, 2);

        // One query for the rows and one for the total count; no query per job.
        assertEquals(2, statementsForFirstPage);

        assertEquals(3, firstPage.totalElements());
        assertEquals(List.of(expectedOrder.get(0).id(), expectedOrder.get(1).id()), jobIds(firstPage));
        assertEquals(List.of(expectedOrder.get(2).id()), jobIds(secondPage));

        GenerationJobSummary succeededSummary = findSummary(firstPage, secondPage, succeededJob);
        assertEquals(JobStatus.SUCCEEDED, succeededSummary.status());
        assertEquals(new GenerationMetrics(1, 20, 2, 4, 2), succeededSummary.metrics());
        GenerationJobSummary queuedSummary = findSummary(firstPage, secondPage, oldestJob);
        assertEquals(JobStatus.QUEUED, queuedSummary.status());
        assertNull(queuedSummary.metrics());
    }

    private List<GenerationJob> newestFirst(List<GenerationJob> jobs) {
        return jobs.stream()
                .sorted(Comparator.comparing(GenerationJob::createdAt).reversed())
                .toList();
    }

    private boolean haveDistinctCreationTimes(List<GenerationJob> jobs) {
        return jobs.stream().map(GenerationJob::createdAt).distinct().count() == jobs.size();
    }

    private List<UUID> jobIds(JobPage jobPage) {
        return jobPage.jobs().stream().map(GenerationJobSummary::id).toList();
    }

    private GenerationJobSummary findSummary(JobPage firstPage, JobPage secondPage, GenerationJob job) {
        return Stream.concat(firstPage.jobs().stream(), secondPage.jobs().stream())
                .filter(summary -> summary.id().equals(job.id()))
                .findFirst()
                .orElseThrow();
    }

    private GenerationRequest request() {
        return new GenerationRequest(Path.of("/projects/fit-bench"), Path.of("/work"),
                GenerationSettings.defaults(), new ObservedProfile(List.of(), List.of()));
    }

    private GenerationResult resultWithTestSource() {
        return new GenerationResult(
                List.of(new GeneratedTestFile(Path.of("sample/PricingTest.java"), "class PricingTest {}")),
                "report text",
                new GenerationMetrics(1, 20, 2, 4, 2),
                Set.of("goal-a", "goal-b")
        );
    }
}
