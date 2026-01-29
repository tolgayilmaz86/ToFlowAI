package io.toflowai.app.database.repository;

import io.toflowai.app.database.model.ExecutionEntity;
import io.toflowai.common.enums.ExecutionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for Execution entities.
 */
@Repository
public interface ExecutionRepository extends JpaRepository<ExecutionEntity, Long> {

    /**
     * Find all executions for a workflow.
     */
    List<ExecutionEntity> findByWorkflowIdOrderByStartedAtDesc(Long workflowId);

    /**
     * Find paginated executions for a workflow.
     */
    Page<ExecutionEntity> findByWorkflowId(Long workflowId, Pageable pageable);

    /**
     * Find all executions by status.
     */
    List<ExecutionEntity> findByStatus(ExecutionStatus status);

    /**
     * Find running executions.
     */
    @Query("SELECT e FROM ExecutionEntity e WHERE e.status IN ('RUNNING', 'WAITING')")
    List<ExecutionEntity> findRunningExecutions();

    /**
     * Find recent executions.
     */
    @Query("SELECT e FROM ExecutionEntity e ORDER BY e.startedAt DESC")
    List<ExecutionEntity> findRecentExecutions(Pageable pageable);

    /**
     * Find executions within a time range.
     */
    @Query("SELECT e FROM ExecutionEntity e WHERE e.startedAt BETWEEN :start AND :end ORDER BY e.startedAt DESC")
    List<ExecutionEntity> findByTimeRange(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * Count executions by status for a workflow.
     */
    @Query("SELECT COUNT(e) FROM ExecutionEntity e WHERE e.workflowId = :workflowId AND e.status = :status")
    long countByWorkflowIdAndStatus(@Param("workflowId") Long workflowId, @Param("status") ExecutionStatus status);

    /**
     * Count successful executions for a workflow.
     */
    @Query("SELECT COUNT(e) FROM ExecutionEntity e WHERE e.workflowId = :workflowId AND e.status = 'SUCCESS'")
    long countSuccessfulByWorkflowId(@Param("workflowId") Long workflowId);

    /**
     * Count failed executions for a workflow.
     */
    @Query("SELECT COUNT(e) FROM ExecutionEntity e WHERE e.workflowId = :workflowId AND e.status = 'FAILED'")
    long countFailedByWorkflowId(@Param("workflowId") Long workflowId);

    /**
     * Delete old executions (cleanup).
     */
    @Query("DELETE FROM ExecutionEntity e WHERE e.finishedAt < :cutoff")
    void deleteOlderThan(@Param("cutoff") Instant cutoff);
}
