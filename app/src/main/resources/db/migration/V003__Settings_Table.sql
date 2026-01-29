-- V003__Settings_Table.sql
-- Extend settings table with additional columns for enhanced configuration

-- Add new columns to existing settings table
ALTER TABLE settings ADD COLUMN IF NOT EXISTS setting_type VARCHAR(20) NOT NULL DEFAULT 'STRING';
ALTER TABLE settings ADD COLUMN IF NOT EXISTS label VARCHAR(255);
ALTER TABLE settings ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE settings ADD COLUMN IF NOT EXISTS is_visible BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE settings ADD COLUMN IF NOT EXISTS requires_restart BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE settings ADD COLUMN IF NOT EXISTS display_order INT DEFAULT 0;
ALTER TABLE settings ADD COLUMN IF NOT EXISTS validation_rules TEXT;
ALTER TABLE settings ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE settings ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Modify setting_value to CLOB for larger values (if not already)
ALTER TABLE settings ALTER COLUMN setting_value CLOB;

-- Index for category-based queries (already exists from V001)
CREATE INDEX IF NOT EXISTS idx_settings_category ON settings(category);
COMMENT ON COLUMN settings.setting_type IS 'Data type (STRING, INTEGER, BOOLEAN, PASSWORD, etc.)';
COMMENT ON COLUMN settings.validation_rules IS 'JSON validation rules (min, max, options, etc.)';
