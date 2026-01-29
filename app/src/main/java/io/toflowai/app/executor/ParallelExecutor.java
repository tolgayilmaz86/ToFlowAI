package io.toflowai.app.executor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import io.toflowai.app.service.ExecutionService;
import io.toflowai.app.service.NodeExecutor;
import io.toflowai.app.service.NodeExecutorRegistry;
import io.toflowai.common.domain.Node;

/**
 * Executor for parallel execution nodes.
 * Executes multiple operations concurrently using virtual threads.
 * 
 * Configuration:
 * - branches: List of branch configurations, each with:
 * - name: Branch identifier
 * - operations: List of operations to execute in this branch
 * - combineResults: How to combine branch results ("merge", "array", "first")
 * - timeout: Maximum execution time in milliseconds (default: 60000)
 * - failFast: If true, cancel other branches on first failure (default: false)
 * 
 * Each operation has:
 * - type: Node type to execute
 * - config: Configuration for that node type
 */
@Component
public class ParallelExecutor implements NodeExecutor {

    private final NodeExecutorRegistry nodeExecutorRegistry;

    public ParallelExecutor(@Lazy NodeExecutorRegistry nodeExecutorRegistry) {
        this.nodeExecutorRegistry = nodeExecutorRegistry;
    }

    @Override
    public String getNodeType() {
        return "parallel";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> config = node.parameters();

        List<Map<String, Object>> branches = (List<Map<String, Object>>) config.get("branches");
        if (branches == null || branches.isEmpty()) {
            return Map.of("error", "No branches configured", "input", input);
        }

        long timeout = getLongConfig(config, "timeout", 60000L);
        boolean failFast = getBooleanConfig(config, "failFast", false);
        String combineResults = getStringConfig(config, "combineResults", "merge");

        Map<String, Object> results = new ConcurrentHashMap<>();
        Map<String, String> errors = new ConcurrentHashMap<>();
        List<String> completedBranches = new CopyOnWriteArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();

            for (Map<String, Object> branch : branches) {
                String branchName = (String) branch.getOrDefault("name", "branch_" + futures.size());

                futures.add(executor.submit(() -> {
                    try {
                        Map<String, Object> branchResult = executeBranch(branch, input, context);
                        results.put(branchName, branchResult);
                        completedBranches.add(branchName);
                    } catch (Exception e) {
                        errors.put(branchName, e.getMessage());
                        if (failFast) {
                            throw new RuntimeException("Branch failed: " + branchName, e);
                        }
                    }
                }));
            }

            // Wait for all branches to complete
            try {
                for (Future<?> future : futures) {
                    future.get(timeout, TimeUnit.MILLISECONDS);
                }
            } catch (TimeoutException e) {
                // Cancel remaining tasks
                for (Future<?> future : futures) {
                    future.cancel(true);
                }
                errors.put("_timeout", "Execution timed out after " + timeout + "ms");
            } catch (ExecutionException e) {
                if (failFast) {
                    // Cancel remaining tasks
                    for (Future<?> future : futures) {
                        future.cancel(true);
                    }
                }
            }
        } catch (Exception e) {
            errors.put("_executor", e.getMessage());
        }

        // Combine results based on strategy
        Map<String, Object> output = new HashMap<>();
        output.put("completedBranches", completedBranches);
        output.put("branchCount", branches.size());
        output.put("successCount", completedBranches.size());
        output.put("hasErrors", !errors.isEmpty());

        if (!errors.isEmpty()) {
            output.put("errors", errors);
        }

        switch (combineResults) {
            case "merge" -> {
                // Merge all branch results into a single map
                Map<String, Object> merged = new HashMap<>(input);
                for (Map.Entry<String, Object> entry : results.entrySet()) {
                    if (entry.getValue() instanceof Map<?, ?> m) {
                        merged.putAll((Map<String, Object>) m);
                    } else {
                        merged.put(entry.getKey(), entry.getValue());
                    }
                }
                output.put("result", merged);
            }
            case "array" -> {
                // Return results as an array
                output.put("result", new ArrayList<>(results.values()));
            }
            case "first" -> {
                // Return first completed result
                if (!results.isEmpty()) {
                    output.put("result", results.values().iterator().next());
                }
            }
            default -> output.put("branches", results);
        }

        return output;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> executeBranch(
            Map<String, Object> branch,
            Map<String, Object> input,
            ExecutionService.ExecutionContext context) {

        List<Map<String, Object>> operations = (List<Map<String, Object>>) branch.get("operations");
        if (operations == null || operations.isEmpty()) {
            return input;
        }

        Map<String, Object> currentData = new HashMap<>(input);

        for (Map<String, Object> operation : operations) {
            String type = (String) operation.get("type");
            if (type == null)
                continue;

            Map<String, Object> opConfig = (Map<String, Object>) operation.getOrDefault("config", Map.of());

            // Create a temporary node for this operation
            Node tempNode = new Node(
                    "parallel_" + UUID.randomUUID().toString().substring(0, 8),
                    type,
                    (String) operation.getOrDefault("name", type),
                    new Node.Position(0, 0),
                    opConfig,
                    null,
                    false,
                    null);

            NodeExecutor executor = nodeExecutorRegistry.getExecutor(type);
            currentData = executor.execute(tempNode, currentData, context);
        }

        return currentData;
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

    private String getStringConfig(Map<String, Object> config, String key, String defaultValue) {
        Object value = config.get(key);
        if (value == null)
            return defaultValue;
        return value.toString();
    }
}
