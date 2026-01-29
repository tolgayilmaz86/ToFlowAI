package io.toflowai.common.domain;

import io.toflowai.common.enums.ExecutionStatus;
import io.toflowai.common.enums.TriggerType;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Represents a single execution of a workflow.
 * Uses Java 25 record for immutable data carrier.
 *
 * @param id             Unique execution identifier
 * @param workflowId     ID of the workflow being executed
 * @param workflowName   Name of the workflow (for display)
 * @param status         Current execution status
 * @param triggerType    How the execution was triggered
 * @param startedAt      When execution started
 * @param finishedAt     When execution completed (null if still running)
 * @param inputData      Initial input data to the workflow
 * @param outputData     Final output data from the workflow
 * @param errorMessage   Error message if execution failed
 * @param nodeExecutions List of per-node execution details
 */
public record Execution(
        Long id,
        Long workflowId,
        String workflowName,
        ExecutionStatus status,
        TriggerType triggerType,
        Instant startedAt,
        Instant finishedAt,
        Map<String, Object> inputData,
        Map<String, Object> outputData,
        String errorMessage,
        List<NodeExecution> nodeExecutions
) {
    /**
     * Per-node execution details.
     */
    public record NodeExecution(
            String nodeId,
            String nodeName,
            String nodeType,
            ExecutionStatus status,
            Instant startedAt,
            Instant finishedAt,
            Map<String, Object> inputData,
            Map<String, Object> outputData,
            String errorMessage
    ) {
        /**
         * Calculate execution duration.
         */
        public Duration duration() {
            if (startedAt == null) return Duration.ZERO;
            Instant end = finishedAt != null ? finishedAt : Instant.now();
            return Duration.between(startedAt, end);
        }
    }

    /**
     * Compact constructor with validation.
     */
    public Execution {
        if (workflowId == null) {
            throw new IllegalArgumentException("Workflow id cannot be null");
        }
        if (status == null) {
            status = ExecutionStatus.PENDING;
        }
        if (nodeExecutions == null) {
            nodeExecutions = List.of();
        }
    }

    /**
     * Calculate total execution duration.
     */
    public Duration duration() {
        if (startedAt == null) return Duration.ZERO;
        Instant end = finishedAt != null ? finishedAt : Instant.now();
        return Duration.between(startedAt, end);
    }

    /**
     * Check if execution is still in progress.
     */
    public boolean isRunning() {
        return status.isRunning();
    }

    /**
     * Check if execution has completed (success, failure, or cancelled).
     */
    public boolean isComplete() {
        return status.isTerminal();
    }

    /**
     * Create a copy with updated status.
     */
    public Execution withStatus(ExecutionStatus newStatus) {
        return new Execution(
                id, workflowId, workflowName, newStatus, triggerType,
                startedAt, finishedAt, inputData, outputData, errorMessage, nodeExecutions
        );
    }

    /**
     * Create a copy marked as finished.
     */
    public Execution withFinished(ExecutionStatus finalStatus, Instant finishTime, Map<String, Object> output, String error) {
        return new Execution(
                id, workflowId, workflowName, finalStatus, triggerType,
                startedAt, finishTime, inputData, output, error, nodeExecutions
        );
    }
}
