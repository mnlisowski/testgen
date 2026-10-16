CREATE TABLE generation_jobs (
    id UUID PRIMARY KEY,
    project_id VARCHAR(100) NOT NULL REFERENCES projects (id),
    profile_id UUID REFERENCES observed_profiles (id),
    status VARCHAR(20) NOT NULL,
    project_root TEXT NOT NULL,
    work_root TEXT NOT NULL,
    seed BIGINT NOT NULL,
    max_candidates INTEGER NOT NULL,
    initial_candidates INTEGER NOT NULL,
    mutated_initial_candidates INTEGER NOT NULL,
    max_values_per_slot INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    error TEXT,
    version BIGINT NOT NULL,
    CONSTRAINT generation_jobs_status_known CHECK (
        status IN ('QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'TIMED_OUT', 'INTERRUPTED')
    )
);

CREATE INDEX generation_jobs_project_created_idx ON generation_jobs (project_id, created_at DESC);

CREATE TABLE generation_results (
    job_id UUID PRIMARY KEY REFERENCES generation_jobs (id),
    report_text TEXT NOT NULL,
    analyzed_classes INTEGER NOT NULL,
    executed_candidates INTEGER NOT NULL,
    selected_candidates INTEGER NOT NULL,
    coverage_goals INTEGER NOT NULL,
    covered_goals INTEGER NOT NULL
);

CREATE TABLE generated_test_files (
    job_id UUID NOT NULL REFERENCES generation_results (job_id),
    relative_path TEXT NOT NULL,
    content TEXT NOT NULL,
    PRIMARY KEY (job_id, relative_path)
);

CREATE TABLE covered_goals (
    job_id UUID NOT NULL REFERENCES generation_results (job_id),
    goal_id TEXT NOT NULL,
    PRIMARY KEY (job_id, goal_id)
);
