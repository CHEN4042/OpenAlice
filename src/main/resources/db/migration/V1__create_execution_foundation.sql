CREATE TABLE executions (
    execution_id TEXT PRIMARY KEY,
    status TEXT NOT NULL CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED', 'INTERRUPTED')),
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);
