CREATE TABLE verification_methods (
    id VARCHAR(40) PRIMARY KEY,
    label VARCHAR(80) NOT NULL,
    description VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO verification_methods (id, label, description, enabled, sort_order)
VALUES
    ('DIP_CHIP', 'Dip Chip', 'Read citizen card data from a supported reader.', TRUE, 10),
    ('MANUAL_ENTRY', 'Manual Entry', 'Capture citizen data through a controlled form.', TRUE, 20);

CREATE INDEX idx_verification_methods_enabled_sort ON verification_methods(enabled, sort_order);
