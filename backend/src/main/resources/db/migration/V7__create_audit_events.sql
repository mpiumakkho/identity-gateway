CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(80) NOT NULL,
    operator_id UUID NULL REFERENCES operator_users(id) ON DELETE SET NULL,
    session_id UUID NULL REFERENCES verification_sessions(id) ON DELETE CASCADE,
    summary VARCHAR(255) NOT NULL,
    metadata_json VARCHAR(2000),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_audit_events_event_type CHECK (event_type IN (
        'AUTH_LOGIN_SUCCEEDED',
        'AUTH_LOGIN_FAILED',
        'AUTH_LOGOUT',
        'VERIFICATION_SESSION_CREATED',
        'MANUAL_IDENTITY_CAPTURED',
        'DIP_CHIP_PAYLOAD_CAPTURED',
        'DOPA_VALIDATION_COMPLETED',
        'VERIFICATION_CLOSED'
    )),
    CONSTRAINT chk_audit_events_summary_not_blank CHECK (length(trim(summary)) > 0),
    CONSTRAINT chk_audit_events_metadata_json_not_blank CHECK (metadata_json IS NULL OR length(trim(metadata_json)) > 0)
);

CREATE INDEX idx_audit_events_session_id ON audit_events(session_id);
CREATE INDEX idx_audit_events_operator_id ON audit_events(operator_id);
CREATE INDEX idx_audit_events_event_type ON audit_events(event_type);
CREATE INDEX idx_audit_events_occurred_at ON audit_events(occurred_at);