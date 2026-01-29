-- ToFlowAI Initial Schema
-- V001__Initial_Schema.sql

-- Workflows table
CREATE TABLE workflows (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    description     VARCHAR(2000),
    nodes_json      CLOB NOT NULL,
    connections_json CLOB NOT NULL,
    settings_json   CLOB,
    is_active       BOOLEAN DEFAULT FALSE,
    trigger_type    VARCHAR(50) DEFAULT 'MANUAL',
    cron_expression VARCHAR(100),
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL,
    last_executed   TIMESTAMP,
    version         INT DEFAULT 1
);

-- Executions table
CREATE TABLE executions (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    workflow_id       BIGINT NOT NULL,
    status            VARCHAR(50) NOT NULL,
    trigger_type      VARCHAR(50),
    started_at        TIMESTAMP,
    finished_at       TIMESTAMP,
    input_data_json   CLOB,
    output_data_json  CLOB,
    error_message     VARCHAR(4000),
    execution_log     CLOB,
    CONSTRAINT fk_executions_workflow FOREIGN KEY (workflow_id) REFERENCES workflows(id) ON DELETE CASCADE
);

-- Node executions table (detailed per-node tracking)
CREATE TABLE node_executions (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    execution_id      BIGINT NOT NULL,
    node_id           VARCHAR(100) NOT NULL,
    node_type         VARCHAR(100) NOT NULL,
    status            VARCHAR(50) NOT NULL,
    started_at        TIMESTAMP,
    finished_at       TIMESTAMP,
    input_data_json   CLOB,
    output_data_json  CLOB,
    error_message     VARCHAR(4000),
    CONSTRAINT fk_node_executions_execution FOREIGN KEY (execution_id) REFERENCES executions(id) ON DELETE CASCADE
);

-- Credentials table (encrypted storage)
CREATE TABLE credentials (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    type            VARCHAR(100) NOT NULL,
    data_encrypted  CLOB NOT NULL,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL
);

-- Variables table (global workflow variables)
CREATE TABLE variables (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name            VARCHAR(255) NOT NULL UNIQUE,
    var_value       VARCHAR(4000),
    is_secret       BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL
);

-- Settings table
CREATE TABLE settings (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    setting_key     VARCHAR(255) NOT NULL UNIQUE,
    setting_value   VARCHAR(4000),
    category        VARCHAR(100)
);

-- Indexes
CREATE INDEX idx_executions_workflow ON executions(workflow_id);
CREATE INDEX idx_executions_status ON executions(status);
CREATE INDEX idx_executions_started ON executions(started_at);
CREATE INDEX idx_node_executions_execution ON node_executions(execution_id);
CREATE INDEX idx_workflows_active ON workflows(is_active);
CREATE INDEX idx_workflows_trigger ON workflows(trigger_type);
