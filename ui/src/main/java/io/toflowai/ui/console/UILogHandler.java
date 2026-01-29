package io.toflowai.ui.console;

import java.util.Map;

import org.springframework.stereotype.Component;

import io.toflowai.common.service.ExecutionLogHandler;
import javafx.application.Platform;

/**
 * Log handler that forwards execution logs to the UI ExecutionConsole.
 * Bridges the backend ExecutionLogger to the UI for real-time display.
 */
@Component
public class UILogHandler implements ExecutionLogHandler {

    @Override
    public void handle(LogEntry entry) {
        ExecutionConsoleService consoleService = ExecutionConsoleService.getInstance();

        // Ensure UI updates happen on the JavaFX thread
        Platform.runLater(() -> {
            switch (entry.category()) {
                case EXECUTION_START -> handleExecutionStart(entry, consoleService);
                case EXECUTION_END -> handleExecutionEnd(entry, consoleService);
                case NODE_START -> handleNodeStart(entry, consoleService);
                case NODE_END -> handleNodeEnd(entry, consoleService);
                case NODE_SKIP -> handleNodeSkip(entry, consoleService);
                case ERROR -> handleError(entry, consoleService);
                case RETRY -> handleRetry(entry, consoleService);
                case RATE_LIMIT -> handleRateLimit(entry, consoleService);
                case DATA_FLOW -> handleDataFlow(entry, consoleService);
                default -> handleGeneric(entry, consoleService);
            }
        });
    }

    private void handleExecutionStart(LogEntry entry, ExecutionConsoleService service) {
        Map<String, Object> context = entry.context();
        String workflowId = getString(context, "workflowId", "unknown");
        String workflowName = getString(context, "workflowName", "Unknown Workflow");

        service.executionStart(entry.executionId(), workflowId, workflowName);
    }

    private void handleExecutionEnd(LogEntry entry, ExecutionConsoleService service) {
        Map<String, Object> context = entry.context();
        boolean success = getBoolean(context, "success", false);
        long durationMs = getLong(context, "duration_ms", 0);

        service.executionEnd(entry.executionId(), success, durationMs);
    }

    private void handleNodeStart(LogEntry entry, ExecutionConsoleService service) {
        Map<String, Object> context = entry.context();
        String nodeId = getString(context, "nodeId", "unknown");
        String nodeName = getString(context, "nodeName", "Unknown Node");
        String nodeType = getString(context, "nodeType", "unknown");

        service.nodeStart(entry.executionId(), nodeId, nodeName, nodeType);
    }

    private void handleNodeEnd(LogEntry entry, ExecutionConsoleService service) {
        Map<String, Object> context = entry.context();
        String nodeId = getString(context, "nodeId", "unknown");
        String nodeName = getString(context, "nodeName", nodeId);
        boolean success = getBoolean(context, "success", true);
        long durationMs = getLong(context, "durationMs", 0);

        service.nodeEnd(entry.executionId(), nodeId, nodeName, success, durationMs);
    }

    private void handleNodeSkip(LogEntry entry, ExecutionConsoleService service) {
        Map<String, Object> context = entry.context();
        String nodeId = getString(context, "nodeId", "unknown");
        String reason = getString(context, "reason", "Unknown reason");

        service.nodeSkip(entry.executionId(), nodeId, nodeId, reason);
    }

    private void handleError(LogEntry entry, ExecutionConsoleService service) {
        Map<String, Object> context = entry.context();
        String nodeId = getString(context, "nodeId", getString(context, "source", "workflow"));
        String message = getString(context, "errorMessage", entry.message());
        String stackTrace = getString(context, "stackTrace", null);

        service.error(entry.executionId(), nodeId, message, stackTrace);
    }

    private void handleRetry(LogEntry entry, ExecutionConsoleService service) {
        Map<String, Object> context = entry.context();
        String nodeId = getString(context, "nodeId", "unknown");
        int attempt = getInt(context, "attempt", 1);
        int maxRetries = getInt(context, "maxRetries", 3);
        long delayMs = getLong(context, "delayMs", 0);

        service.retry(entry.executionId(), nodeId, attempt, maxRetries, delayMs);
    }

    private void handleRateLimit(LogEntry entry, ExecutionConsoleService service) {
        Map<String, Object> context = entry.context();
        String bucketId = getString(context, "bucketId", "default");
        boolean throttled = getBoolean(context, "throttled", false);
        long waitMs = getLong(context, "waitMs", 0);

        service.rateLimit(entry.executionId(), bucketId, throttled, waitMs);
    }

    private void handleDataFlow(LogEntry entry, ExecutionConsoleService service) {
        Map<String, Object> context = entry.context();
        String fromNode = getString(context, "fromNode", "unknown");
        String toNode = getString(context, "toNode", "unknown");
        int dataSize = getInt(context, "dataSize", 0);

        service.dataFlow(entry.executionId(), fromNode, toNode, dataSize);
    }

    private void handleGeneric(LogEntry entry, ExecutionConsoleService service) {
        String details = entry.context() != null ? entry.context().toString() : null;

        switch (entry.level()) {
            case ERROR, FATAL -> service.error(entry.executionId(), "system", entry.message(), details);
            case WARN -> service.info(entry.executionId(), "⚠️ " + entry.message(), details);
            case DEBUG -> service.debug(entry.executionId(), entry.message(), details);
            case TRACE -> service.debug(entry.executionId(), entry.message(), details);
            default -> service.info(entry.executionId(), entry.message(), details);
        }
    }

    // Helper methods for extracting values from context map
    private String getString(Map<String, Object> context, String key, String defaultValue) {
        if (context == null)
            return defaultValue;
        Object value = context.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private boolean getBoolean(Map<String, Object> context, String key, boolean defaultValue) {
        if (context == null)
            return defaultValue;
        Object value = context.get(key);
        if (value instanceof Boolean b)
            return b;
        if (value != null)
            return Boolean.parseBoolean(value.toString());
        return defaultValue;
    }

    private int getInt(Map<String, Object> context, String key, int defaultValue) {
        if (context == null)
            return defaultValue;
        Object value = context.get(key);
        if (value instanceof Number n)
            return n.intValue();
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private long getLong(Map<String, Object> context, String key, long defaultValue) {
        if (context == null)
            return defaultValue;
        Object value = context.get(key);
        if (value instanceof Number n)
            return n.longValue();
        if (value != null) {
            try {
                return Long.parseLong(value.toString());
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
