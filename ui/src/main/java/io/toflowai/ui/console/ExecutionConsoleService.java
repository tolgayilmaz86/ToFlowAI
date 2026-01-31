package io.toflowai.ui.console;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import javafx.application.Platform;

/**
 * Service that connects to the ExecutionConsole for real-time logging.
 * Acts as a bridge between workflow execution and the console UI.
 */
public class ExecutionConsoleService {

    private static ExecutionConsoleService instance;
    private ExecutionConsole console;
    private final ConcurrentHashMap<String, NodeExecutionState> nodeStates = new ConcurrentHashMap<>();
    
    // Buffer for logs when console is not yet created
    private final ConcurrentLinkedQueue<Consumer<ExecutionConsole>> logBuffer = new ConcurrentLinkedQueue<>();

    // Callback interfaces for node state changes
    public interface NodeStateListener {
        void onNodeStateChanged(String nodeId, NodeState state);
    }

    public enum NodeState {
        IDLE,
        RUNNING,
        SUCCESS,
        FAILED,
        SKIPPED
    }

    public record NodeExecutionState(
            String nodeId,
            String nodeName,
            NodeState state,
            long startTime,
            long endTime,
            String error) {
        public NodeExecutionState running() {
            return new NodeExecutionState(nodeId, nodeName, NodeState.RUNNING, System.currentTimeMillis(), 0, null);
        }

        public NodeExecutionState success() {
            return new NodeExecutionState(nodeId, nodeName, NodeState.SUCCESS, startTime, System.currentTimeMillis(),
                    null);
        }

        public NodeExecutionState failed(String error) {
            return new NodeExecutionState(nodeId, nodeName, NodeState.FAILED, startTime, System.currentTimeMillis(),
                    error);
        }

        public NodeExecutionState skipped() {
            return new NodeExecutionState(nodeId, nodeName, NodeState.SKIPPED, 0, 0, null);
        }
    }

    private final java.util.List<NodeStateListener> nodeStateListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    private ExecutionConsoleService() {
    }

    public static synchronized ExecutionConsoleService getInstance() {
        if (instance == null) {
            instance = new ExecutionConsoleService();
        }
        return instance;
    }

    /**
     * Set the console instance.
     */
    public void setConsole(ExecutionConsole console) {
        this.console = console;
    }

    /**
     * Get or create the console window.
     */
    public ExecutionConsole getOrCreateConsole() {
        if (console == null) {
            console = new ExecutionConsole();
            // Flush any buffered logs to the newly created console
            flushLogBuffer();
        }
        return console;
    }

    /**
     * Show the console window.
     */
    public void showConsole() {
        Platform.runLater(() -> {
            ExecutionConsole c = getOrCreateConsole();
            c.show();
            c.toFront();
        });
    }

    /**
     * Flush buffered logs to the console.
     */
    private void flushLogBuffer() {
        Consumer<ExecutionConsole> logOperation;
        while ((logOperation = logBuffer.poll()) != null) {
            logOperation.accept(console);
        }
    }

    /**
     * Add a node state listener.
     */
    public void addNodeStateListener(NodeStateListener listener) {
        nodeStateListeners.add(listener);
    }

    /**
     * Remove a node state listener.
     */
    public void removeNodeStateListener(NodeStateListener listener) {
        nodeStateListeners.remove(listener);
    }

    // ===== Logging Methods =====

    /**
     * Log execution start.
     */
    public void executionStart(String executionId, String workflowId, String workflowName) {
        if (console != null) {
            console.startExecution(executionId, workflowName);
        } else {
            logBuffer.add(c -> c.startExecution(executionId, workflowName));
        }
        nodeStates.clear();
    }

    /**
     * Log execution end.
     */
    public void executionEnd(String executionId, boolean success, long durationMs) {
        if (console != null) {
            console.endExecution(executionId, success, durationMs);
        } else {
            logBuffer.add(c -> c.endExecution(executionId, success, durationMs));
        }

        // Reset all node states to idle after execution
        for (String nodeId : nodeStates.keySet()) {
            notifyNodeStateChanged(nodeId, NodeState.IDLE);
        }
    }

    /**
     * Log node start.
     */
    public void nodeStart(String executionId, String nodeId, String nodeName, String nodeType) {
        // Update node state
        NodeExecutionState state = new NodeExecutionState(nodeId, nodeName, NodeState.RUNNING,
                System.currentTimeMillis(), 0, null);
        nodeStates.put(nodeId, state);
        notifyNodeStateChanged(nodeId, NodeState.RUNNING);

        if (console != null) {
            console.nodeStart(executionId, nodeId, nodeName, nodeType);
        } else {
            logBuffer.add(c -> c.nodeStart(executionId, nodeId, nodeName, nodeType));
        }
    }

    /**
     * Log node end.
     */
    public void nodeEnd(String executionId, String nodeId, String nodeName, boolean success, long durationMs) {
        // Update node state
        NodeExecutionState current = nodeStates.get(nodeId);
        if (current != null) {
            nodeStates.put(nodeId, success ? current.success() : current.failed(null));
        }
        notifyNodeStateChanged(nodeId, success ? NodeState.SUCCESS : NodeState.FAILED);

        if (console != null) {
            console.nodeEnd(executionId, nodeId, nodeName, success, durationMs);
        } else {
            logBuffer.add(c -> c.nodeEnd(executionId, nodeId, nodeName, success, durationMs));
        }
    }

    /**
     * Log node skip.
     */
    public void nodeSkip(String executionId, String nodeId, String nodeName, String reason) {
        NodeExecutionState state = new NodeExecutionState(nodeId, nodeName, NodeState.SKIPPED, 0, 0, null);
        nodeStates.put(nodeId, state);
        notifyNodeStateChanged(nodeId, NodeState.SKIPPED);

        if (console != null) {
            console.nodeSkip(executionId, nodeId, nodeName, reason);
        } else {
            logBuffer.add(c -> c.nodeSkip(executionId, nodeId, nodeName, reason));
        }
    }

    /**
     * Log an error.
     */
    public void error(String executionId, String nodeId, String nodeName, Exception e) {
        String message = e.getMessage();
        String stackTrace = getStackTrace(e);

        // Update node state
        NodeExecutionState current = nodeStates.get(nodeId);
        if (current != null) {
            nodeStates.put(nodeId, current.failed(message));
        } else {
            nodeStates.put(nodeId, new NodeExecutionState(nodeId, nodeName, NodeState.FAILED, 0, 0, message));
        }
        notifyNodeStateChanged(nodeId, NodeState.FAILED);

        if (console != null) {
            console.error(executionId, nodeName != null ? nodeName : nodeId, message, stackTrace);
        } else {
            logBuffer.add(c -> c.error(executionId, nodeName != null ? nodeName : nodeId, message, stackTrace));
        }
    }

    /**
     * Log error with custom message.
     */
    public void error(String executionId, String source, String message, String details) {
        if (console != null) {
            console.error(executionId, source, message, details);
        } else {
            logBuffer.add(c -> c.error(executionId, source, message, details));
        }
    }

    /**
     * Log retry attempt.
     */
    public void retry(String executionId, String nodeId, int attempt, int maxRetries, long delayMs) {
        if (console != null) {
            console.retry(executionId, nodeId, attempt, maxRetries, delayMs);
        } else {
            logBuffer.add(c -> c.retry(executionId, nodeId, attempt, maxRetries, delayMs));
        }
    }

    /**
     * Log rate limiting.
     */
    public void rateLimit(String executionId, String bucketId, boolean throttled, long waitMs) {
        if (console != null) {
            console.rateLimit(executionId, bucketId, throttled, waitMs);
        } else {
            logBuffer.add(c -> c.rateLimit(executionId, bucketId, throttled, waitMs));
        }
    }

    /**
     * Log data flow.
     */
    public void dataFlow(String executionId, String fromNode, String toNode, int dataSize) {
        if (console != null) {
            console.dataFlow(executionId, fromNode, toNode, dataSize);
        } else {
            logBuffer.add(c -> c.dataFlow(executionId, fromNode, toNode, dataSize));
        }
    }

    /**
     * Log info message.
     */
    public void info(String executionId, String message, String details) {
        if (console != null) {
            console.info(executionId, message, details);
        } else {
            logBuffer.add(c -> c.info(executionId, message, details));
        }
    }

    /**
     * Log debug message.
     */
    public void debug(String executionId, String message, String details) {
        if (console != null) {
            console.debug(executionId, message, details);
        } else {
            logBuffer.add(c -> c.debug(executionId, message, details));
        }
    }

    /**
     * Get current state of a node.
     */
    public NodeState getNodeState(String nodeId) {
        NodeExecutionState state = nodeStates.get(nodeId);
        return state != null ? state.state() : NodeState.IDLE;
    }

    /**
     * Clear all states.
     */
    public void clearStates() {
        nodeStates.clear();
    }

    private void notifyNodeStateChanged(String nodeId, NodeState state) {
        for (NodeStateListener listener : nodeStateListeners) {
            try {
                listener.onNodeStateChanged(nodeId, state);
            } catch (Exception e) {
                // Don't let listener errors affect execution
            }
        }
    }

    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
