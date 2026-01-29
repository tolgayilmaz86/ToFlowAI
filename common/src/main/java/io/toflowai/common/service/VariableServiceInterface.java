package io.toflowai.common.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.toflowai.common.dto.VariableDTO;
import io.toflowai.common.dto.VariableDTO.VariableScope;

/**
 * Service interface for managing workflow variables.
 */
public interface VariableServiceInterface {

    /**
     * Find all variables.
     */
    List<VariableDTO> findAll();

    /**
     * Find all global variables.
     */
    List<VariableDTO> findGlobalVariables();

    /**
     * Find all variables for a specific workflow.
     */
    List<VariableDTO> findByWorkflowId(Long workflowId);

    /**
     * Find a variable by ID.
     */
    Optional<VariableDTO> findById(Long id);

    /**
     * Find a variable by name and scope.
     */
    Optional<VariableDTO> findByNameAndScope(String name, VariableScope scope, Long workflowId);

    /**
     * Create a new variable.
     */
    VariableDTO create(VariableDTO variable);

    /**
     * Update an existing variable.
     */
    VariableDTO update(Long id, VariableDTO variable);

    /**
     * Delete a variable.
     */
    void delete(Long id);

    /**
     * Get the resolved value of a variable.
     * For SECRET types, returns the decrypted value.
     */
    String getResolvedValue(Long id);

    /**
     * Get all variables as a map (name -> value) for a given workflow.
     * Includes both global and workflow-specific variables.
     * Workflow variables override global variables with the same name.
     */
    Map<String, Object> getVariablesForWorkflow(Long workflowId);

    /**
     * Resolve variable references in a string.
     * Replaces ${variableName} with actual values.
     */
    String resolveVariables(String input, Long workflowId);

    /**
     * Validate variable name format.
     */
    boolean isValidVariableName(String name);
}
