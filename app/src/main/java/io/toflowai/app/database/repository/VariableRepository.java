package io.toflowai.app.database.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.toflowai.app.entity.VariableEntity;
import io.toflowai.common.dto.VariableDTO.VariableScope;

/**
 * Repository for Variable entities.
 */
@Repository
public interface VariableRepository extends JpaRepository<VariableEntity, Long> {

    /**
     * Find all global variables.
     */
    List<VariableEntity> findByScope(VariableScope scope);

    /**
     * Find all variables for a specific workflow.
     */
    List<VariableEntity> findByWorkflowId(Long workflowId);

    /**
     * Find by name and scope (for global variables).
     */
    Optional<VariableEntity> findByNameAndScopeAndWorkflowIdIsNull(String name, VariableScope scope);

    /**
     * Find by name, scope, and workflow ID.
     */
    Optional<VariableEntity> findByNameAndScopeAndWorkflowId(String name, VariableScope scope, Long workflowId);

    /**
     * Find all variables for a workflow (both global and workflow-specific).
     */
    List<VariableEntity> findByScopeOrWorkflowId(VariableScope scope, Long workflowId);

    /**
     * Check if a variable name exists in a scope.
     */
    boolean existsByNameAndScopeAndWorkflowId(String name, VariableScope scope, Long workflowId);
}
