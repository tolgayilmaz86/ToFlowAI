package io.toflowai.app.service;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.toflowai.common.service.ExecutionLogHandler;

/**
 * Structured logging service for workflow execution.
 * Provides JSON-formatted log entries with context and timing.
 */
@Service
public class ExecutionLogger {

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, ExecutionLog> executionLogs = new ConcurrentHashMap<>();
    private final List<ExecutionLogHandler> logHandlers = new CopyOnWriteArrayList<>();

    public ExecutionLogger() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        this.objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    }

    /**
     * Log levels for execution logging.
     */
    public enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR, FATAL
    }

    /**
     * Categories for log entries.
     */
    public enum LogCategory {
        EXECUTION_START,
        EXECUTION_END,
        NODE_START,
        NODE_END,
        NODE_SKIP,
        NODE_INPUT, // New: logs node input data
        NODE_OUTPUT, // New: logs node output data
        DATA_FLOW,
        VARIABLE,
        EXPRESSION_EVAL, // New: logs expression evaluation
        ERROR,
        RETRY,
        RATE_LIMIT,
        PERFORMANCE,
        CUSTOM
    }

    /**
     * Start logging for an execution.
     */
    public void startExecution(String executionId, String workflowId, String workflowName) {
        ExecutionLog log = new ExecutionLog(executionId, workflowId, workflowName);
        executionLogs.put(executionId, log);

        logEntry(executionId, LogLevel.INFO, LogCategory.EXECUTION_START,
                "Workflow execution started",
                Map.of("workflowId", workflowId, "workflowName", workflowName));
    }

    /**
     * End logging for an execution.
     */
    public void endExecution(String executionId, boolean success, Object result) {
        ExecutionLog log = executionLogs.get(executionId);
        if (log != null) {
            log.setEndTime(Instant.now());
            log.setSuccess(success);
        }

        logEntry(executionId, success ? LogLevel.INFO : LogLevel.ERROR,
                LogCategory.EXECUTION_END,
                success ? "Workflow execution completed" : "Workflow execution failed",
                Map.of("success", success,
                        "duration_ms", log != null ? log.getDurationMs() : 0,
                        "nodeCount", log != null ? log.getEntries().size() : 0));
    }

    /**
     * Log node execution start.
     */
    public void nodeStart(String executionId, String nodeId, String nodeType, String nodeName) {
        logEntry(executionId, LogLevel.DEBUG, LogCategory.NODE_START,
                "Node execution started",
                Map.of("nodeId", nodeId, "nodeType", nodeType, "nodeName", nodeName));
    }

    /**
     * Log node execution end.
     */
    public void nodeEnd(String executionId, String nodeId, String nodeType, long durationMs, boolean success) {
        logEntry(executionId, success ? LogLevel.DEBUG : LogLevel.ERROR,
                LogCategory.NODE_END,
                success ? "Node execution completed" : "Node execution failed",
                Map.of("nodeId", nodeId, "nodeType", nodeType,
                        "durationMs", durationMs, "success", success));
    }

    /**
     * Log node skip (conditional).
     */
    public void nodeSkip(String executionId, String nodeId, String reason) {
        logEntry(executionId, LogLevel.DEBUG, LogCategory.NODE_SKIP,
                "Node execution skipped",
                Map.of("nodeId", nodeId, "reason", reason));
    }

    /**
     * Log data flow between nodes.
     */
    public void dataFlow(String executionId, String fromNode, String toNode, int dataSize) {
        logEntry(executionId, LogLevel.TRACE, LogCategory.DATA_FLOW,
                "Data passed between nodes",
                Map.of("fromNode", fromNode, "toNode", toNode, "dataSize", dataSize));
    }

    /**
     * Log variable operation.
     */
    public void variable(String executionId, String operation, String variableName, Object value) {
        Map<String, Object> context = new HashMap<>();
        context.put("operation", operation);
        context.put("variableName", variableName);
        if (value != null) {
            context.put("valueType", value.getClass().getSimpleName());
            context.put("valuePreview", truncate(String.valueOf(value), 100));
        }

        logEntry(executionId, LogLevel.TRACE, LogCategory.VARIABLE,
                "Variable " + operation,
                context);
    }

    /**
     * Log node input data for traceability.
     */
    public void nodeInput(String executionId, String nodeId, String nodeName, Map<String, Object> input) {
        Map<String, Object> context = new HashMap<>();
        context.put("nodeId", nodeId);
        context.put("nodeName", nodeName);
        context.put("inputKeys", input != null ? input.keySet() : List.of());
        context.put("inputSize", input != null ? input.size() : 0);

        // Add truncated preview of each input value
        if (input != null) {
            Map<String, String> inputPreview = new HashMap<>();
            for (Map.Entry<String, Object> entry : input.entrySet()) {
                inputPreview.put(entry.getKey(), formatValuePreview(entry.getValue()));
            }
            context.put("inputPreview", inputPreview);
        }

        logEntry(executionId, LogLevel.DEBUG, LogCategory.NODE_INPUT,
                "Node '" + nodeName + "' input data",
                context);
    }

    /**
     * Log node output data for traceability.
     */
    public void nodeOutput(String executionId, String nodeId, String nodeName, Map<String, Object> output) {
        Map<String, Object> context = new HashMap<>();
        context.put("nodeId", nodeId);
        context.put("nodeName", nodeName);
        context.put("outputKeys", output != null ? output.keySet() : List.of());
        context.put("outputSize", output != null ? output.size() : 0);

        // Add truncated preview of each output value
        if (output != null) {
            Map<String, String> outputPreview = new HashMap<>();
            for (Map.Entry<String, Object> entry : output.entrySet()) {
                outputPreview.put(entry.getKey(), formatValuePreview(entry.getValue()));
            }
            context.put("outputPreview", outputPreview);
        }

        logEntry(executionId, LogLevel.DEBUG, LogCategory.NODE_OUTPUT,
                "Node '" + nodeName + "' output data",
                context);
    }

    /**
     * Log expression evaluation for debugging templating and interpolation.
     */
    public void expressionEval(String executionId, String nodeId, String expression,
            Object result, boolean success) {
        Map<String, Object> context = new HashMap<>();
        context.put("nodeId", nodeId);
        context.put("expression", truncate(expression, 200));
        context.put("resultPreview", formatValuePreview(result));
        context.put("success", success);

        logEntry(executionId, LogLevel.TRACE, LogCategory.EXPRESSION_EVAL,
                success ? "Expression evaluated" : "Expression evaluation failed",
                context);
    }

    /**
     * Log error with full details including input context and source location.
     */
    public void error(String executionId, String nodeId, Exception e) {
        errorWithContext(executionId, nodeId, null, null, e);
    }

    /**
     * Log error with full details including input context and source location.
     * Enhanced version that captures the input data at the time of error.
     */
    public void errorWithContext(String executionId, String nodeId, String nodeName,
            Map<String, Object> inputAtError, Exception e) {
        Map<String, Object> context = new HashMap<>();
        context.put("nodeId", nodeId);
        if (nodeName != null) {
            context.put("nodeName", nodeName);
        }
        context.put("errorType", e.getClass().getName());
        context.put("errorMessage", e.getMessage());

        // Extract source location from stack trace
        StackTraceElement[] stackTrace = e.getStackTrace();
        if (stackTrace.length > 0) {
            StackTraceElement source = findRelevantStackFrame(stackTrace);
            context.put("sourceClass", source.getClassName());
            context.put("sourceMethod", source.getMethodName());
            context.put("sourceLine", source.getLineNumber());
            context.put("sourceLocation", source.getClassName() + "." + source.getMethodName()
                    + "(" + source.getFileName() + ":" + source.getLineNumber() + ")");
        }

        // Include input data preview if available
        if (inputAtError != null && !inputAtError.isEmpty()) {
            Map<String, String> inputPreview = new HashMap<>();
            for (Map.Entry<String, Object> entry : inputAtError.entrySet()) {
                inputPreview.put(entry.getKey(), formatValuePreview(entry.getValue()));
            }
            context.put("inputAtError", inputPreview);
        }

        // Get root cause if this is a wrapped exception
        Throwable rootCause = getRootCause(e);
        if (rootCause != e) {
            context.put("rootCauseType", rootCause.getClass().getName());
            context.put("rootCauseMessage", rootCause.getMessage());
        }

        context.put("stackTrace", getStackTrace(e));

        logEntry(executionId, LogLevel.ERROR, LogCategory.ERROR,
                "Error in node '" + (nodeName != null ? nodeName : nodeId) + "': " + e.getMessage(),
                context);
    }

    /**
     * Find the most relevant stack frame (first one in user code, not framework).
     */
    private StackTraceElement findRelevantStackFrame(StackTraceElement[] stackTrace) {
        for (StackTraceElement frame : stackTrace) {
            String className = frame.getClassName();
            // Look for frames in our codebase (io.toflowai)
            if (className.startsWith("io.toflowai") &&
                    !className.contains("$Proxy") &&
                    !className.contains("$$")) {
                return frame;
            }
        }
        // Fall back to first frame if no user code found
        return stackTrace[0];
    }

    /**
     * Get the root cause of an exception chain.
     */
    private Throwable getRootCause(Throwable t) {
        Throwable rootCause = t;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }

    /**
     * Format a value for preview in logs (truncated, with type info).
     */
    private String formatValuePreview(Object value) {
        if (value == null) {
            return "null";
        }

        String typeName = value.getClass().getSimpleName();
        String preview;

        if (value instanceof String s) {
            preview = truncate(s, 100);
        } else if (value instanceof Map<?, ?> map) {
            preview = "Map(" + map.size() + " entries)";
        } else if (value instanceof List<?> list) {
            preview = "List(" + list.size() + " items)";
        } else if (value instanceof Number || value instanceof Boolean) {
            preview = value.toString();
        } else {
            preview = truncate(value.toString(), 100);
        }

        return "[" + typeName + "] " + preview;
    }

    /**
     * Log retry attempt.
     */
    public void retry(String executionId, String nodeId, int attempt, int maxRetries, long delayMs, String reason) {
        logEntry(executionId, LogLevel.WARN, LogCategory.RETRY,
                "Retry attempt",
                Map.of("nodeId", nodeId, "attempt", attempt, "maxRetries", maxRetries,
                        "delayMs", delayMs, "reason", reason));
    }

    /**
     * Log rate limiting event.
     */
    public void rateLimit(String executionId, String bucketId, boolean throttled, long waitMs) {
        logEntry(executionId, throttled ? LogLevel.WARN : LogLevel.DEBUG,
                LogCategory.RATE_LIMIT,
                throttled ? "Rate limited" : "Rate limit passed",
                Map.of("bucketId", bucketId, "throttled", throttled, "waitMs", waitMs));
    }

    /**
     * Log performance metric.
     */
    public void performance(String executionId, String metric, long value, String unit) {
        logEntry(executionId, LogLevel.DEBUG, LogCategory.PERFORMANCE,
                "Performance metric",
                Map.of("metric", metric, "value", value, "unit", unit));
    }

    /**
     * Custom log entry.
     */
    public void custom(String executionId, LogLevel level, String message, Map<String, Object> context) {
        logEntry(executionId, level, LogCategory.CUSTOM, message, context);
    }

    /**
     * Create a log entry.
     */
    private void logEntry(String executionId, LogLevel level, LogCategory category,
            String message, Map<String, Object> context) {
        LogEntry entry = new LogEntry(
                UUID.randomUUID().toString(),
                executionId,
                Instant.now(),
                level,
                category,
                message,
                context != null ? new HashMap<>(context) : Map.of());

        // Add to execution log
        ExecutionLog log = executionLogs.get(executionId);
        if (log != null) {
            log.addEntry(entry);
        }

        // Convert to common interface type for handlers
        ExecutionLogHandler.LogEntry commonEntry = new ExecutionLogHandler.LogEntry(
                entry.id(),
                entry.executionId(),
                entry.timestamp(),
                ExecutionLogHandler.LogLevel.valueOf(entry.level().name()),
                ExecutionLogHandler.LogCategory.valueOf(entry.category().name()),
                entry.message(),
                entry.context());

        // Notify handlers
        for (ExecutionLogHandler handler : logHandlers) {
            try {
                handler.handle(commonEntry);
            } catch (Exception e) {
                // Don't let handler errors affect execution
            }
        }
    }

    /**
     * Get all log entries for an execution.
     */
    public List<LogEntry> getLogEntries(String executionId) {
        ExecutionLog log = executionLogs.get(executionId);
        return log != null ? log.getEntries() : List.of();
    }

    /**
     * Get filtered log entries.
     */
    public List<LogEntry> getLogEntries(String executionId, LogLevel minLevel, LogCategory category) {
        ExecutionLog log = executionLogs.get(executionId);
        if (log == null)
            return List.of();

        return log.getEntries().stream()
                .filter(e -> e.level().ordinal() >= minLevel.ordinal())
                .filter(e -> category == null || e.category() == category)
                .toList();
    }

    /**
     * Get execution summary.
     */
    public ExecutionSummary getSummary(String executionId) {
        ExecutionLog log = executionLogs.get(executionId);
        if (log == null)
            return null;

        int errorCount = (int) log.getEntries().stream()
                .filter(e -> e.level() == LogLevel.ERROR || e.level() == LogLevel.FATAL)
                .count();

        int warnCount = (int) log.getEntries().stream()
                .filter(e -> e.level() == LogLevel.WARN)
                .count();

        int nodeCount = (int) log.getEntries().stream()
                .filter(e -> e.category() == LogCategory.NODE_END)
                .count();

        return new ExecutionSummary(
                log.getExecutionId(),
                log.getWorkflowId(),
                log.getWorkflowName(),
                log.getStartTime(),
                log.getEndTime(),
                log.getDurationMs(),
                log.isSuccess(),
                nodeCount,
                errorCount,
                warnCount,
                log.getEntries().size());
    }

    /**
     * Export logs to JSON.
     */
    public String exportToJson(String executionId) throws JsonProcessingException {
        ExecutionLog log = executionLogs.get(executionId);
        if (log == null)
            return "{}";

        Map<String, Object> export = new HashMap<>();
        export.put("summary", getSummary(executionId));
        export.put("entries", log.getEntries());

        return objectMapper.writeValueAsString(export);
    }

    /**
     * Export logs to a file.
     */
    public void exportToFile(String executionId, Path filePath) throws IOException {
        String json = exportToJson(executionId);
        Files.writeString(filePath, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Add a log handler.
     */
    public void addHandler(ExecutionLogHandler handler) {
        logHandlers.add(handler);
    }

    /**
     * Remove a log handler.
     */
    public void removeHandler(ExecutionLogHandler handler) {
        logHandlers.remove(handler);
    }

    /**
     * Clear logs for an execution.
     */
    public void clearExecution(String executionId) {
        executionLogs.remove(executionId);
    }

    /**
     * Clear all logs.
     */
    public void clearAll() {
        executionLogs.clear();
    }

    private String truncate(String s, int maxLength) {
        if (s == null)
            return null;
        if (s.length() <= maxLength)
            return s;
        return s.substring(0, maxLength) + "...";
    }

    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /**
     * Log entry record.
     */
    public record LogEntry(
            String id,
            String executionId,
            Instant timestamp,
            LogLevel level,
            LogCategory category,
            String message,
            Map<String, Object> context) {
    }

    /**
     * Execution summary record.
     */
    public record ExecutionSummary(
            String executionId,
            String workflowId,
            String workflowName,
            Instant startTime,
            Instant endTime,
            long durationMs,
            boolean success,
            int nodeCount,
            int errorCount,
            int warnCount,
            int totalEntries) {
    }

    /**
     * Internal class to track execution logs.
     */
    private static class ExecutionLog {
        private final String executionId;
        private final String workflowId;
        private final String workflowName;
        private final Instant startTime;
        private Instant endTime;
        private boolean success;
        private final List<LogEntry> entries = new CopyOnWriteArrayList<>();

        ExecutionLog(String executionId, String workflowId, String workflowName) {
            this.executionId = executionId;
            this.workflowId = workflowId;
            this.workflowName = workflowName;
            this.startTime = Instant.now();
        }

        void addEntry(LogEntry entry) {
            entries.add(entry);
        }

        List<LogEntry> getEntries() {
            return new ArrayList<>(entries);
        }

        String getExecutionId() {
            return executionId;
        }

        String getWorkflowId() {
            return workflowId;
        }

        String getWorkflowName() {
            return workflowName;
        }

        Instant getStartTime() {
            return startTime;
        }

        Instant getEndTime() {
            return endTime;
        }

        void setEndTime(Instant endTime) {
            this.endTime = endTime;
        }

        boolean isSuccess() {
            return success;
        }

        void setSuccess(boolean success) {
            this.success = success;
        }

        long getDurationMs() {
            if (endTime == null) {
                return Instant.now().toEpochMilli() - startTime.toEpochMilli();
            }
            return endTime.toEpochMilli() - startTime.toEpochMilli();
        }
    }
}
