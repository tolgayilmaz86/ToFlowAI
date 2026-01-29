package io.toflowai.app.executor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import io.toflowai.app.service.ExecutionService;
import io.toflowai.app.service.NodeExecutor;
import io.toflowai.app.service.WorkflowService;
import io.toflowai.common.domain.Node;
import io.toflowai.common.dto.ExecutionDTO;
import io.toflowai.common.dto.WorkflowDTO;
import io.toflowai.common.enums.ExecutionStatus;

/**
 * Executor for subworkflow nodes.
 * Executes another workflow as a nested operation.
 * 
 * Configuration:
 * - workflowId: ID of the workflow to execute
 * - workflowName: Name of the workflow (for display)
 * - inputMapping: Map of subworkflow input names to expressions
 * - outputMapping: Map of output names to subworkflow output paths
 * - waitForCompletion: Whether to wait for subworkflow to complete (default:
 * true)
 * - timeout: Maximum execution time in milliseconds (default: 300000 - 5
 * minutes)
 */
@Component
public class SubworkflowExecutor implements NodeExecutor {

    private final WorkflowService workflowService;
    private final ApplicationContext applicationContext;

    // Use lazy loading to avoid circular dependency
    private ExecutionService executionService;

    public SubworkflowExecutor(WorkflowService workflowService, ApplicationContext applicationContext) {
        this.workflowService = workflowService;
        this.applicationContext = applicationContext;
    }

    private ExecutionService getExecutionService() {
        if (executionService == null) {
            executionService = applicationContext.getBean(ExecutionService.class);
        }
        return executionService;
    }

    @Override
    public String getNodeType() {
        return "subworkflow";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> config = node.parameters();

        // Get workflow to execute
        Long workflowId = getWorkflowId(config);
        if (workflowId == null) {
            throw new IllegalArgumentException("Subworkflow node requires 'workflowId' configuration");
        }

        // Prevent infinite recursion - check if we're already executing this workflow
        Long currentWorkflowId = context.getWorkflow().id();
        if (workflowId.equals(currentWorkflowId)) {
            throw new IllegalStateException("Subworkflow cannot call itself (recursive execution detected)");
        }

        // Verify workflow exists
        Optional<WorkflowDTO> subworkflow = workflowService.findById(workflowId);
        if (subworkflow.isEmpty()) {
            throw new IllegalArgumentException("Subworkflow not found: " + workflowId);
        }

        // Build subworkflow input from input mapping
        Map<String, Object> subworkflowInput = buildSubworkflowInput(config, input);

        // Check if we should wait for completion
        boolean waitForCompletion = getBooleanConfig(config, "waitForCompletion", true);
        long timeout = getLongConfig(config, "timeout", 300000L); // 5 minutes default

        Map<String, Object> result = new HashMap<>();

        if (waitForCompletion) {
            // Execute synchronously and wait for result
            ExecutionDTO execution = getExecutionService().execute(workflowId, subworkflowInput);

            result.put("executionId", execution.id());
            result.put("status", execution.status().name());
            result.put("success", execution.status() == ExecutionStatus.SUCCESS);

            if (execution.status() == ExecutionStatus.SUCCESS) {
                // Apply output mapping
                Map<String, Object> subOutput = execution.outputData();
                result.put("output", applyOutputMapping(config, subOutput));
                result.put("rawOutput", subOutput);
            } else {
                result.put("error", execution.errorMessage());
                result.put("output", Map.of());
            }

            result.put("startedAt", execution.startedAt() != null ? execution.startedAt().toString() : null);
            result.put("finishedAt", execution.finishedAt() != null ? execution.finishedAt().toString() : null);
            result.put("durationMs", execution.durationMs());
        } else {
            // Execute asynchronously
            var future = getExecutionService().executeAsync(workflowId, subworkflowInput);

            result.put("async", true);
            result.put("message", "Subworkflow started asynchronously");
            result.put("workflowId", workflowId);
            result.put("workflowName", subworkflow.get().name());

            // We don't wait for the result, but we store a reference
            // The caller can check execution history later
        }

        return result;
    }

    private Long getWorkflowId(Map<String, Object> config) {
        Object idObj = config.get("workflowId");
        if (idObj == null) {
            // Try getting by name
            String name = (String) config.get("workflowName");
            if (name != null) {
                return workflowService.findByName(name)
                        .map(WorkflowDTO::id)
                        .orElse(null);
            }
            return null;
        }
        if (idObj instanceof Number num) {
            return num.longValue();
        }
        if (idObj instanceof String str) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException e) {
                // Try as workflow name
                return workflowService.findByName(str)
                        .map(WorkflowDTO::id)
                        .orElse(null);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildSubworkflowInput(Map<String, Object> config, Map<String, Object> parentInput) {
        Map<String, Object> inputMapping = (Map<String, Object>) config.get("inputMapping");
        if (inputMapping == null || inputMapping.isEmpty()) {
            // Pass through all parent input
            return new HashMap<>(parentInput);
        }

        Map<String, Object> subInput = new HashMap<>();
        for (Map.Entry<String, Object> entry : inputMapping.entrySet()) {
            String targetKey = entry.getKey();
            Object sourceValue = entry.getValue();

            if (sourceValue instanceof String expr) {
                // Simple expression: direct key reference
                if (expr.startsWith("$.")) {
                    String path = expr.substring(2);
                    subInput.put(targetKey, getNestedValue(parentInput, path));
                } else {
                    subInput.put(targetKey, parentInput.getOrDefault(expr, expr));
                }
            } else {
                // Direct value
                subInput.put(targetKey, sourceValue);
            }
        }

        return subInput;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> applyOutputMapping(Map<String, Object> config, Map<String, Object> subOutput) {
        Map<String, Object> outputMapping = (Map<String, Object>) config.get("outputMapping");
        if (outputMapping == null || outputMapping.isEmpty()) {
            // Return all subworkflow output
            return subOutput != null ? new HashMap<>(subOutput) : Map.of();
        }

        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : outputMapping.entrySet()) {
            String targetKey = entry.getKey();
            Object sourceExpr = entry.getValue();

            if (sourceExpr instanceof String path) {
                if (path.startsWith("$.")) {
                    result.put(targetKey, getNestedValue(subOutput, path.substring(2)));
                } else {
                    result.put(targetKey, subOutput != null ? subOutput.get(path) : null);
                }
            } else {
                result.put(targetKey, sourceExpr);
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private Object getNestedValue(Map<String, Object> map, String path) {
        if (map == null || path == null || path.isEmpty()) {
            return null;
        }

        String[] parts = path.split("\\.");
        Object current = map;

        for (String part : parts) {
            if (current instanceof Map<?, ?> m) {
                current = m.get(part);
            } else {
                return null;
            }
        }

        return current;
    }

    private boolean getBooleanConfig(Map<String, Object> config, String key, boolean defaultValue) {
        Object value = config.get(key);
        if (value == null)
            return defaultValue;
        if (value instanceof Boolean b)
            return b;
        if (value instanceof String s)
            return Boolean.parseBoolean(s);
        return defaultValue;
    }

    private long getLongConfig(Map<String, Object> config, String key, long defaultValue) {
        Object value = config.get(key);
        if (value == null)
            return defaultValue;
        if (value instanceof Number n)
            return n.longValue();
        if (value instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
