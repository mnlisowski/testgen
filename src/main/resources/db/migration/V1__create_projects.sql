CREATE TABLE projects (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT projects_id_not_blank CHECK (length(trim(id)) > 0),
    CONSTRAINT projects_name_not_blank CHECK (length(trim(name)) > 0)
);
