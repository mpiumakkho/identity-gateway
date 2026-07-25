CREATE TABLE operator_sessions (
    id UUID PRIMARY KEY,
    operator_id UUID NOT NULL REFERENCES operator_users(id),
    token_hash VARCHAR(96) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMPTZ NULL
);

CREATE INDEX idx_operator_sessions_operator_id ON operator_sessions(operator_id);
CREATE INDEX idx_operator_sessions_expires_at ON operator_sessions(expires_at);