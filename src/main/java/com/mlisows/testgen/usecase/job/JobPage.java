package com.mlisows.testgen.usecase.job;

import java.util.List;

/**
 * One page of job summaries. The use case layer has its own type instead of Spring Data's
 * {@code Page}, so it does not depend on Spring.
 *
 * @param page          zero-based page number
 * @param size          requested page size; {@code jobs} can be shorter on the last page
 * @param totalElements number of jobs on all pages
 */
public record JobPage(List<GenerationJobSummary> jobs, int page, int size, long totalElements) {
    public JobPage {
        jobs = List.copyOf(jobs);
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be positive");
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements must not be negative");
        }
    }
}
