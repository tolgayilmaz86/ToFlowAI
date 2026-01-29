package io.toflowai.common.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.toflowai.common.dto.ExecutionDTO;

/**
 * Service interface for workflow execution operations.
 * Defined in common module so UI can depend on it without circular dependency.
 */
public interface ExecutionServiceInterface {

    /**
     * Find all executions.
     */
    List<ExecutionDTO> findAll();

    /**
     * Find an execution by ID.
     */
    Optional<ExecutionDTO> findById(Long id);

    /**
     * Find executions by workflow ID.
     */
    List<ExecutionDTO> findByWorkflowId(Long workflowId);

    /**
     * Find currently running executions.
     */
    List<ExecutionDTO> findRunningExecutions();

    /**
     * Find executions within a time range.
     */
    List<ExecutionDTO> findByTimeRange(Instant start, Instant end);

    /**
     * Execute a workflow asynchronously.
     */
    CompletableFuture<ExecutionDTO> executeAsync(Long workflowId, Map<String, Object> input);

    /**
     * Execute a workflow synchronously.
     */
    ExecutionDTO execute(Long workflowId, Map<String, Object> input);
}
