package io.toflowai.common.enums;

/**
 * Status of a workflow execution.
 */
public enum ExecutionStatus {
    PENDING("Pending", "Execution is queued"),
    RUNNING("Running", "Execution is in progress"),
    SUCCESS("Success", "Execution completed successfully"),
    FAILED("Failed", "Execution failed with an error"),
    CANCELLED("Cancelled", "Execution was cancelled by user"),
    WAITING("Waiting", "Execution is waiting for external event");

    private final String displayName;
    private final String description;

    ExecutionStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED || this == CANCELLED;
    }

    public boolean isRunning() {
        return this == RUNNING || this == WAITING;
    }
}
