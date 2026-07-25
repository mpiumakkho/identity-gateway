CREATE TABLE dip_chip_identity_entries (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL UNIQUE REFERENCES verification_sessions(id) ON DELETE CASCADE,
    national_id VARCHAR(13) NOT NULL,
    title VARCHAR(30) NOT NULL,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    date_of_birth DATE NOT NULL,
    laser_code VARCHAR(20) NOT NULL,
    card_issue_date DATE NOT NULL,
    card_expiry_date DATE NOT NULL,
    reader_name VARCHAR(80) NOT NULL,
    reader_serial_number VARCHAR(80) NOT NULL,
    raw_payload TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_dip_chip_identity_entries_national_id_digits CHECK (national_id ~ '^[0-9]{13}$'),
    CONSTRAINT chk_dip_chip_identity_entries_title_not_blank CHECK (length(trim(title)) > 0),
    CONSTRAINT chk_dip_chip_identity_entries_first_name_not_blank CHECK (length(trim(first_name)) > 0),
    CONSTRAINT chk_dip_chip_identity_entries_last_name_not_blank CHECK (length(trim(last_name)) > 0),
    CONSTRAINT chk_dip_chip_identity_entries_laser_code_length CHECK (length(trim(laser_code)) BETWEEN 8 AND 20),
    CONSTRAINT chk_dip_chip_identity_entries_card_dates CHECK (card_expiry_date >= card_issue_date),
    CONSTRAINT chk_dip_chip_identity_entries_reader_name_not_blank CHECK (length(trim(reader_name)) > 0),
    CONSTRAINT chk_dip_chip_identity_entries_reader_serial_not_blank CHECK (length(trim(reader_serial_number)) > 0),
    CONSTRAINT chk_dip_chip_identity_entries_raw_payload_not_blank CHECK (length(trim(raw_payload)) BETWEEN 1 AND 10000)
);

CREATE INDEX idx_dip_chip_identity_entries_national_id ON dip_chip_identity_entries(national_id);
CREATE INDEX idx_dip_chip_identity_entries_reader_serial_number ON dip_chip_identity_entries(reader_serial_number);