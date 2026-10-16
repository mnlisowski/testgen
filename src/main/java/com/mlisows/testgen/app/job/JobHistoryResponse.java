package com.mlisows.testgen.app.job;

import com.mlisows.testgen.usecase.job.GenerationJobSummary;
import com.mlisows.testgen.usecase.job.JobPage;

import java.util.ArrayList;
import java.util.List;

public record JobHistoryResponse(List<JobResponse> jobs, int page, int size, long totalElements) {
    public static JobHistoryResponse from(JobPage jobPage) {
        List<JobResponse> jobs = new ArrayList<>();
        for (GenerationJobSummary job : jobPage.jobs()) {
            jobs.add(JobResponse.from(job));
        }
        return new JobHistoryResponse(jobs, jobPage.page(), jobPage.size(), jobPage.totalElements());
    }
}
