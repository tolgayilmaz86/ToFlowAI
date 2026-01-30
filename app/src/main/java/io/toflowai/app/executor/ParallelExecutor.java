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

        // Validate and parse configuration
        ExecutionConfig execConfig = parseExecutionConfig(config);
        if (execConfig.hasValidationError()) {
            return Map.of("error", execConfig.getValidationError(), "input", input);
        }

        // Execute branches in parallel
        ExecutionResult execResult = executeBranchesInParallel(execConfig, input, context);

        // Combine and return results
        return buildFinalOutput(execConfig, execResult, input);
    }

    /**
     * Configuration holder for parallel execution.
     */
    private static class ExecutionConfig {
        private final List<Map<String, Object>> branches;
        private final long timeout;
        private final boolean failFast;
        private final String combineResults;
        private final String validationError;

        public ExecutionConfig(List<Map<String, Object>> branches, long timeout,
                boolean failFast, String combineResults, String validationError) {
            this.branches = branches;
            this.timeout = timeout;
            this.failFast = failFast;
            this.combineResults = combineResults;
            this.validationError = validationError;
        }

        public List<Map<String, Object>> getBranches() { return branches; }
        public long getTimeout() { return timeout; }
        public boolean isFailFast() { return failFast; }
        public String getCombineResults() { return combineResults; }
        public boolean hasValidationError() { return validationError != null; }
        public String getValidationError() { return validationError; }
    }

    /**
     * Result holder for parallel execution.
     */
    private static class ExecutionResult {
        private final Map<String, Object> results;
        private final Map<String, String> errors;
        private final List<String> completedBranches;

        public ExecutionResult(Map<String, Object> results, Map<String, String> errors,
                List<String> completedBranches) {
            this.results = results;
            this.errors = errors;
            this.completedBranches = completedBranches;
        }

        public Map<String, Object> getResults() { return results; }
        public Map<String, String> getErrors() { return errors; }
        public List<String> getCompletedBranches() { return completedBranches; }
    }

    /**
     * Parses and validates execution configuration.
     */
    private ExecutionConfig parseExecutionConfig(Map<String, Object> config) {
        List<Map<String, Object>> branches = (List<Map<String, Object>>) config.get("branches");
        if (branches == null || branches.isEmpty()) {
            return new ExecutionConfig(null, 0, false, null, "No branches configured");
        }

        long timeout = getLongConfig(config, "timeout", 60000L);
        boolean failFast = getBooleanConfig(config, "failFast", false);
        String combineResults = getStringConfig(config, "combineResults", "merge");

        return new ExecutionConfig(branches, timeout, failFast, combineResults, null);
    }

    /**
     * Executes all branches in parallel using virtual threads.
     */
    private ExecutionResult executeBranchesInParallel(ExecutionConfig config,
            Map<String, Object> input, ExecutionService.ExecutionContext context) {

        Map<String, Object> results = new ConcurrentHashMap<>();
        Map<String, String> errors = new ConcurrentHashMap<>();
        List<String> completedBranches = new CopyOnWriteArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = submitBranchTasks(config, input, context, results, errors, completedBranches, executor);

            waitForCompletionWithTimeout(futures, config.getTimeout(), config.isFailFast(), errors);

        } catch (Exception e) {
            errors.put("_executor", e.getMessage());
        }

        return new ExecutionResult(results, errors, completedBranches);
    }

    /**
     * Submits branch execution tasks to the executor.
     */
    private List<Future<?>> submitBranchTasks(ExecutionConfig config, Map<String, Object> input,
            ExecutionService.ExecutionContext context, Map<String, Object> results,
            Map<String, String> errors, List<String> completedBranches, ExecutorService executor) {

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < config.getBranches().size(); i++) {
            Map<String, Object> branch = config.getBranches().get(i);
            String branchName = (String) branch.getOrDefault("name", "branch_" + i);

            futures.add(executor.submit(() ->
                executeSingleBranch(branch, branchName, input, context, results, errors, completedBranches, config.isFailFast())));
        }

        return futures;
    }

    /**
     * Executes a single branch and handles errors.
     */
    private void executeSingleBranch(Map<String, Object> branch, String branchName,
            Map<String, Object> input, ExecutionService.ExecutionContext context,
            Map<String, Object> results, Map<String, String> errors,
            List<String> completedBranches, boolean failFast) {

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
    }

    /**
     * Waits for all futures to complete with timeout handling.
     */
    private void waitForCompletionWithTimeout(List<Future<?>> futures, long timeout,
            boolean failFast, Map<String, String> errors) {

        try {
            for (Future<?> future : futures) {
                future.get(timeout, TimeUnit.MILLISECONDS);
            }
        } catch (TimeoutException e) {
            cancelAllFutures(futures);
            errors.put("_timeout", "Execution timed out after " + timeout + "ms");
        } catch (ExecutionException e) {
            if (failFast) {
                cancelAllFutures(futures);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cancelAllFutures(futures);
        }
    }

    /**
     * Cancels all pending futures.
     */
    private void cancelAllFutures(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            future.cancel(true);
        }
    }

    /**
     * Builds the final output based on execution results.
     */
    private Map<String, Object> buildFinalOutput(ExecutionConfig config,
            ExecutionResult result, Map<String, Object> input) {

        Map<String, Object> output = new HashMap<>();
        output.put("completedBranches", result.getCompletedBranches());
        output.put("branchCount", config.getBranches().size());
        output.put("successCount", result.getCompletedBranches().size());
        output.put("hasErrors", !result.getErrors().isEmpty());

        if (!result.getErrors().isEmpty()) {
            output.put("errors", result.getErrors());
        }

        combineResultsBasedOnStrategy(config.getCombineResults(), result.getResults(), input, output);

        return output;
    }

    /**
     * Combines results based on the specified strategy.
     */
    private void combineResultsBasedOnStrategy(String strategy, Map<String, Object> results,
            Map<String, Object> input, Map<String, Object> output) {

        switch (strategy) {
            case "merge" -> output.put("result", mergeResults(results, input));
            case "array" -> output.put("result", new ArrayList<>(results.values()));
            case "first" -> {
                if (!results.isEmpty()) {
                    output.put("result", results.values().iterator().next());
                }
            }
            default -> output.put("branches", results);
        }
    }

    /**
     * Merges all branch results into a single map.
     */
    private Map<String, Object> mergeResults(Map<String, Object> results, Map<String, Object> input) {
        Map<String, Object> merged = new HashMap<>(input);
        for (Map.Entry<String, Object> entry : results.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> map) {
                merged.putAll((Map<String, Object>) map);
            } else {
                merged.put(entry.getKey(), entry.getValue());
            }
        }
        return merged;
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
