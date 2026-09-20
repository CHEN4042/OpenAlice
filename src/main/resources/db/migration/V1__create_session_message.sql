CREATE TABLE session_message (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    session_id VARCHAR(128) NOT NULL,
    role VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_session_message_role CHECK (role IN ('user', 'assistant', 'system'))
);

CREATE INDEX idx_session_message_session_created_at
    ON session_message (session_id, created_at);
