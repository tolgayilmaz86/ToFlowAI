package io.toflowai.app.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.toflowai.app.database.model.ExecutionEntity;
import io.toflowai.app.database.repository.ExecutionRepository;
import io.toflowai.common.domain.Connection;
import io.toflowai.common.domain.Node;
import io.toflowai.common.dto.ExecutionDTO;
import io.toflowai.common.dto.WorkflowDTO;
import io.toflowai.common.enums.ExecutionStatus;
import io.toflowai.common.enums.TriggerType;

/**
 * Service for executing workflows.
 */
@Service
@Transactional
public class ExecutionService {

    private final ExecutionRepository executionRepository;
    private final WorkflowService workflowService;
    private final CredentialService credentialService;
    private final NodeExecutorRegistry nodeExecutorRegistry;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;

    public ExecutionService(
            ExecutionRepository executionRepository,
            WorkflowService workflowService,
            CredentialService credentialService,
            NodeExecutorRegistry nodeExecutorRegistry,
            ObjectMapper objectMapper) {
        this.executionRepository = executionRepository;
        this.workflowService = workflowService;
        this.credentialService = credentialService;
        this.nodeExecutorRegistry = nodeExecutorRegistry;
        this.objectMapper = objectMapper;
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    public List<ExecutionDTO> findAll() {
        return executionRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    public Optional<ExecutionDTO> findById(Long id) {
        return executionRepository.findById(id)
                .map(this::toDTO);
    }

    public List<ExecutionDTO> findByWorkflowId(Long workflowId) {
        return executionRepository.findByWorkflowIdOrderByStartedAtDesc(workflowId).stream()
                .map(this::toDTO)
                .toList();
    }

    public List<ExecutionDTO> findRunningExecutions() {
        return executionRepository.findRunningExecutions().stream()
                .map(this::toDTO)
                .toList();
    }

    public List<ExecutionDTO> findByTimeRange(Instant start, Instant end) {
        return executionRepository.findByTimeRange(start, end).stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Execute a workflow asynchronously.
     */
    public CompletableFuture<ExecutionDTO> executeAsync(Long workflowId, Map<String, Object> input) {
        return CompletableFuture.supplyAsync(() -> execute(workflowId, input), executorService);
    }

    /**
     * Execute a workflow synchronously.
     */
    public ExecutionDTO execute(Long workflowId, Map<String, Object> input) {
        WorkflowDTO workflow = workflowService.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));

        // Create execution record
        ExecutionEntity execution = new ExecutionEntity(workflowId, TriggerType.MANUAL);
        execution.setStatus(ExecutionStatus.RUNNING);
        execution.setStartedAt(Instant.now());
        execution.setInputDataJson(serializeData(input));
        execution = executionRepository.save(execution);

        try {
            // Build execution context
            ExecutionContext context = new ExecutionContext(
                    execution.getId(),
                    workflow,
                    input,
                    credentialService);

            // Execute workflow
            Map<String, Object> output = executeWorkflow(workflow, context);

            // Update execution as success
            execution.setStatus(ExecutionStatus.SUCCESS);
            execution.setFinishedAt(Instant.now());
            execution.setOutputDataJson(serializeData(output));
            execution.setExecutionLog(serializeData(context.getNodeExecutions()));
            executionRepository.save(execution);

        } catch (Exception e) {
            // Update execution as failed
            execution.setStatus(ExecutionStatus.FAILED);
            execution.setFinishedAt(Instant.now());
            execution.setErrorMessage(e.getMessage());
            executionRepository.save(execution);
        }

        return toDTO(execution);
    }

    /**
     * Cancel a running execution.
     */
    public void cancel(Long executionId) {
        ExecutionEntity execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));

        if (execution.getStatus() == ExecutionStatus.RUNNING) {
            execution.setStatus(ExecutionStatus.CANCELLED);
            execution.setFinishedAt(Instant.now());
            executionRepository.save(execution);
        }
    }

    private Map<String, Object> executeWorkflow(WorkflowDTO workflow, ExecutionContext context) {
        // Find trigger nodes (entry points)
        List<Node> triggerNodes = workflow.getTriggerNodes();
        if (triggerNodes.isEmpty()) {
            throw new IllegalStateException("Workflow has no trigger nodes");
        }

        // Execute starting from trigger nodes
        Map<String, Object> lastOutput = new HashMap<>();
        for (Node trigger : triggerNodes) {
            lastOutput = executeNode(trigger, workflow, context, context.getInput());
        }

        return lastOutput;
    }

    private Map<String, Object> executeNode(
            Node node,
            WorkflowDTO workflow,
            ExecutionContext context,
            Map<String, Object> input) {

        if (node.disabled()) {
            // Skip disabled nodes
            return input;
        }

        Instant startTime = Instant.now();
        Map<String, Object> output;

        try {
            // Get executor for node type
            NodeExecutor executor = nodeExecutorRegistry.getExecutor(node.type());

            // Execute node
            output = executor.execute(node, input, context);

            // Record successful execution
            context.recordNodeExecution(node.id(), ExecutionStatus.SUCCESS, startTime, output, null);

        } catch (Exception e) {
            // Record failed execution
            context.recordNodeExecution(node.id(), ExecutionStatus.FAILED, startTime, null, e.getMessage());
            throw new RuntimeException("Node execution failed: " + node.name(), e);
        }

        // Execute connected nodes - use virtual threads for parallel branches
        List<Connection> outgoing = workflow.getOutgoingConnections(node.id());

        if (outgoing.size() > 1) {
            // Multiple outgoing connections - execute in parallel with virtual threads
            try (var scope = new java.util.concurrent.StructuredTaskScope.ShutdownOnFailure()) {
                Map<String, Object> finalOutput = output;
                List<java.util.concurrent.StructuredTaskScope.Subtask<Map<String, Object>>> subtasks = outgoing.stream()
                        .map(connection -> scope.fork(() -> {
                            Node targetNode = workflow.findNode(connection.targetNodeId());
                            if (targetNode == null) {
                                throw new IllegalStateException("Target node not found: " + connection.targetNodeId());
                            }
                            return executeNode(targetNode, workflow, context, finalOutput);
                        }))
                        .toList();

                scope.join();
                scope.throwIfFailed();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Parallel execution interrupted", e);
            } catch (java.util.concurrent.ExecutionException e) {
                throw new RuntimeException("Parallel execution failed", e.getCause());
            }
        } else {
            // Single connection - execute sequentially
            for (Connection connection : outgoing) {
                Node targetNode = workflow.findNode(connection.targetNodeId());
                if (targetNode == null) {
                    throw new IllegalStateException("Target node not found: " + connection.targetNodeId());
                }
                executeNode(targetNode, workflow, context, output);
            }
        }

        return output;
    }

    private ExecutionDTO toDTO(ExecutionEntity entity) {
        List<ExecutionDTO.NodeExecutionDTO> nodeExecutions = parseNodeExecutions(entity.getExecutionLog());

        Long durationMs = null;
        if (entity.getStartedAt() != null && entity.getFinishedAt() != null) {
            durationMs = java.time.Duration.between(entity.getStartedAt(), entity.getFinishedAt()).toMillis();
        }

        return new ExecutionDTO(
                entity.getId(),
                entity.getWorkflowId(),
                null, // workflowName - could fetch from workflow if needed
                entity.getStatus(),
                entity.getTriggerType(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                durationMs,
                parseData(entity.getInputDataJson()),
                parseData(entity.getOutputDataJson()),
                entity.getErrorMessage(),
                nodeExecutions);
    }

    private String serializeData(Object data) {
        if (data == null)
            return null;
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize data", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseData(String json) {
        if (json == null || json.isBlank())
            return Map.of();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse data", e);
        }
    }

    private List<ExecutionDTO.NodeExecutionDTO> parseNodeExecutions(String json) {
        if (json == null || json.isBlank())
            return List.of();
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class,
                            ExecutionDTO.NodeExecutionDTO.class));
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    /**
     * Execution context holds state during workflow execution.
     */
    public static class ExecutionContext {
        private final Long executionId;
        private final WorkflowDTO workflow;
        private final Map<String, Object> input;
        private final CredentialService credentialService;
        private final List<Map<String, Object>> nodeExecutions = new ArrayList<>();

        public ExecutionContext(Long executionId, WorkflowDTO workflow,
                Map<String, Object> input, CredentialService credentialService) {
            this.executionId = executionId;
            this.workflow = workflow;
            this.input = input;
            this.credentialService = credentialService;
        }

        public Long getExecutionId() {
            return executionId;
        }

        public WorkflowDTO getWorkflow() {
            return workflow;
        }

        public Map<String, Object> getInput() {
            return input;
        }

        public String getDecryptedCredential(Long credentialId) {
            return credentialService.getDecryptedData(credentialId);
        }

        public void recordNodeExecution(String nodeId, ExecutionStatus status,
                Instant startTime, Map<String, Object> output, String error) {
            nodeExecutions.add(Map.of(
                    "nodeId", nodeId,
                    "status", status.name(),
                    "startedAt", startTime.toString(),
                    "finishedAt", Instant.now().toString(),
                    "output", output != null ? output : Map.of(),
                    "error", error != null ? error : ""));
        }

        public List<Map<String, Object>> getNodeExecutions() {
            return nodeExecutions;
        }
    }
}
