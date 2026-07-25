CREATE TABLE dopa_validation_attempts (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES verification_sessions(id) ON DELETE CASCADE,
    identity_source VARCHAR(40) NOT NULL,
    result_status VARCHAR(40) NOT NULL,
    response_code VARCHAR(40) NOT NULL,
    response_message VARCHAR(255) NOT NULL,
    consent_reference VARCHAR(80) NOT NULL,
    validated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_dopa_validation_attempts_identity_source CHECK (identity_source IN ('MANUAL_ENTRY', 'DIP_CHIP')),
    CONSTRAINT chk_dopa_validation_attempts_result_status CHECK (result_status IN ('MATCHED', 'NOT_MATCHED')),
    CONSTRAINT chk_dopa_validation_attempts_response_code_not_blank CHECK (length(trim(response_code)) > 0),
    CONSTRAINT chk_dopa_validation_attempts_response_message_not_blank CHECK (length(trim(response_message)) > 0),
    CONSTRAINT chk_dopa_validation_attempts_consent_reference_not_blank CHECK (length(trim(consent_reference)) > 0)
);

CREATE INDEX idx_dopa_validation_attempts_session_id ON dopa_validation_attempts(session_id);
CREATE INDEX idx_dopa_validation_attempts_validated_at ON dopa_validation_attempts(validated_at);