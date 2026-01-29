package io.toflowai.app.database.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import io.toflowai.app.database.model.WorkflowEntity;
import io.toflowai.common.enums.TriggerType;

/**
 * Repository for Workflow entities.
 */
@Repository
public interface WorkflowRepository extends JpaRepository<WorkflowEntity, Long> {

    /**
     * Find workflow by exact name.
     */
    Optional<WorkflowEntity> findByName(String name);

    /**
     * Find all workflows by name (case-insensitive).
     */
    List<WorkflowEntity> findByNameContainingIgnoreCase(String name);

    /**
     * Find all active workflows.
     */
    List<WorkflowEntity> findByIsActiveTrue();

    /**
     * Find all workflows by trigger type.
     */
    List<WorkflowEntity> findByTriggerType(TriggerType triggerType);

    /**
     * Find all active scheduled workflows.
     */
    @Query("SELECT w FROM WorkflowEntity w WHERE w.isActive = true AND w.triggerType = 'SCHEDULE'")
    List<WorkflowEntity> findActiveScheduledWorkflows();

    /**
     * Find all active webhook workflows.
     */
    @Query("SELECT w FROM WorkflowEntity w WHERE w.isActive = true AND w.triggerType = 'WEBHOOK'")
    List<WorkflowEntity> findActiveWebhookWorkflows();

    /**
     * Count total workflows.
     */
    @Query("SELECT COUNT(w) FROM WorkflowEntity w")
    long countWorkflows();

    /**
     * Count active workflows.
     */
    @Query("SELECT COUNT(w) FROM WorkflowEntity w WHERE w.isActive = true")
    long countActiveWorkflows();
}
