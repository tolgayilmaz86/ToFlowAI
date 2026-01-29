-- Extend variables table for workflow-specific variables and encryption

-- Add new columns to existing variables table
ALTER TABLE variables ADD COLUMN IF NOT EXISTS encrypted_value CLOB;
ALTER TABLE variables ADD COLUMN IF NOT EXISTS var_type VARCHAR(20) DEFAULT 'STRING';
ALTER TABLE variables ADD COLUMN IF NOT EXISTS var_scope VARCHAR(20) DEFAULT 'GLOBAL';
ALTER TABLE variables ADD COLUMN IF NOT EXISTS workflow_id BIGINT;
ALTER TABLE variables ADD COLUMN IF NOT EXISTS description VARCHAR(500);
ALTER TABLE variables ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- Drop old is_secret column if exists (replaced by var_type=SECRET)
ALTER TABLE variables DROP COLUMN IF EXISTS is_secret;

-- Make var_type and var_scope NOT NULL (set defaults first)
UPDATE variables SET var_type = 'STRING' WHERE var_type IS NULL;
UPDATE variables SET var_scope = 'GLOBAL' WHERE var_scope IS NULL;
UPDATE variables SET updated_at = created_at WHERE updated_at IS NULL;

-- Add foreign key constraint
ALTER TABLE variables ADD CONSTRAINT IF NOT EXISTS fk_variable_workflow 
    FOREIGN KEY (workflow_id) REFERENCES workflows(id) ON DELETE CASCADE;

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_variables_scope ON variables(var_scope);
CREATE INDEX IF NOT EXISTS idx_variables_workflow_id ON variables(workflow_id);
