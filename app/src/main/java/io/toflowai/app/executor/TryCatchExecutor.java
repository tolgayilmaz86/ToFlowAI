package io.toflowai.app.executor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import io.toflowai.app.service.ExecutionService;
import io.toflowai.app.service.NodeExecutor;
import io.toflowai.app.service.NodeExecutorRegistry;
import io.toflowai.common.domain.Node;

/**
 * Executor for try/catch error handling.
 * Wraps operations in a try block and handles errors gracefully.
 * 
 * Configuration:
 * - tryOperations: List of operations to execute in try block
 * - catchOperations: List of operations to execute if error occurs (optional)
 * - finallyOperations: List of operations to always execute (optional)
 * - errorVariable: Name of variable to store error info (default: "error")
 * - continueOnError: Whether to continue workflow on error (default: true)
 * - logErrors: Whether to log errors (default: true)
 * 
 * Each operation has:
 * - type: Node type to execute
 * - name: Display name (optional)
 * - config: Configuration for that node type
 */
@Component
public class TryCatchExecutor implements NodeExecutor {

    private final NodeExecutorRegistry nodeExecutorRegistry;

    public TryCatchExecutor(@Lazy NodeExecutorRegistry nodeExecutorRegistry) {
        this.nodeExecutorRegistry = nodeExecutorRegistry;
    }

    @Override
    public String getNodeType() {
        return "tryCatch";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> config = node.parameters();

        List<Map<String, Object>> tryOperations = (List<Map<String, Object>>) config.get("tryOperations");
        List<Map<String, Object>> catchOperations = (List<Map<String, Object>>) config.get("catchOperations");
        List<Map<String, Object>> finallyOperations = (List<Map<String, Object>>) config.get("finallyOperations");

        String errorVariable = getStringConfig(config, "errorVariable", "error");
        boolean continueOnError = getBooleanConfig(config, "continueOnError", true);
        boolean logErrors = getBooleanConfig(config, "logErrors", true);

        Map<String, Object> result = new HashMap<>(input);
        Map<String, Object> errorInfo = null;
        boolean success = true;

        // Try block
        try {
            if (tryOperations != null && !tryOperations.isEmpty()) {
                result = executeOperations(tryOperations, result, context);
            }
        } catch (Exception e) {
            success = false;
            errorInfo = captureError(e);

            if (logErrors) {
                System.err.println("[TryCatch] Error in try block: " + e.getMessage());
            }

            // Store error in result
            result.put(errorVariable, errorInfo);

            // Catch block
            if (catchOperations != null && !catchOperations.isEmpty()) {
                try {
                    // Pass error info to catch operations
                    Map<String, Object> catchInput = new HashMap<>(result);
                    catchInput.put("_caughtError", errorInfo);
                    result = executeOperations(catchOperations, catchInput, context);
                } catch (Exception catchError) {
                    // Error in catch block
                    Map<String, Object> catchErrorInfo = captureError(catchError);
                    result.put("catchError", catchErrorInfo);

                    if (logErrors) {
                        System.err.println("[TryCatch] Error in catch block: " + catchError.getMessage());
                    }

                    if (!continueOnError) {
                        throw new RuntimeException("Error in catch block", catchError);
                    }
                }
            } else if (!continueOnError) {
                throw new RuntimeException("Error in try block", e);
            }
        }

        // Finally block - always executes
        if (finallyOperations != null && !finallyOperations.isEmpty()) {
            try {
                Map<String, Object> finallyInput = new HashMap<>(result);
                finallyInput.put("_success", success);
                finallyInput.put("_hadError", !success);
                result = executeOperations(finallyOperations, finallyInput, context);
            } catch (Exception finallyError) {
                Map<String, Object> finallyErrorInfo = captureError(finallyError);
                result.put("finallyError", finallyErrorInfo);

                if (logErrors) {
                    System.err.println("[TryCatch] Error in finally block: " + finallyError.getMessage());
                }

                if (!continueOnError) {
                    throw new RuntimeException("Error in finally block", finallyError);
                }
            }
        }

        // Add metadata
        result.put("_tryCatchSuccess", success);
        result.put("_tryCatchExecuted", true);

        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> executeOperations(
            List<Map<String, Object>> operations,
            Map<String, Object> input,
            ExecutionService.ExecutionContext context) {

        Map<String, Object> currentData = new HashMap<>(input);

        for (Map<String, Object> operation : operations) {
            String type = (String) operation.get("type");
            if (type == null)
                continue;

            Map<String, Object> opConfig = (Map<String, Object>) operation.getOrDefault("config", Map.of());
            String name = (String) operation.getOrDefault("name", type);

            // Create a temporary node for this operation
            Node tempNode = new Node(
                    "trycatch_" + UUID.randomUUID().toString().substring(0, 8),
                    type,
                    name,
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

    private Map<String, Object> captureError(Exception e) {
        Map<String, Object> errorInfo = new HashMap<>();
        errorInfo.put("message", e.getMessage());
        errorInfo.put("type", e.getClass().getSimpleName());
        errorInfo.put("fullType", e.getClass().getName());
        errorInfo.put("timestamp", java.time.Instant.now().toString());

        // Capture stack trace
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        errorInfo.put("stackTrace", sw.toString());

        // Capture cause if present
        if (e.getCause() != null) {
            Map<String, Object> causeInfo = new HashMap<>();
            causeInfo.put("message", e.getCause().getMessage());
            causeInfo.put("type", e.getCause().getClass().getSimpleName());
            errorInfo.put("cause", causeInfo);
        }

        return errorInfo;
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
