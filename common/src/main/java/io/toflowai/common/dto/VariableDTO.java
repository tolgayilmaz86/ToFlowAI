package io.toflowai.common.dto;

import java.time.Instant;

/**
 * DTO for workflow variables.
 * Variables can be global (shared across workflows) or workflow-specific.
 */
public record VariableDTO(
        Long id,
        String name,
        String value,
        VariableType type,
        VariableScope scope,
        Long workflowId, // null for global variables
        String description,
        Instant createdAt,
        Instant updatedAt) {
    /**
     * Create a new global variable.
     */
    public static VariableDTO globalVariable(String name, String value, VariableType type, String description) {
        return new VariableDTO(null, name, value, type, VariableScope.GLOBAL, null, description, null, null);
    }

    /**
     * Create a new workflow variable.
     */
    public static VariableDTO workflowVariable(String name, String value, VariableType type, Long workflowId,
            String description) {
        return new VariableDTO(null, name, value, type, VariableScope.WORKFLOW, workflowId, description, null, null);
    }

    /**
     * Variable value types.
     */
    public enum VariableType {
        STRING("String", "Text value"),
        NUMBER("Number", "Numeric value (integer or decimal)"),
        BOOLEAN("Boolean", "True/False value"),
        JSON("JSON", "JSON object or array"),
        SECRET("Secret", "Encrypted sensitive value");

        private final String displayName;
        private final String description;

        VariableType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Variable scope.
     */
    public enum VariableScope {
        GLOBAL("Global", "Available across all workflows"),
        WORKFLOW("Workflow", "Available only within a specific workflow"),
        EXECUTION("Execution", "Temporary, only during execution");

        private final String displayName;
        private final String description;

        VariableScope(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }
}
