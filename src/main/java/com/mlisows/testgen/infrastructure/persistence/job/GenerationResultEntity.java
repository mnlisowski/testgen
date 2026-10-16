package com.mlisows.testgen.infrastructure.persistence.job;

import com.mlisows.testgen.usecase.generation.GeneratedTestFile;
import com.mlisows.testgen.usecase.generation.GenerationResult;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Result of a succeeded job. It is a separate table, so reading job statuses does not load
 * the report and test sources.
 */
@Entity
@Table(name = "generation_results")
public class GenerationResultEntity {
    @Id
    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    // @MapsId: the primary key of this row is the id of its job.
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id")
    private GenerationJobEntity job;

    @Column(name = "report_text", nullable = false, columnDefinition = "TEXT")
    private String reportText;

    @Embedded
    private GenerationMetricsEmbeddable metrics;

    @ElementCollection
    @CollectionTable(name = "generated_test_files", joinColumns = @JoinColumn(name = "job_id"))
    @OrderBy("relativePath")
    private List<GeneratedTestFileEmbeddable> tests = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "covered_goals", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "goal_id", nullable = false, columnDefinition = "TEXT")
    private Set<String> coveredGoalIds = new HashSet<>();

    protected GenerationResultEntity() {
    }

    public GenerationResultEntity(GenerationJobEntity job, GenerationResult result) {
        this.job = Objects.requireNonNull(job, "job");
        this.reportText = result.reportText();
        this.metrics = new GenerationMetricsEmbeddable(result.metrics());
        for (GeneratedTestFile testFile : result.tests()) {
            this.tests.add(new GeneratedTestFileEmbeddable(testFile));
        }
        this.coveredGoalIds.addAll(result.coveredGoalIds());
    }

    public GenerationResult toResult() {
        List<GeneratedTestFile> testFiles = new ArrayList<>();
        for (GeneratedTestFileEmbeddable test : tests) {
            testFiles.add(test.toTestFile());
        }
        return new GenerationResult(testFiles, reportText, metrics.toMetrics(), coveredGoalIds);
    }
}
