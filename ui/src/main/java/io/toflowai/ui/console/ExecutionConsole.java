package io.toflowai.ui.console;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Execution Console - A sophisticated logging window for workflow execution.
 * Shows hierarchical logs with indentation, colors, and real-time updates.
 */
public class ExecutionConsole extends Stage {

    private final VBox logContainer;
    private final ScrollPane scrollPane;
    private final Map<String, ExecutionSession> sessions = new ConcurrentHashMap<>();
    private final List<ConsoleLogListener> listeners = new CopyOnWriteArrayList<>();
    private final Label filterStatusLabel = new Label("");
    private final Label sessionCountLabel = new Label("Sessions: 0");

    private boolean autoScroll = true;
    private boolean showTimestamps = true;
    private boolean showDebug = false;
    private boolean showTrace = false;
    private String filterText = "";

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    // Colors for different log levels and categories
    private static final String COLOR_INFO = "#88c0d0"; // Nord blue
    private static final String COLOR_DEBUG = "#a3be8c"; // Nord green
    private static final String COLOR_WARN = "#ebcb8b"; // Nord yellow
    private static final String COLOR_ERROR = "#bf616a"; // Nord red
    private static final String COLOR_TRACE = "#b48ead"; // Nord purple
    private static final String COLOR_TIMESTAMP = "#616e88"; // Nord muted
    private static final String COLOR_NODE = "#81a1c1"; // Nord lighter blue
    private static final String COLOR_SUCCESS = "#a3be8c"; // Nord green
    private static final String COLOR_EXECUTION = "#d8dee9"; // Nord white

    public ExecutionConsole() {
        initStyle(StageStyle.DECORATED);
        setTitle("Execution Console");
        setMinWidth(600);
        setMinHeight(400);
        setWidth(900);
        setHeight(600);

        // Main layout
        BorderPane root = new BorderPane();
        root.getStyleClass().add("execution-console");
        root.setStyle("-fx-background-color: #2e3440;"); // Nord dark background

        // Toolbar
        ToolBar toolbar = createToolbar();
        root.setTop(toolbar);

        // Log container with scroll
        logContainer = new VBox(2);
        logContainer.setPadding(new Insets(10));
        logContainer.setStyle("-fx-background-color: #2e3440;");

        scrollPane = new ScrollPane(logContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #2e3440; -fx-background-color: #2e3440;");
        scrollPane.getStyleClass().add("console-scroll");

        // Auto-scroll behavior
        logContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (autoScroll) {
                scrollPane.setVvalue(1.0);
            }
        });

        root.setCenter(scrollPane);

        // Status bar
        HBox statusBar = createStatusBar();
        root.setBottom(statusBar);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/io/toflowai/ui/styles/console.css").toExternalForm());
        setScene(scene);

        // Handle window close by hiding instead of closing to preserve logs
        setOnCloseRequest(event -> {
            event.consume(); // Prevent default close behavior
            hide(); // Hide the window instead
        });
    }

    private ToolBar createToolbar() {
        ToolBar toolbar = new ToolBar();
        toolbar.setStyle("-fx-background-color: #3b4252; -fx-padding: 5;");

        // Clear button
        Button clearBtn = createToolButton("Clear", MaterialDesignD.DELETE_OUTLINE, () -> {
            if (!sessions.isEmpty()) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Clear Console");
                confirm.setHeaderText("Clear all log entries?");
                confirm.setContentText("This will permanently remove all execution logs and cannot be undone.");
                confirm.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        clear();
                    }
                });
            }
        });

        // Copy button
        Button copyBtn = createToolButton("Copy All", MaterialDesignC.CONTENT_COPY, () -> {
            copyToClipboard();
            // Show feedback
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Logs Copied");
            info.setHeaderText(null);
            info.setContentText("All execution logs have been copied to clipboard.");
            info.showAndWait();
        });

        // Auto-scroll toggle
        ToggleButton autoScrollBtn = new ToggleButton();
        autoScrollBtn.setGraphic(FontIcon.of(MaterialDesignA.ARROW_DOWN_BOLD, 16, Color.web("#d8dee9")));
        autoScrollBtn.setTooltip(new Tooltip("Auto-scroll"));
        autoScrollBtn.setSelected(autoScroll);
        autoScrollBtn.selectedProperty().addListener((obs, old, val) -> autoScroll = val);
        autoScrollBtn.setStyle("-fx-background-color: transparent;");

        // Timestamps toggle
        ToggleButton timestampBtn = new ToggleButton();
        timestampBtn.setGraphic(FontIcon.of(MaterialDesignC.CLOCK_OUTLINE, 16, Color.web("#d8dee9")));
        timestampBtn.setTooltip(new Tooltip("Show timestamps"));
        timestampBtn.setSelected(showTimestamps);
        timestampBtn.selectedProperty().addListener((obs, old, val) -> {
            showTimestamps = val;
            refreshDisplay();
        });
        timestampBtn.setStyle("-fx-background-color: transparent;");

        // Debug toggle
        ToggleButton debugBtn = new ToggleButton("DEBUG");
        debugBtn.setTooltip(new Tooltip("Show only debug messages"));
        debugBtn.setSelected(showDebug);
        debugBtn.selectedProperty().addListener((obs, old, val) -> {
            showDebug = val;
            Platform.runLater(() -> refreshDisplay());
        });
        debugBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #a3be8c;");

        // Trace toggle
        ToggleButton traceBtn = new ToggleButton("TRACE");
        traceBtn.setTooltip(new Tooltip("Show only trace messages"));
        traceBtn.setSelected(showTrace);
        traceBtn.selectedProperty().addListener((obs, old, val) -> {
            showTrace = val;
            Platform.runLater(() -> refreshDisplay());
        });
        traceBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #b48ead;");

        // Separator
        Separator sep1 = new Separator();
        sep1.setOrientation(Orientation.VERTICAL);

        // Filter field
        TextField filterField = new TextField();
        filterField.setPromptText("Filter logs...");
        filterField.setPrefWidth(200);
        filterField.setStyle("-fx-background-color: #434c5e; -fx-text-fill: #d8dee9; -fx-prompt-text-fill: #616e88;");
        filterField.textProperty().addListener((obs, old, val) -> {
            filterText = val.toLowerCase();
            Platform.runLater(() -> refreshDisplay());
        });

        toolbar.getItems().addAll(
                clearBtn, copyBtn,
                new Separator(),
                autoScrollBtn, timestampBtn,
                new Separator(),
                debugBtn, traceBtn,
                new Separator(),
                filterField);

        return toolbar;
    }

    private Button createToolButton(String tooltip, org.kordamp.ikonli.Ikon icon, Runnable action) {
        Button btn = new Button();
        btn.setGraphic(FontIcon.of(icon, 16, Color.web("#d8dee9")));
        btn.setTooltip(new Tooltip(tooltip));
        btn.setOnAction(e -> action.run());
        btn.setStyle("-fx-background-color: transparent;");
        return btn;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(5, 10, 5, 10));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setStyle("-fx-background-color: #3b4252;");

        Label status = new Label("Ready");
        status.setStyle("-fx-text-fill: #88c0d0;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        filterStatusLabel.setStyle("-fx-text-fill: #ebcb8b;");

        sessionCountLabel.setStyle("-fx-text-fill: #616e88;");

        statusBar.getChildren().addAll(status, spacer, filterStatusLabel, sessionCountLabel);
        return statusBar;
    }

    // ===== Public API =====

    /**
     * Start a new execution session.
     */
    public void startExecution(String executionId, String workflowName) {
        Platform.runLater(() -> {
            ExecutionSession session = new ExecutionSession(executionId, workflowName);
            sessions.put(executionId, session);

            // Add execution header
            HBox header = createExecutionHeader(executionId, workflowName, Instant.now());
            logContainer.getChildren().add(header);
            session.setHeaderIndex(logContainer.getChildren().size() - 1);

            updateSessionCount();
        });
    }

    /**
     * End an execution session.
     */
    public void endExecution(String executionId, boolean success, long durationMs) {
        Platform.runLater(() -> {
            ExecutionSession session = sessions.get(executionId);
            if (session != null) {
                session.setComplete(true);
                session.setSuccess(success);
                session.setDurationMs(durationMs);

                // Add completion entry
                logEntry(executionId, 0,
                        success ? LogEntryType.SUCCESS : LogEntryType.ERROR,
                        success ? "✓ Execution completed" : "✗ Execution failed",
                        String.format("Duration: %dms", durationMs));
            }
        });
    }

    /**
     * Log a node starting execution.
     */
    public void nodeStart(String executionId, String nodeId, String nodeName, String nodeType) {
        Platform.runLater(() -> {
            ExecutionSession session = sessions.get(executionId);
            if (session != null) {
                int depth = session.pushNode(nodeId);
                logEntry(executionId, depth, LogEntryType.NODE_START,
                        "▶ " + nodeName + " (" + nodeType + ")",
                        "Node ID: " + nodeId);
            }
        });
    }

    /**
     * Log a node completing execution.
     */
    public void nodeEnd(String executionId, String nodeId, String nodeName, boolean success, long durationMs) {
        Platform.runLater(() -> {
            ExecutionSession session = sessions.get(executionId);
            if (session != null) {
                int depth = session.getNodeDepth(nodeId);
                String status = success ? "✓" : "✗";
                String details = String.format("Node ID: %s | Duration: %dms", nodeId, durationMs);
                logEntry(executionId, depth,
                        success ? LogEntryType.NODE_END : LogEntryType.ERROR,
                        status + " " + nodeName,
                        details);
                session.popNode(nodeId);
            }
        });
    }

    /**
     * Log node input data.
     */
    public void nodeInput(String executionId, String nodeId, String nodeName, Object input) {
        Platform.runLater(() -> {
            ExecutionSession session = sessions.get(executionId);
            if (session != null) {
                int depth = session.getNodeDepth(nodeId);
                String inputStr = formatDataPreview(input);
                logEntry(executionId, depth + 1, LogEntryType.TRACE,
                        "📥 Input: " + nodeName,
                        inputStr);
            }
        });
    }

    /**
     * Log node output data.
     */
    public void nodeOutput(String executionId, String nodeId, String nodeName, Object output) {
        Platform.runLater(() -> {
            ExecutionSession session = sessions.get(executionId);
            if (session != null) {
                int depth = session.getNodeDepth(nodeId);
                String outputStr = formatDataPreview(output);
                logEntry(executionId, depth + 1, LogEntryType.TRACE,
                        "📤 Output: " + nodeName,
                        outputStr);
            }
        });
    }

    /**
     * Format data for preview display.
     */
    private String formatDataPreview(Object data) {
        if (data == null) {
            return "null";
        }

        String str = data.toString();
        if (str.length() > 200) {
            return str.substring(0, 200) + "...";
        }

        // For collections/maps, show count
        if (data instanceof Map) {
            return String.format("Map with %d entries: %s", ((Map<?, ?>) data).size(), str);
        }
        if (data instanceof Iterable && !(data instanceof String)) {
            int count = 0;
            for (Object item : (Iterable<?>) data) {
                count++;
                if (count > 10)
                    break; // Don't count too many
            }
            return String.format("Collection with %d items: %s", count, str);
        }

        return str;
    }

    /**
     * Log a node being skipped.
     */
    public void nodeSkip(String executionId, String nodeId, String nodeName, String reason) {
        Platform.runLater(() -> {
            ExecutionSession session = sessions.get(executionId);
            if (session != null) {
                int depth = session.getCurrentDepth();
                logEntry(executionId, depth, LogEntryType.DEBUG,
                        "⏭ Skipped: " + nodeName,
                        reason);
            }
        });
    }

    /**
     * Log an error.
     */
    public void error(String executionId, String source, String message, String stackTrace) {
        Platform.runLater(() -> {
            ExecutionSession session = sessions.get(executionId);
            int depth = session != null ? session.getCurrentDepth() : 0;

            logEntry(executionId, depth, LogEntryType.ERROR,
                    "❌ Error in " + source,
                    message);

            if (stackTrace != null && !stackTrace.isEmpty()) {
                logEntry(executionId, depth + 1, LogEntryType.TRACE,
                        "Stack trace:",
                        stackTrace);
            }
        });
    }

    /**
     * Log a retry attempt.
     */
    public void retry(String executionId, String nodeId, int attempt, int maxRetries, long delayMs) {
        Platform.runLater(() -> {
            ExecutionSession session = sessions.get(executionId);
            int depth = session != null ? session.getCurrentDepth() : 0;

            logEntry(executionId, depth, LogEntryType.WARN,
                    String.format("🔄 Retry %d/%d", attempt, maxRetries),
                    String.format("Waiting %dms before next attempt", delayMs));
        });
    }

    /**
     * Log rate limiting.
     */
    public void rateLimit(String executionId, String bucketId, boolean throttled, long waitMs) {
        Platform.runLater(() -> {
            ExecutionSession session = sessions.get(executionId);
            int depth = session != null ? session.getCurrentDepth() : 0;

            if (throttled) {
                logEntry(executionId, depth, LogEntryType.WARN,
                        "⏱ Rate limited: " + bucketId,
                        String.format("Waited %dms for tokens", waitMs));
            } else {
                logEntry(executionId, depth, LogEntryType.DEBUG,
                        "⏱ Rate limit passed: " + bucketId,
                        null);
            }
        });
    }

    /**
     * Log data flow between nodes.
     */
    public void dataFlow(String executionId, String fromNode, String toNode, int dataSize) {
        Platform.runLater(() -> {
            ExecutionSession session = sessions.get(executionId);
            int depth = session != null ? session.getCurrentDepth() : 0;

            logEntry(executionId, depth, LogEntryType.TRACE,
                    String.format("📦 Data: %s → %s", fromNode, toNode),
                    String.format("Size: %d bytes", dataSize));
        });
    }

    /**
     * Log custom info message.
     */
    public void info(String executionId, String message, String details) {
        Platform.runLater(() -> {
            ExecutionSession session = sessions.get(executionId);
            int depth = session != null ? session.getCurrentDepth() : 0;
            logEntry(executionId, depth, LogEntryType.INFO, message, details);
        });
    }

    /**
     * Log custom debug message.
     */
    public void debug(String executionId, String message, String details) {
        Platform.runLater(() -> {
            ExecutionSession session = sessions.get(executionId);
            int depth = session != null ? session.getCurrentDepth() : 0;
            logEntry(executionId, depth, LogEntryType.DEBUG, message, details);
        });
    }

    /**
     * Clear all log entries.
     */
    public void clear() {
        Platform.runLater(() -> {
            logContainer.getChildren().clear();
            sessions.clear();
            updateSessionCount();
        });
    }

    /**
     * Copy all logs to clipboard.
     */
    public void copyToClipboard() {
        StringBuilder sb = new StringBuilder();
        for (ExecutionSession session : sessions.values()) {
            sb.append("=== Execution: ").append(session.getWorkflowName()).append(" ===\n");
            for (LogEntryData entry : session.getEntries()) {
                sb.append(formatEntryAsText(entry)).append("\n");
            }
            sb.append("\n");
        }

        ClipboardContent content = new ClipboardContent();
        content.putString(sb.toString());
        Clipboard.getSystemClipboard().setContent(content);
    }

    // ===== Private Methods =====

    private void logEntry(String executionId, int depth, LogEntryType type, String message, String details) {
        ExecutionSession session = sessions.get(executionId);
        LogEntryData data = new LogEntryData(Instant.now(), depth, type, message, details);

        if (session != null) {
            session.addEntry(data);
        }

        // Check visibility based on filters
        if (!shouldShowEntry(data)) {
            return;
        }

        VBox entryBox = createLogEntryView(data);
        logContainer.getChildren().add(entryBox);

        // Notify listeners
        for (ConsoleLogListener listener : listeners) {
            listener.onLogEntry(executionId, type, message, details);
        }
    }

    private boolean shouldShowEntry(LogEntryData entry) {
        // If any level filters are active, only show selected levels
        if (showDebug || showTrace) {
            if (entry.type == LogEntryType.DEBUG && !showDebug)
                return false;
            if (entry.type == LogEntryType.TRACE && !showTrace)
                return false;
            // Hide other levels when filters are active
            if (entry.type != LogEntryType.DEBUG && entry.type != LogEntryType.TRACE)
                return false;
        }

        // Text filter
        if (!filterText.isEmpty()) {
            String searchable = (entry.message + " " + (entry.details != null ? entry.details : "")).toLowerCase();
            if (!searchable.contains(filterText))
                return false;
        }

        return true;
    }

    private HBox createExecutionHeader(String executionId, String workflowName, Instant timestamp) {
        HBox header = new HBox(10);
        header.setPadding(new Insets(8, 10, 8, 10));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #3b4252; -fx-background-radius: 5;");

        FontIcon icon = FontIcon.of(MaterialDesignP.PLAY_CIRCLE, 18, Color.web(COLOR_SUCCESS));

        Text title = new Text("Execution: " + workflowName);
        title.setFont(Font.font("System", FontWeight.BOLD, 13));
        title.setFill(Color.web(COLOR_EXECUTION));

        Text time = new Text(formatTimestamp(timestamp));
        time.setFill(Color.web(COLOR_TIMESTAMP));
        time.setFont(Font.font("System", 11));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Text id = new Text("#" + executionId.substring(0, Math.min(8, executionId.length())));
        id.setFill(Color.web(COLOR_TIMESTAMP));
        id.setFont(Font.font("Monospace", 10));

        header.getChildren().addAll(icon, title, time, spacer, id);
        return header;
    }

    private VBox createLogEntryView(LogEntryData entry) {
        VBox entryContainer = new VBox(2);
        entryContainer.setPadding(new Insets(2, 10, 2, 10));

        // Main log line
        HBox mainLine = new HBox(5);
        mainLine.setAlignment(Pos.TOP_LEFT);

        // Indentation
        int indentPixels = entry.depth * 20;
        Region indent = new Region();
        indent.setMinWidth(indentPixels);
        indent.setPrefWidth(indentPixels);

        // Timestamp
        if (showTimestamps) {
            Text timestamp = new Text(formatTimestamp(entry.timestamp) + " ");
            timestamp.setFill(Color.web(COLOR_TIMESTAMP));
            timestamp.setFont(Font.font("Monospace", 10));
            mainLine.getChildren().add(timestamp);
        }

        mainLine.getChildren().add(indent);

        // Type indicator with color
        Text typeIndicator = new Text(getTypeIndicator(entry.type));
        typeIndicator.setFill(Color.web(getColorForType(entry.type)));
        typeIndicator.setFont(Font.font("System", FontWeight.BOLD, 12));
        mainLine.getChildren().add(typeIndicator);

        // Message
        Text messageText = new Text(entry.message);
        messageText.setFill(Color.web(getColorForType(entry.type)));
        messageText.setFont(Font.font("System", 12));
        mainLine.getChildren().add(messageText);

        entryContainer.getChildren().add(mainLine);

        // Details section (collapsible)
        if (entry.details != null && !entry.details.isEmpty()) {
            // Create clickable details indicator
            String indicator = getDetailsIndicator(entry);
            Text detailsLink = new Text(" " + indicator);
            detailsLink.setFill(Color.web(COLOR_TIMESTAMP));
            detailsLink.setFont(Font.font("System", FontWeight.BOLD, 11));

            // Make it clickable to expand
            detailsLink.setOnMouseClicked(e -> toggleDetails(entryContainer, entry));
            detailsLink.setOnMouseEntered(e -> {
                detailsLink.setUnderline(true);
                detailsLink.setFill(Color.web("#88c0d0")); // Nord blue on hover
            });
            detailsLink.setOnMouseExited(e -> {
                detailsLink.setUnderline(false);
                detailsLink.setFill(Color.web(COLOR_TIMESTAMP));
            });
            detailsLink.setStyle("-fx-cursor: hand;");

            mainLine.getChildren().add(detailsLink);
        }

        // Make error entries more visible
        if (entry.type == LogEntryType.ERROR) {
            entryContainer.setStyle("-fx-background-color: rgba(191, 97, 106, 0.1);");
        }

        return entryContainer;
    }

    private String getDetailsIndicator(LogEntryData entry) {
        if (entry.details == null || entry.details.isEmpty()) {
            return "";
        }

        // Check content type and return appropriate indicator
        String details = entry.details.toLowerCase();

        if (details.contains("duration")) {
            return "⏱️"; // Clock emoji for duration
        } else if (details.contains("error") || details.contains("exception") || details.contains("stack")) {
            return "⚠️"; // Warning for errors
        } else if (details.contains("node id") || details.contains("type:")) {
            return "ℹ️"; // Info for node details
        } else if (details.contains("input") || details.contains("output")) {
            return "📄"; // Document for data
        } else if (details.contains("size:") || details.contains("bytes")) {
            return "📊"; // Chart for data size
        } else {
            return "▶"; // Default expand arrow
        }
    }

    private String getTypeIndicator(LogEntryType type) {
        return switch (type) {
            case INFO -> "ℹ ";
            case DEBUG -> "🔧 ";
            case WARN -> "⚠ ";
            case ERROR -> "❌ ";
            case TRACE -> "📝 ";
            case NODE_START -> "▶ ";
            case NODE_END -> "■ ";
            case SUCCESS -> "✓ ";
        };
    }

    private String getColorForType(LogEntryType type) {
        return switch (type) {
            case INFO -> COLOR_INFO;
            case DEBUG -> COLOR_DEBUG;
            case WARN -> COLOR_WARN;
            case ERROR -> COLOR_ERROR;
            case TRACE -> COLOR_TRACE;
            case NODE_START, NODE_END -> COLOR_NODE;
            case SUCCESS -> COLOR_SUCCESS;
        };
    }

    private String formatTimestamp(Instant timestamp) {
        return LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault()).format(TIME_FORMAT);
    }

    private String truncate(String s, int maxLength) {
        if (s == null)
            return "";
        if (s.length() <= maxLength)
            return s;
        return s.substring(0, maxLength) + "...";
    }

    /**
     * Toggle the expanded details view for a log entry.
     */
    private void toggleDetails(VBox entryContainer, LogEntryData entry) {
        // Check if details are already expanded
        if (entryContainer.getChildren().size() > 1) {
            // Remove the expanded details
            entryContainer.getChildren().remove(1);
            // Update the indicator back to collapsed state
            updateDetailsIndicator(entryContainer, entry, false);
        } else {
            // Add expanded details
            Region detailsContent = createDetailsContent(entry);
            entryContainer.getChildren().add(detailsContent);

            // Update the indicator to show expanded state
            updateDetailsIndicator(entryContainer, entry, true);
        }
    }

    private Region createDetailsContent(LogEntryData entry) {
        // Create a styled container for details
        VBox detailsBox = new VBox(5);
        detailsBox.setPadding(new Insets(10, 20, 10, 40)); // Indent from main content
        detailsBox.setStyle("-fx-background-color: rgba(46, 52, 64, 0.3); -fx-background-radius: 3;"); // Subtle
                                                                                                       // background

        // Parse and format details based on content type
        if (entry.details.contains("• ")) {
            // Structured details (from our new formatContextDetails)
            String[] lines = entry.details.split("\n");
            for (String line : lines) {
                if (line.trim().isEmpty())
                    continue;

                Text detailLine = new Text(line);
                detailLine.setFill(Color.web("#d8dee9")); // Nord light text
                detailLine.setFont(Font.font("System", 11));
                detailsBox.getChildren().add(detailLine);
            }
        } else if (entry.details.contains("Duration:")) {
            // Special formatting for duration
            Text durationText = new Text("⏱️ Execution Time: " + entry.details.replace("Duration:", "").trim());
            durationText.setFill(Color.web("#88c0d0")); // Nord blue
            durationText.setFont(Font.font("System", FontWeight.BOLD, 12));
            detailsBox.getChildren().add(durationText);
        } else if (entry.details.contains("Node ID:")) {
            // Special formatting for node details
            String[] parts = entry.details.split("\\|");
            for (String part : parts) {
                Text detailText = new Text("ℹ️ " + part.trim());
                detailText.setFill(Color.web("#a3be8c")); // Nord green
                detailText.setFont(Font.font("System", 11));
                detailsBox.getChildren().add(detailText);
            }
        } else {
            // Fallback to text area for other content
            TextArea detailsArea = new TextArea(entry.details);
            detailsArea.setEditable(false);
            detailsArea.setWrapText(true);
            detailsArea.setPrefRowCount(Math.min(10, (int) Math.ceil(entry.details.length() / 80.0)));
            detailsArea.setMaxWidth(600);
            detailsArea.getStyleClass().add("log-details-textarea");
            detailsArea.setStyle("-fx-control-inner-background: rgba(46, 52, 64, 0.5); -fx-text-fill: #d8dee9;");
            return detailsArea;
        }

        return detailsBox;
    }

    private void updateDetailsIndicator(VBox entryContainer, LogEntryData entry, boolean expanded) {
        HBox mainLine = (HBox) entryContainer.getChildren().get(0);
        if (mainLine.getChildren().size() > 3) {
            Text indicator = (Text) mainLine.getChildren().get(3);
            String baseIndicator = getDetailsIndicator(entry);
            indicator.setText(expanded ? " ▼" : " " + baseIndicator);
        }
    }

    private String formatEntryAsText(LogEntryData entry) {
        StringBuilder sb = new StringBuilder();
        sb.append(formatTimestamp(entry.timestamp)).append(" ");
        sb.append("  ".repeat(entry.depth));
        sb.append("[").append(entry.type.name()).append("] ");
        sb.append(entry.message);
        if (entry.details != null) {
            sb.append(" — ").append(entry.details);
        }
        return sb.toString();
    }

    private void refreshDisplay() {
        updateFilterStatus();
        logContainer.getChildren().clear();

        // Rebuild the display from stored session data
        for (ExecutionSession session : sessions.values()) {
            // Add session header
            HBox header = createExecutionHeader(session.getExecutionId(),
                    session.getWorkflowName(), session.getStartTime());
            logContainer.getChildren().add(header);

            // Add all visible entries for this session
            for (LogEntryData entry : session.getEntries()) {
                if (shouldShowEntry(entry)) {
                    VBox entryBox = createLogEntryView(entry);
                    logContainer.getChildren().add(entryBox);
                }
            }
        }

        // Auto-scroll to bottom if enabled
        if (autoScroll) {
            Platform.runLater(() -> scrollPane.setVvalue(1.0));
        }
    }

    private void updateFilterStatus() {
        StringBuilder status = new StringBuilder();
        if (showDebug) {
            status.append("DEBUG ");
        }
        if (showTrace) {
            status.append("TRACE ");
        }
        if (!filterText.isEmpty()) {
            status.append("FILTER: ").append(filterText).append(" ");
        }

        Platform.runLater(() -> {
            filterStatusLabel.setText(status.toString().trim());
        });
    }

    private void updateSessionCount() {
        Platform.runLater(() -> {
            sessionCountLabel.setText("Sessions: " + sessions.size());
        });
    }

    /**
     * Add a listener for log events.
     */
    public void addLogListener(ConsoleLogListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove a listener.
     */
    public void removeLogListener(ConsoleLogListener listener) {
        listeners.remove(listener);
    }

    // ===== Inner Classes =====

    public enum LogEntryType {
        INFO, DEBUG, WARN, ERROR, TRACE, NODE_START, NODE_END, SUCCESS
    }

    public record LogEntryData(
            Instant timestamp,
            int depth,
            LogEntryType type,
            String message,
            String details) {
    }

    public interface ConsoleLogListener {
        void onLogEntry(String executionId, LogEntryType type, String message, String details);
    }

    /**
     * Tracks an execution session with its call stack for indentation.
     */
    private static class ExecutionSession {
        private final String executionId;
        private final String workflowName;
        private final Instant startTime;
        private final List<String> nodeStack = new ArrayList<>();
        private final Map<String, Integer> nodeDepths = new HashMap<>();
        private final List<LogEntryData> entries = new ArrayList<>();
        private int headerIndex;
        private boolean complete;
        private boolean success;
        private long durationMs;

        ExecutionSession(String executionId, String workflowName) {
            this.executionId = executionId;
            this.workflowName = workflowName;
            this.startTime = Instant.now();
        }

        int pushNode(String nodeId) {
            int depth = nodeStack.size();
            nodeStack.add(nodeId);
            nodeDepths.put(nodeId, depth);
            return depth;
        }

        void popNode(String nodeId) {
            nodeStack.remove(nodeId);
            nodeDepths.remove(nodeId);
        }

        int getNodeDepth(String nodeId) {
            return nodeDepths.getOrDefault(nodeId, 0);
        }

        int getCurrentDepth() {
            return nodeStack.size();
        }

        void addEntry(LogEntryData entry) {
            entries.add(entry);
        }

        List<LogEntryData> getEntries() {
            return entries;
        }

        String getExecutionId() {
            return executionId;
        }

        String getWorkflowName() {
            return workflowName;
        }

        Instant getStartTime() {
            return startTime;
        }

        int getHeaderIndex() {
            return headerIndex;
        }

        void setHeaderIndex(int index) {
            this.headerIndex = index;
        }

        boolean isComplete() {
            return complete;
        }

        void setComplete(boolean complete) {
            this.complete = complete;
        }

        boolean isSuccess() {
            return success;
        }

        void setSuccess(boolean success) {
            this.success = success;
        }

        long getDurationMs() {
            return durationMs;
        }

        void setDurationMs(long durationMs) {
            this.durationMs = durationMs;
        }
    }
}
