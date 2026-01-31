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
                case VARIABLE -> handleVariable(entry, consoleService);
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

    private void handleVariable(LogEntry entry, ExecutionConsoleService service) {
        Map<String, Object> context = entry.context();
        String operation = getString(context, "operation", "unknown");
        String variableName = getString(context, "variableName", "unknown");
        String valuePreview = getString(context, "valuePreview", "");
        String valueType = getString(context, "valueType", "unknown");

        String message = operation + " variable: " + variableName;
        String details = "Type: " + valueType + (valuePreview.isEmpty() ? "" : " | Value: " + valuePreview);

        service.debug(entry.executionId(), message, details);
    }

    private void handleGeneric(LogEntry entry, ExecutionConsoleService service) {
        String details = formatContextDetails(entry.context());

        switch (entry.level()) {
            case ERROR, FATAL -> service.error(entry.executionId(), "system", entry.message(), details);
            case WARN -> service.info(entry.executionId(), "⚠️ " + entry.message(), details);
            case DEBUG -> service.debug(entry.executionId(), entry.message(), details);
            case TRACE -> service.debug(entry.executionId(), entry.message(), details);
            default -> service.info(entry.executionId(), entry.message(), details);
        }
    }

    /**
     * Format context map into readable details string.
     */
    private String formatContextDetails(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (Map.Entry<String, Object> entry : context.entrySet()) {
            if (!first) {
                sb.append("\n");
            }
            sb.append("• ").append(entry.getKey()).append(": ");

            Object value = entry.getValue();
            if (value == null) {
                sb.append("null");
            } else if (value instanceof String str) {
                // For strings, show quotes if they contain spaces or special chars
                if (str.contains(" ") || str.contains("\n") || str.contains("\t")) {
                    sb.append("'").append(str.replace("\n", "\\n").replace("\t", "\\t")).append("'");
                } else {
                    sb.append(str);
                }
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value.toString());
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) value;
                sb.append("{");
                boolean firstInner = true;
                for (Map.Entry<String, Object> innerEntry : map.entrySet()) {
                    if (!firstInner) sb.append(", ");
                    sb.append(innerEntry.getKey()).append(": ").append(innerEntry.getValue());
                    firstInner = false;
                }
                sb.append("}");
            } else if (value instanceof Iterable) {
                sb.append("[");
                boolean firstInner = true;
                for (Object item : (Iterable<?>) value) {
                    if (!firstInner) sb.append(", ");
                    sb.append(item != null ? item.toString() : "null");
                    firstInner = false;
                }
                sb.append("]");
            } else {
                sb.append(value.toString());
            }

            first = false;
        }

        return sb.toString();
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
