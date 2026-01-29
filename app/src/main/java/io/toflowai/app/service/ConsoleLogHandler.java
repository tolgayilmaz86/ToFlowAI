package io.toflowai.app.service;

import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.toflowai.common.service.ExecutionLogHandler;

/**
 * Console log handler that outputs structured logs to SLF4J.
 * Formats log entries for human readability while preserving structure.
 */
@Component
public class ConsoleLogHandler implements ExecutionLogHandler {

    private static final Logger log = LoggerFactory.getLogger("workflow.execution");
    private final ObjectMapper objectMapper;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private boolean enabled = true;
    private ExecutionLogHandler.LogLevel minLevel = ExecutionLogHandler.LogLevel.INFO;
    private boolean includeContext = true;

    public ConsoleLogHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Override
    public void handle(ExecutionLogHandler.LogEntry entry) {
        if (!enabled)
            return;
        if (entry.level().ordinal() < minLevel.ordinal())
            return;

        String message = formatMessage(entry);

        switch (entry.level()) {
            case TRACE -> log.trace(message);
            case DEBUG -> log.debug(message);
            case INFO -> log.info(message);
            case WARN -> log.warn(message);
            case ERROR, FATAL -> log.error(message);
        }
    }

    private String formatMessage(ExecutionLogHandler.LogEntry entry) {
        StringBuilder sb = new StringBuilder();

        // Timestamp
        String time = timeFormatter.format(entry.timestamp().atZone(java.time.ZoneId.systemDefault()));
        sb.append("[").append(time).append("] ");

        // Category icon
        sb.append(getCategoryIcon(entry.category())).append(" ");

        // Execution ID (abbreviated)
        String execId = entry.executionId();
        if (execId != null && execId.length() > 8) {
            execId = execId.substring(0, 8);
        }
        sb.append("[").append(execId).append("] ");

        // Category
        sb.append(entry.category().name()).append(": ");

        // Message
        sb.append(entry.message());

        // Context (if enabled and not empty)
        if (includeContext && entry.context() != null && !entry.context().isEmpty()) {
            try {
                String contextJson = objectMapper.writeValueAsString(entry.context());
                sb.append(" | ").append(contextJson);
            } catch (JsonProcessingException e) {
                sb.append(" | {context serialization error}");
            }
        }

        return sb.toString();
    }

    private String getCategoryIcon(ExecutionLogHandler.LogCategory category) {
        return switch (category) {
            case EXECUTION_START -> "🚀";
            case EXECUTION_END -> "🏁";
            case NODE_START -> "▶️";
            case NODE_END -> "✅";
            case NODE_SKIP -> "⏭️";
            case DATA_FLOW -> "📦";
            case VARIABLE -> "📝";
            case ERROR -> "❌";
            case RETRY -> "🔄";
            case RATE_LIMIT -> "⏱️";
            case PERFORMANCE -> "📊";
            case CUSTOM -> "💬";
        };
    }

    // Configuration methods

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setMinLevel(ExecutionLogHandler.LogLevel minLevel) {
        this.minLevel = minLevel;
    }

    public ExecutionLogHandler.LogLevel getMinLevel() {
        return minLevel;
    }

    public void setIncludeContext(boolean includeContext) {
        this.includeContext = includeContext;
    }

    public boolean isIncludeContext() {
        return includeContext;
    }
}
