-- V003__Settings_Table.sql
-- Settings table for application configuration

CREATE TABLE settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(255) NOT NULL UNIQUE,
    setting_value CLOB,
    category VARCHAR(50) NOT NULL,
    setting_type VARCHAR(20) NOT NULL,
    label VARCHAR(255),
    description TEXT,
    is_visible BOOLEAN NOT NULL DEFAULT TRUE,
    requires_restart BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT DEFAULT 0,
    validation_rules TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for fast key lookup
CREATE INDEX idx_settings_key ON settings(setting_key);

-- Index for category-based queries
CREATE INDEX idx_settings_category ON settings(category);

-- Comments for documentation
COMMENT ON TABLE settings IS 'Application settings and user preferences';
COMMENT ON COLUMN settings.setting_key IS 'Unique setting identifier (e.g., general.theme)';
COMMENT ON COLUMN settings.setting_value IS 'Setting value (encrypted for PASSWORD type)';
COMMENT ON COLUMN settings.category IS 'Setting category for UI grouping';
COMMENT ON COLUMN settings.setting_type IS 'Data type (STRING, INTEGER, BOOLEAN, PASSWORD, etc.)';
COMMENT ON COLUMN settings.validation_rules IS 'JSON validation rules (min, max, options, etc.)';
