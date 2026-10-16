CREATE TABLE observed_profiles (
    id UUID PRIMARY KEY,
    project_id VARCHAR(100) NOT NULL REFERENCES projects (id),
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX observed_profiles_project_id_idx ON observed_profiles (project_id);
