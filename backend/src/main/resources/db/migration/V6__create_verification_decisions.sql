CREATE TABLE verification_decisions (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL UNIQUE REFERENCES verification_sessions(id) ON DELETE CASCADE,
    decision VARCHAR(40) NOT NULL,
    notes VARCHAR(1000),
    decided_by UUID NOT NULL REFERENCES operator_users(id),
    decided_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_verification_decisions_decision CHECK (decision IN ('APPROVED', 'REJECTED')),
    CONSTRAINT chk_verification_decisions_notes_not_blank CHECK (notes IS NULL OR length(trim(notes)) > 0)
);

CREATE INDEX idx_verification_decisions_decision ON verification_decisions(decision);
CREATE INDEX idx_verification_decisions_decided_at ON verification_decisions(decided_at);