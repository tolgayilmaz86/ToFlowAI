package io.toflowai.app.database.model;

import io.toflowai.common.enums.ExecutionStatus;
import io.toflowai.common.enums.TriggerType;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * JPA Entity for Workflow Execution.
 */
@Entity
@Table(name = "executions", indexes = {
        @Index(name = "idx_executions_workflow", columnList = "workflow_id"),
        @Index(name = "idx_executions_status", columnList = "status"),
        @Index(name = "idx_executions_started", columnList = "started_at")
})
public class ExecutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workflow_id", nullable = false)
    private Long workflowId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status = ExecutionStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type")
    private TriggerType triggerType;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "input_data_json", columnDefinition = "CLOB")
    private String inputDataJson;

    @Column(name = "output_data_json", columnDefinition = "CLOB")
    private String outputDataJson;

    @Column(name = "error_message", length = 4000)
    private String errorMessage;

    @Column(name = "execution_log", columnDefinition = "CLOB")
    private String executionLog;

    // Default constructor for JPA
    protected ExecutionEntity() {
    }

    public ExecutionEntity(Long workflowId, TriggerType triggerType) {
        this.workflowId = workflowId;
        this.triggerType = triggerType;
        this.status = ExecutionStatus.PENDING;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(Long workflowId) {
        this.workflowId = workflowId;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public TriggerType getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(TriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getInputDataJson() {
        return inputDataJson;
    }

    public void setInputDataJson(String inputDataJson) {
        this.inputDataJson = inputDataJson;
    }

    public String getOutputDataJson() {
        return outputDataJson;
    }

    public void setOutputDataJson(String outputDataJson) {
        this.outputDataJson = outputDataJson;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getExecutionLog() {
        return executionLog;
    }

    public void setExecutionLog(String executionLog) {
        this.executionLog = executionLog;
    }
}
