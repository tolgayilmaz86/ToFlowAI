package io.toflowai.common.service;

import java.time.Instant;
import java.util.Map;

/**
 * Interface for handling execution log events.
 * Allows decoupled notification of execution progress for UI updates.
 */
public interface ExecutionLogHandler {

    /**
     * Log level enumeration.
     */
    enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR, FATAL
    }

    /**
     * Log category enumeration.
     */
    enum LogCategory {
        EXECUTION_START,
        EXECUTION_END,
        NODE_START,
        NODE_END,
        NODE_SKIP,
        DATA_FLOW,
        VARIABLE,
        ERROR,
        RETRY,
        RATE_LIMIT,
        PERFORMANCE,
        CUSTOM
    }

    /**
     * Log entry record containing all log information.
     */
    record LogEntry(
            String id,
            String executionId,
            Instant timestamp,
            LogLevel level,
            LogCategory category,
            String message,
            Map<String, Object> context) {
    }

    /**
     * Handle a log entry.
     */
    void handle(LogEntry entry);
}
