package io.toflowai.common.service;

import java.util.List;
import java.util.Optional;

import io.toflowai.common.dto.WorkflowDTO;
import io.toflowai.common.enums.TriggerType;

/**
 * Service interface for workflow operations.
 * Defined in common module so UI can depend on it without circular dependency.
 */
public interface WorkflowServiceInterface {

    /**
     * Find all workflows.
     */
    List<WorkflowDTO> findAll();

    /**
     * Find a workflow by ID.
     */
    Optional<WorkflowDTO> findById(Long id);

    /**
     * Find workflows by trigger type.
     */
    List<WorkflowDTO> findByTriggerType(TriggerType triggerType);

    /**
     * Create a new workflow.
     */
    WorkflowDTO create(WorkflowDTO dto);

    /**
     * Update an existing workflow.
     */
    WorkflowDTO update(WorkflowDTO dto);

    /**
     * Delete a workflow.
     */
    void delete(Long id);

    /**
     * Duplicate a workflow.
     */
    WorkflowDTO duplicate(Long id);

    /**
     * Set workflow active state.
     */
    void setActive(Long id, boolean active);
}
