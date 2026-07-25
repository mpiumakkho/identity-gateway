CREATE TABLE manual_identity_entries (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL UNIQUE REFERENCES verification_sessions(id) ON DELETE CASCADE,
    national_id VARCHAR(13) NOT NULL,
    title VARCHAR(30) NOT NULL,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    date_of_birth DATE NOT NULL,
    laser_code VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_manual_identity_entries_national_id_digits CHECK (national_id ~ '^[0-9]{13}$'),
    CONSTRAINT chk_manual_identity_entries_title_not_blank CHECK (length(trim(title)) > 0),
    CONSTRAINT chk_manual_identity_entries_first_name_not_blank CHECK (length(trim(first_name)) > 0),
    CONSTRAINT chk_manual_identity_entries_last_name_not_blank CHECK (length(trim(last_name)) > 0),
    CONSTRAINT chk_manual_identity_entries_laser_code_length CHECK (length(trim(laser_code)) BETWEEN 8 AND 20)
);

CREATE INDEX idx_manual_identity_entries_national_id ON manual_identity_entries(national_id);
