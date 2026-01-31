package io.toflowai.ui.console;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
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
import java.util.concurrent.atomic.AtomicInteger;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignE;
import org.kordamp.ikonli.materialdesign2.MaterialDesignN;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Execution Console - A sophisticated logging window for workflow execution.
 * Features: Summary dashboard, hierarchical logs, real-time updates, filtering.
 */
public class ExecutionConsole extends Stage {

    private final VBox logContainer;
    private final ScrollPane logsScrollPane;
    private final VBox summaryContainer;
    private final Map<String, ExecutionSession> sessions = new ConcurrentHashMap<>();
    private final List<ConsoleLogListener> listeners = new CopyOnWriteArrayList<>();
    private final Label filterStatusLabel = new Label("");
    private final Label sessionCountLabel = new Label("Sessions: 0");
    private final ComboBox<String> sessionSelector = new ComboBox<>();
    private final TabPane mainTabPane;

    // Summary UI components
    private Label summaryWorkflowLabel;
    private Label summaryStatusLabel;
    private Label summaryDurationLabel;
    private Label summaryStartTimeLabel;
    private Label summaryNodesCountLabel;
    private Label summaryErrorCountLabel;
    private VBox summaryNodesList;
    private ProgressBar summaryProgressBar;
    private Label summaryProgressLabel;

    private boolean autoScroll = true;
    private boolean pendingAutoScroll = false; // Flag to indicate new entry was added
    private boolean showTimestamps = true;
    private boolean showDebug = false;
    private boolean showTrace = false;
    private String filterText = "";
    private String selectedSessionId = null;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Nord color palette
    private static final String COLOR_INFO = "#88c0d0";
    private static final String COLOR_DEBUG = "#a3be8c";
    private static final String COLOR_WARN = "#ebcb8b";
    private static final String COLOR_ERROR = "#bf616a";
    private static final String COLOR_TRACE = "#b48ead";
    private static final String COLOR_TIMESTAMP = "#616e88";
    private static final String COLOR_NODE = "#81a1c1";
    private static final String COLOR_SUCCESS = "#a3be8c";
    private static final String COLOR_EXECUTION = "#d8dee9";
    private static final String BG_DARK = "#2e3440";
    private static final String BG_MEDIUM = "#3b4252";
    private static final String BG_LIGHT = "#434c5e";

    public ExecutionConsole() {
        initStyle(StageStyle.DECORATED);
        setTitle("Execution Console");
        setMinWidth(700);
        setMinHeight(500);
        setWidth(1000);
        setHeight(700);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("execution-console");
        root.setStyle("-fx-background-color: " + BG_DARK + ";");

        // Toolbar
        ToolBar toolbar = createToolbar();
        root.setTop(toolbar);

        // Main tab pane with Summary and Logs tabs
        mainTabPane = new TabPane();
        mainTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        mainTabPane.setStyle("-fx-background-color: " + BG_DARK + ";");

        // Summary Tab
        summaryContainer = new VBox(15);
        summaryContainer.setPadding(new Insets(20));
        summaryContainer.setStyle("-fx-background-color: " + BG_DARK + ";");
        ScrollPane summaryScroll = new ScrollPane(summaryContainer);
        summaryScroll.setFitToWidth(true);
        summaryScroll.setStyle("-fx-background: " + BG_DARK + "; -fx-background-color: " + BG_DARK + ";");

        Tab summaryTab = new Tab("Summary", summaryScroll);
        summaryTab.setGraphic(FontIcon.of(MaterialDesignC.CHART_BOX_OUTLINE, 14, Color.web(COLOR_INFO)));
        buildSummaryView();

        // Logs Tab with enhanced features
        BorderPane logsPane = new BorderPane();
        logsPane.setStyle("-fx-background-color: " + BG_DARK + ";");

        // Logs toolbar
        HBox logsToolbar = createLogsToolbar();
        logsPane.setTop(logsToolbar);

        logContainer = new VBox(1);
        logContainer.setPadding(new Insets(5, 10, 10, 10));
        logContainer.setStyle("-fx-background-color: " + BG_DARK + ";");
        logsScrollPane = new ScrollPane(logContainer);
        logsScrollPane.setFitToWidth(true);
        logsScrollPane.setStyle("-fx-background: " + BG_DARK + "; -fx-background-color: " + BG_DARK + ";");
        logsScrollPane.getStyleClass().add("console-scroll");

        logContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (autoScroll && pendingAutoScroll) {
                logsScrollPane.setVvalue(1.0);
                pendingAutoScroll = false;
            }
        });

        logsPane.setCenter(logsScrollPane);

        Tab logsTab = new Tab("Logs", logsPane);
        logsTab.setGraphic(FontIcon.of(MaterialDesignT.TEXT_BOX_OUTLINE, 14, Color.web(COLOR_DEBUG)));

        mainTabPane.getTabs().addAll(summaryTab, logsTab);
        root.setCenter(mainTabPane);

        // Status bar
        HBox statusBar = createStatusBar();
        root.setBottom(statusBar);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/io/toflowai/ui/styles/console.css").toExternalForm());
        setScene(scene);

        setOnCloseRequest(event -> {
            event.consume();
            hide();
        });
    }

    private void buildSummaryView() {
        // Empty state message
        Label emptyLabel = new Label("No execution selected. Run a workflow to see details.");
        emptyLabel.setStyle("-fx-text-fill: " + COLOR_TIMESTAMP + "; -fx-font-size: 14px;");
        emptyLabel.setWrapText(true);
        summaryContainer.getChildren().add(emptyLabel);
    }

    private void updateSummaryView() {
        Platform.runLater(() -> {
            summaryContainer.getChildren().clear();

            if (selectedSessionId == null || !sessions.containsKey(selectedSessionId)) {
                Label emptyLabel = new Label("No execution selected. Run a workflow to see details.");
                emptyLabel.setStyle("-fx-text-fill: " + COLOR_TIMESTAMP + "; -fx-font-size: 14px;");
                summaryContainer.getChildren().add(emptyLabel);
                return;
            }

            ExecutionSession session = sessions.get(selectedSessionId);

            // Header with workflow name
            HBox header = new HBox(10);
            header.setAlignment(Pos.CENTER_LEFT);
            FontIcon icon = FontIcon.of(MaterialDesignP.PLAY_CIRCLE_OUTLINE, 24, Color.web(COLOR_SUCCESS));
            Label titleLabel = new Label(session.getWorkflowName());
            titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_EXECUTION + ";");
            header.getChildren().addAll(icon, titleLabel);

            // Status card
            VBox statusCard = createCard("Execution Status", createStatusContent(session));

            // Progress card (if running)
            VBox progressCard = null;
            if (!session.isComplete()) {
                progressCard = createCard("Progress", createProgressContent(session));
            }

            // Metrics card
            VBox metricsCard = createCard("Metrics", createMetricsContent(session));

            // Nodes card
            VBox nodesCard = createCard("Node Execution", createNodesContent(session));

            // Add all cards
            summaryContainer.getChildren().add(header);
            summaryContainer.getChildren().add(statusCard);
            if (progressCard != null)
                summaryContainer.getChildren().add(progressCard);
            summaryContainer.getChildren().add(metricsCard);
            summaryContainer.getChildren().add(nodesCard);
        });
    }

    private VBox createCard(String title, Region content) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: " + BG_MEDIUM + "; -fx-background-radius: 8;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_INFO + ";");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: " + BG_LIGHT + ";");

        card.getChildren().addAll(titleLabel, sep, content);
        return card;
    }

    private GridPane createStatusContent(ExecutionSession session) {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(8);

        // Status
        addGridRow(grid, 0, "Status:", createStatusBadge(session));

        // Execution ID
        Label idLabel = new Label(session.getExecutionId());
        idLabel.setStyle("-fx-text-fill: " + COLOR_TRACE + "; -fx-font-family: monospace;");
        addGridRow(grid, 1, "Execution ID:", idLabel);

        // Start Time
        String startTime = DATE_TIME_FORMAT
                .format(LocalDateTime.ofInstant(session.getStartTime(), ZoneId.systemDefault()));
        addGridRow(grid, 2, "Started:", new Label(startTime) {
            {
                setStyle("-fx-text-fill: " + COLOR_EXECUTION + ";");
            }
        });

        // Duration
        String duration = session.isComplete()
                ? formatDuration(session.getDurationMs())
                : formatDuration(Duration.between(session.getStartTime(), Instant.now()).toMillis()) + " (running)";
        addGridRow(grid, 3, "Duration:", new Label(duration) {
            {
                setStyle("-fx-text-fill: " + COLOR_SUCCESS + ";");
            }
        });

        return grid;
    }

    private HBox createStatusBadge(ExecutionSession session) {
        HBox badge = new HBox(5);
        badge.setAlignment(Pos.CENTER_LEFT);
        badge.setPadding(new Insets(4, 10, 4, 10));

        String status, color, iconCode;
        if (!session.isComplete()) {
            status = "RUNNING";
            color = COLOR_INFO;
            iconCode = "sync";
        } else if (session.isSuccess()) {
            status = "COMPLETED";
            color = COLOR_SUCCESS;
            iconCode = "check";
        } else {
            status = "FAILED";
            color = COLOR_ERROR;
            iconCode = "close";
        }

        badge.setStyle("-fx-background-color: " + color + "22; -fx-background-radius: 12;");
        FontIcon statusIcon = FontIcon.of(
                session.isComplete()
                        ? (session.isSuccess() ? MaterialDesignC.CHECK_CIRCLE : MaterialDesignC.CLOSE_CIRCLE)
                        : MaterialDesignS.SYNC,
                14, Color.web(color));
        Label statusLabel = new Label(status);
        statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 11px;");
        badge.getChildren().addAll(statusIcon, statusLabel);
        return badge;
    }

    private VBox createProgressContent(ExecutionSession session) {
        VBox content = new VBox(8);

        int totalNodes = session.getTotalNodes();
        int completedNodes = session.getCompletedNodes();
        double progress = totalNodes > 0 ? (double) completedNodes / totalNodes : 0;

        ProgressBar progressBar = new ProgressBar(progress);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: " + COLOR_INFO + ";");

        Label progressLabel = new Label(
                String.format("%d / %d nodes completed (%.0f%%)", completedNodes, totalNodes, progress * 100));
        progressLabel.setStyle("-fx-text-fill: " + COLOR_EXECUTION + ";");

        content.getChildren().addAll(progressBar, progressLabel);
        return content;
    }

    private GridPane createMetricsContent(ExecutionSession session) {
        GridPane grid = new GridPane();
        grid.setHgap(40);
        grid.setVgap(8);

        int errorCount = (int) session.getEntries().stream().filter(e -> e.type == LogEntryType.ERROR).count();
        int warnCount = (int) session.getEntries().stream().filter(e -> e.type == LogEntryType.WARN).count();
        int nodeCount = session.getTotalNodes();

        addMetricBox(grid, 0, "Total Nodes", String.valueOf(nodeCount), COLOR_NODE, MaterialDesignN.NODEJS);
        addMetricBox(grid, 1, "Completed", String.valueOf(session.getCompletedNodes()), COLOR_SUCCESS,
                MaterialDesignC.CHECK);
        addMetricBox(grid, 2, "Warnings", String.valueOf(warnCount), COLOR_WARN, MaterialDesignA.ALERT);
        addMetricBox(grid, 3, "Errors", String.valueOf(errorCount), COLOR_ERROR, MaterialDesignA.ALERT_CIRCLE);

        return grid;
    }

    private void addMetricBox(GridPane grid, int col, String label, String value, String color,
            org.kordamp.ikonli.Ikon icon) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10, 20, 10, 20));
        box.setStyle("-fx-background-color: " + BG_LIGHT + "; -fx-background-radius: 6;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        Label nameLabel = new Label(label);
        nameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + COLOR_TIMESTAMP + ";");

        box.getChildren().addAll(valueLabel, nameLabel);
        grid.add(box, col, 0);
    }

    private VBox createNodesContent(ExecutionSession session) {
        VBox content = new VBox(5);

        List<LogEntryData> nodeEntries = session.getEntries().stream()
                .filter(e -> e.type == LogEntryType.NODE_START || e.type == LogEntryType.NODE_END
                        || e.type == LogEntryType.ERROR)
                .toList();

        if (nodeEntries.isEmpty()) {
            Label emptyLabel = new Label("No node executions yet...");
            emptyLabel.setStyle("-fx-text-fill: " + COLOR_TIMESTAMP + "; -fx-font-style: italic;");
            content.getChildren().add(emptyLabel);
        } else {
            for (LogEntryData entry : nodeEntries) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(6, 10, 6, 10));
                row.setStyle("-fx-background-color: " + BG_LIGHT + "; -fx-background-radius: 4;");

                FontIcon icon = FontIcon.of(
                        entry.type == LogEntryType.NODE_START ? MaterialDesignP.PLAY
                                : entry.type == LogEntryType.ERROR ? MaterialDesignA.ALERT_CIRCLE
                                        : MaterialDesignC.CHECK,
                        12, Color.web(getColorForType(entry.type)));

                Label timeLabel = new Label(formatTimestamp(entry.timestamp));
                timeLabel.setStyle(
                        "-fx-text-fill: " + COLOR_TIMESTAMP + "; -fx-font-size: 10px; -fx-font-family: monospace;");

                Label msgLabel = new Label(entry.message);
                msgLabel.setStyle("-fx-text-fill: " + COLOR_EXECUTION + ";");
                HBox.setHgrow(msgLabel, Priority.ALWAYS);

                row.getChildren().addAll(icon, timeLabel, msgLabel);
                content.getChildren().add(row);
            }
        }
        return content;
    }

    private void addGridRow(GridPane grid, int row, String label, Region value) {
        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-text-fill: " + COLOR_TIMESTAMP + "; -fx-font-weight: bold;");
        grid.add(labelNode, 0, row);
        grid.add(value, 1, row);
    }

    private String formatDuration(long ms) {
        if (ms < 1000)
            return ms + "ms";
        if (ms < 60000)
            return String.format("%.2fs", ms / 1000.0);
        long mins = ms / 60000;
        long secs = (ms % 60000) / 1000;
        return String.format("%dm %ds", mins, secs);
    }

    private ToolBar createToolbar() {
        ToolBar toolbar = new ToolBar();
        toolbar.setStyle("-fx-background-color: " + BG_MEDIUM + "; -fx-padding: 5;");

        Button clearBtn = createToolButton("Clear", MaterialDesignD.DELETE_OUTLINE, () -> {
            if (!sessions.isEmpty()) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Clear all execution logs?", ButtonType.OK,
                        ButtonType.CANCEL);
                confirm.showAndWait().ifPresent(r -> {
                    if (r == ButtonType.OK)
                        clear();
                });
            }
        });

        Button copyBtn = createToolButton("Copy All", MaterialDesignC.CONTENT_COPY, this::copyToClipboard);

        ToggleButton autoScrollBtn = new ToggleButton();
        autoScrollBtn.setGraphic(FontIcon.of(MaterialDesignA.ARROW_DOWN_BOLD, 16, Color.web(COLOR_EXECUTION)));
        autoScrollBtn.setTooltip(new Tooltip("Auto-scroll"));
        autoScrollBtn.setSelected(autoScroll);
        autoScrollBtn.selectedProperty().addListener((obs, old, val) -> autoScroll = val);
        autoScrollBtn.setStyle("-fx-background-color: transparent;");

        ToggleButton timestampBtn = new ToggleButton();
        timestampBtn.setGraphic(FontIcon.of(MaterialDesignC.CLOCK_OUTLINE, 16, Color.web(COLOR_EXECUTION)));
        timestampBtn.setTooltip(new Tooltip("Show timestamps"));
        timestampBtn.setSelected(showTimestamps);
        timestampBtn.selectedProperty().addListener((obs, old, val) -> {
            showTimestamps = val;
            refreshDisplay();
        });
        timestampBtn.setStyle("-fx-background-color: transparent;");

        // DEBUG toggle with clear visual state
        ToggleButton debugBtn = new ToggleButton("DEBUG");
        debugBtn.setTooltip(new Tooltip("Filter to show only DEBUG level messages"));
        debugBtn.setSelected(showDebug);
        styleToggleButton(debugBtn, COLOR_DEBUG, showDebug);
        debugBtn.selectedProperty().addListener((obs, old, val) -> {
            showDebug = val;
            styleToggleButton(debugBtn, COLOR_DEBUG, val);
            refreshDisplay();
        });

        // TRACE toggle with clear visual state
        ToggleButton traceBtn = new ToggleButton("TRACE");
        traceBtn.setTooltip(new Tooltip("Filter to show only TRACE level messages"));
        traceBtn.setSelected(showTrace);
        styleToggleButton(traceBtn, COLOR_TRACE, showTrace);
        traceBtn.selectedProperty().addListener((obs, old, val) -> {
            showTrace = val;
            styleToggleButton(traceBtn, COLOR_TRACE, val);
            refreshDisplay();
        });

        TextField filterField = new TextField();
        filterField.setPromptText("Filter logs...");
        filterField.setPrefWidth(180);
        filterField.setStyle("-fx-background-color: " + BG_LIGHT + "; -fx-text-fill: " + COLOR_EXECUTION + ";");
        filterField.textProperty().addListener((obs, old, val) -> {
            filterText = val.toLowerCase();
            refreshDisplay();
        });

        sessionSelector.setPromptText("Select session...");
        sessionSelector.setPrefWidth(200);
        sessionSelector.setStyle("-fx-background-color: " + BG_LIGHT + ";");
        sessionSelector.valueProperty().addListener((obs, old, val) -> {
            if (val != null) {
                selectedSessionId = val;
                updateSummaryView();
                refreshDisplay();
            }
        });

        toolbar.getItems().addAll(
                clearBtn, copyBtn, new Separator(Orientation.VERTICAL),
                autoScrollBtn, timestampBtn, new Separator(Orientation.VERTICAL),
                debugBtn, traceBtn, new Separator(Orientation.VERTICAL),
                filterField, new Separator(Orientation.VERTICAL),
                new Label("Session:"), sessionSelector);

        return toolbar;
    }

    private Button createToolButton(String tooltip, org.kordamp.ikonli.Ikon icon, Runnable action) {
        Button btn = new Button();
        btn.setGraphic(FontIcon.of(icon, 16, Color.web(COLOR_EXECUTION)));
        btn.setTooltip(new Tooltip(tooltip));
        btn.setOnAction(e -> action.run());
        btn.setStyle("-fx-background-color: transparent;");
        return btn;
    }

    /**
     * Style a toggle button with clear enabled/disabled visual states.
     * When enabled: solid background with contrasting text, pill shape.
     * When disabled: transparent background with muted text, pill shape border.
     */
    private void styleToggleButton(ToggleButton btn, String color, boolean selected) {
        if (selected) {
            // Enabled state: solid background pill with white text
            btn.setStyle(String.format(
                    "-fx-background-color: %s; " +
                            "-fx-text-fill: %s; " +
                            "-fx-background-radius: 12; " +
                            "-fx-padding: 4 12 4 12; " +
                            "-fx-font-weight: bold; " +
                            "-fx-font-size: 11px; " +
                            "-fx-cursor: hand;",
                    color, BG_DARK));
        } else {
            // Disabled state: transparent with border, muted text
            btn.setStyle(String.format(
                    "-fx-background-color: transparent; " +
                            "-fx-text-fill: %s; " +
                            "-fx-border-color: %s; " +
                            "-fx-border-width: 1; " +
                            "-fx-border-radius: 12; " +
                            "-fx-background-radius: 12; " +
                            "-fx-padding: 3 11 3 11; " +
                            "-fx-font-size: 11px; " +
                            "-fx-opacity: 0.6; " +
                            "-fx-cursor: hand;",
                    color, color));
        }

        // Add hover effect
        btn.setOnMouseEntered(e -> {
            if (!btn.isSelected()) {
                btn.setStyle(String.format(
                        "-fx-background-color: %s22; " +
                                "-fx-text-fill: %s; " +
                                "-fx-border-color: %s; " +
                                "-fx-border-width: 1; " +
                                "-fx-border-radius: 12; " +
                                "-fx-background-radius: 12; " +
                                "-fx-padding: 3 11 3 11; " +
                                "-fx-font-size: 11px; " +
                                "-fx-opacity: 1.0; " +
                                "-fx-cursor: hand;",
                        color, color, color));
            }
        });
        btn.setOnMouseExited(e -> styleToggleButton(btn, color, btn.isSelected()));
    }

    // Labels for log statistics (updated dynamically)
    private final Label infoCountLabel = new Label("0");
    private final Label warnCountLabel = new Label("0");
    private final Label errorCountLabel = new Label("0");
    private boolean showLineNumbers = false;
    private final AtomicInteger logLineNumber = new AtomicInteger(0);

    /**
     * Create the logs tab toolbar with statistics and controls.
     */
    private HBox createLogsToolbar() {
        HBox toolbar = new HBox(8);
        toolbar.setPadding(new Insets(8, 12, 8, 12));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: " + BG_LIGHT + "; -fx-border-color: " + BG_MEDIUM
                + "; -fx-border-width: 0 0 1 0;");

        // Log level statistics
        HBox statsBox = new HBox(12);
        statsBox.setAlignment(Pos.CENTER_LEFT);

        statsBox.getChildren().addAll(
                createStatBadge("ℹ", infoCountLabel, COLOR_INFO),
                createStatBadge("⚠", warnCountLabel, COLOR_WARN),
                createStatBadge("❌", errorCountLabel, COLOR_ERROR));

        // Separator
        Separator sep1 = new Separator(Orientation.VERTICAL);
        sep1.setPadding(new Insets(0, 5, 0, 5));

        // Jump to error button
        Button jumpErrorBtn = new Button("Jump to Error");
        jumpErrorBtn.setGraphic(FontIcon.of(MaterialDesignA.ARROW_DOWN_CIRCLE, 14, Color.web(COLOR_ERROR)));
        jumpErrorBtn
                .setStyle("-fx-background-color: transparent; -fx-text-fill: " + COLOR_ERROR + "; -fx-cursor: hand;");
        jumpErrorBtn.setTooltip(new Tooltip("Scroll to first error"));
        jumpErrorBtn.setOnAction(e -> jumpToFirstError());

        // Line numbers toggle
        ToggleButton lineNumBtn = new ToggleButton("#");
        lineNumBtn.setTooltip(new Tooltip("Show line numbers"));
        lineNumBtn.setSelected(showLineNumbers);
        styleToggleButton(lineNumBtn, COLOR_TIMESTAMP, showLineNumbers);
        lineNumBtn.selectedProperty().addListener((obs, old, val) -> {
            showLineNumbers = val;
            styleToggleButton(lineNumBtn, COLOR_TIMESTAMP, val);
            refreshDisplay();
        });

        // Export button
        Button exportBtn = new Button("Export");
        exportBtn.setGraphic(FontIcon.of(MaterialDesignE.EXPORT, 14, Color.web(COLOR_EXECUTION)));
        exportBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: " + COLOR_EXECUTION + "; -fx-cursor: hand;");
        exportBtn.setTooltip(new Tooltip("Export logs to file"));
        exportBtn.setOnAction(e -> exportLogs());

        // Expand/Collapse all
        Button expandAllBtn = new Button();
        expandAllBtn.setGraphic(FontIcon.of(MaterialDesignC.CHEVRON_DOWN_BOX, 14, Color.web(COLOR_EXECUTION)));
        expandAllBtn.setTooltip(new Tooltip("Expand all details"));
        expandAllBtn.setStyle("-fx-background-color: transparent;");
        expandAllBtn.setOnAction(e -> expandAllDetails(true));

        Button collapseAllBtn = new Button();
        collapseAllBtn.setGraphic(FontIcon.of(MaterialDesignC.CHEVRON_UP_BOX, 14, Color.web(COLOR_EXECUTION)));
        collapseAllBtn.setTooltip(new Tooltip("Collapse all details"));
        collapseAllBtn.setStyle("-fx-background-color: transparent;");
        collapseAllBtn.setOnAction(e -> expandAllDetails(false));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Total entries label
        Label totalLabel = new Label("0 entries");
        totalLabel.setStyle("-fx-text-fill: " + COLOR_TIMESTAMP + "; -fx-font-size: 11px;");

        toolbar.getChildren().addAll(
                new Label("Logs:") {
                    {
                        setStyle("-fx-text-fill: " + COLOR_EXECUTION + "; -fx-font-weight: bold;");
                    }
                },
                statsBox,
                sep1,
                jumpErrorBtn,
                lineNumBtn,
                expandAllBtn,
                collapseAllBtn,
                spacer,
                exportBtn,
                totalLabel);

        return toolbar;
    }

    private HBox createStatBadge(String icon, Label countLabel, String color) {
        HBox badge = new HBox(4);
        badge.setAlignment(Pos.CENTER);
        badge.setPadding(new Insets(2, 6, 2, 6));
        badge.setStyle("-fx-background-color: " + color + "22; -fx-background-radius: 10;");

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 10px;");

        countLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 11px;");

        badge.getChildren().addAll(iconLabel, countLabel);
        return badge;
    }

    private void jumpToFirstError() {
        Platform.runLater(() -> {
            for (javafx.scene.Node node : logContainer.getChildren()) {
                if (node instanceof VBox vbox && vbox.getStyle().contains("rgba(191, 97, 106")) {
                    // Found an error entry
                    logsScrollPane.setVvalue(vbox.getLayoutY() / logContainer.getHeight());
                    vbox.setStyle(vbox.getStyle() + " -fx-border-color: " + COLOR_ERROR
                            + "; -fx-border-width: 2; -fx-border-radius: 4;");
                    // Flash effect
                    new Thread(() -> {
                        try {
                            Thread.sleep(1500);
                        } catch (InterruptedException ignored) {
                        }
                        Platform.runLater(() -> vbox.setStyle("-fx-background-color: rgba(191, 97, 106, 0.1);"));
                    }).start();
                    return;
                }
            }
        });
    }

    private void expandAllDetails(boolean expand) {
        Platform.runLater(() -> {
            for (javafx.scene.Node node : logContainer.getChildren()) {
                if (node instanceof VBox container) {
                    // Each log entry that has details
                    if (expand) {
                        // Find entries with expandable content
                        if (container.getChildren().size() == 1
                                && container.getUserData() instanceof LogEntryData entry) {
                            if (entry.details != null && !entry.details.isEmpty()) {
                                toggleDetails(container, entry);
                            }
                        }
                    } else {
                        // Collapse - remove expanded details
                        if (container.getChildren().size() > 1) {
                            container.getChildren().remove(1, container.getChildren().size());
                        }
                    }
                }
            }
        });
    }

    private void exportLogs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Logs");
        fileChooser.setInitialFileName("execution_logs.txt");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("JSON Files", "*.json"));

        File file = fileChooser.showSaveDialog(this);
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                if (file.getName().endsWith(".json")) {
                    writer.write(exportAsJson());
                } else {
                    writer.write(exportAsText());
                }
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Logs exported successfully!", ButtonType.OK);
                alert.showAndWait();
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to export logs: " + e.getMessage(),
                        ButtonType.OK);
                alert.showAndWait();
            }
        }
    }

    private String exportAsText() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Execution Console Export ===\n");
        sb.append("Exported at: ").append(LocalDateTime.now().format(DATE_TIME_FORMAT)).append("\n\n");

        for (ExecutionSession session : sessions.values()) {
            sb.append("─".repeat(60)).append("\n");
            sb.append("Workflow: ").append(session.getWorkflowName()).append("\n");
            sb.append("Execution ID: ").append(session.getExecutionId()).append("\n");
            sb.append("Status: ")
                    .append(session.isComplete() ? (session.isSuccess() ? "COMPLETED" : "FAILED") : "RUNNING")
                    .append("\n");
            sb.append("─".repeat(60)).append("\n\n");

            int lineNum = 1;
            for (LogEntryData entry : session.getEntries()) {
                sb.append(String.format("%4d | ", lineNum++));
                sb.append(formatTimestamp(entry.timestamp)).append(" ");
                sb.append("  ".repeat(entry.depth));
                sb.append("[").append(entry.type.name()).append("] ");
                sb.append(entry.message);
                if (entry.details != null && !entry.details.isEmpty()) {
                    sb.append("\n      | ").append(entry.details.replace("\n", "\n      | "));
                }
                sb.append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String exportAsJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"exportTime\": \"").append(LocalDateTime.now()).append("\",\n");
        sb.append("  \"sessions\": [\n");

        boolean firstSession = true;
        for (ExecutionSession session : sessions.values()) {
            if (!firstSession)
                sb.append(",\n");
            firstSession = false;

            sb.append("    {\n");
            sb.append("      \"workflowName\": \"").append(escapeJson(session.getWorkflowName())).append("\",\n");
            sb.append("      \"executionId\": \"").append(session.getExecutionId()).append("\",\n");
            sb.append("      \"status\": \"")
                    .append(session.isComplete() ? (session.isSuccess() ? "completed" : "failed") : "running")
                    .append("\",\n");
            sb.append("      \"entries\": [\n");

            boolean firstEntry = true;
            for (LogEntryData entry : session.getEntries()) {
                if (!firstEntry)
                    sb.append(",\n");
                firstEntry = false;

                sb.append("        {");
                sb.append("\"timestamp\": \"").append(entry.timestamp).append("\", ");
                sb.append("\"level\": \"").append(entry.type.name()).append("\", ");
                sb.append("\"message\": \"").append(escapeJson(entry.message)).append("\"");
                if (entry.details != null) {
                    sb.append(", \"details\": \"").append(escapeJson(entry.details)).append("\"");
                }
                sb.append("}");
            }
            sb.append("\n      ]\n    }");
        }
        sb.append("\n  ]\n}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t",
                "\\t");
    }

    private void updateLogStats() {
        if (selectedSessionId == null)
            return;
        ExecutionSession session = sessions.get(selectedSessionId);
        if (session == null)
            return;

        int infoCount = 0, warnCount = 0, errCount = 0;
        for (LogEntryData entry : session.getEntries()) {
            switch (entry.type) {
                case INFO, NODE_START, NODE_END, SUCCESS -> infoCount++;
                case WARN -> warnCount++;
                case ERROR -> errCount++;
                default -> {
                }
            }
        }

        final int finalInfo = infoCount, finalWarn = warnCount, finalErr = errCount;
        Platform.runLater(() -> {
            infoCountLabel.setText(String.valueOf(finalInfo));
            warnCountLabel.setText(String.valueOf(finalWarn));
            errorCountLabel.setText(String.valueOf(finalErr));
        });
    }

    /**
     * Refresh the logs display based on current filters and session selection.
     */
    private void refreshDisplay() {
        Platform.runLater(() -> {
            logContainer.getChildren().clear();
            logLineNumber.set(0); // Reset line counter

            if (selectedSessionId == null) {
                // Show all sessions
                for (ExecutionSession session : sessions.values()) {
                    logContainer.getChildren().add(
                            createExecutionHeader(session.getExecutionId(), session.getWorkflowName(),
                                    session.getStartTime()));
                    for (LogEntryData entry : session.getEntries()) {
                        if (shouldShowEntry(entry)) {
                            logContainer.getChildren().add(createLogEntryView(entry));
                        }
                    }
                }
            } else {
                // Show specific session
                ExecutionSession session = sessions.get(selectedSessionId);
                if (session != null) {
                    logContainer.getChildren().add(
                            createExecutionHeader(session.getExecutionId(), session.getWorkflowName(),
                                    session.getStartTime()));
                    for (LogEntryData entry : session.getEntries()) {
                        if (shouldShowEntry(entry)) {
                            logContainer.getChildren().add(createLogEntryView(entry));
                        }
                    }
                }
            }

            updateLogStats();

            // Update filter status
            int totalFiltered = 0;
            for (javafx.scene.Node node : logContainer.getChildren()) {
                if (node instanceof VBox)
                    totalFiltered++;
            }
            filterStatusLabel.setText(filterText.isEmpty() ? "" : "Filtered: " + totalFiltered);
        });
    }

    private HBox createStatusBar() {
        HBox bar = new HBox(10);
        bar.setPadding(new Insets(5, 10, 5, 10));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: " + BG_MEDIUM + ";");

        Label status = new Label("Ready");
        status.setStyle("-fx-text-fill: " + COLOR_INFO + ";");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        filterStatusLabel.setStyle("-fx-text-fill: " + COLOR_WARN + ";");
        sessionCountLabel.setStyle("-fx-text-fill: " + COLOR_TIMESTAMP + ";");

        bar.getChildren().addAll(status, spacer, filterStatusLabel, sessionCountLabel);
        return bar;
    }

    // ===== Public API =====

    public void startExecution(String executionId, String workflowName) {
        Platform.runLater(() -> {
            ExecutionSession session = new ExecutionSession(executionId, workflowName);
            sessions.put(executionId, session);
            sessionSelector.getItems().add(executionId);
            sessionSelector.setValue(executionId);
            selectedSessionId = executionId;

            HBox header = createExecutionHeader(executionId, workflowName, Instant.now());
            logContainer.getChildren().add(header);
            session.setHeaderIndex(logContainer.getChildren().size() - 1);

            updateSessionCount();
            updateSummaryView();
        });
    }

    public void endExecution(String executionId, boolean success, long durationMs) {
        Platform.runLater(() -> {
            ExecutionSession session = sessions.get(executionId);
            if (session != null) {
                session.setComplete(true);
                session.setSuccess(success);
                session.setDurationMs(durationMs);
                logEntry(executionId, 0, success ? LogEntryType.SUCCESS : LogEntryType.ERROR,
                        success ? "✓ Execution completed" : "✗ Execution failed",
                        String.format("Duration: %s", formatDuration(durationMs)));
                updateSummaryView();
            }
        });
    }

    public void nodeStart(String executionId, String nodeId, String nodeName, String nodeType) {
        Platform.runLater(() -> {
            ExecutionSession session = sessions.get(executionId);
            if (session != null) {
                int depth = session.pushNode(nodeId);
                logEntry(executionId, depth, LogEntryType.NODE_START,
                        "▶ " + nodeName + " (" + nodeType + ")", "Node ID: " + nodeId);
                updateSummaryView();
            }
        });
    }

    public void nodeEnd(String executionId, String nodeId, String nodeName, boolean success, long durationMs) {
        Platform.runLater(() -> {
            ExecutionSession session = sessions.get(executionId);
            if (session != null) {
                int depth = session.getNodeDepth(nodeId);
                logEntry(executionId, depth, success ? LogEntryType.NODE_END : LogEntryType.ERROR,
                        (success ? "✓ " : "✗ ") + nodeName,
                        String.format("Duration: %s", formatDuration(durationMs)));
                session.popNode(nodeId);
                updateSummaryView();
            }
        });
    }

    public void nodeInput(String executionId, String nodeId, String nodeName, Object input) {
        Platform.runLater(() -> {
            ExecutionSession session = sessions.get(executionId);
            if (session != null) {
                int depth = session.getNodeDepth(nodeId);
                logEntry(executionId, depth + 1, LogEntryType.TRACE, "📥 Input: " + nodeName, formatDataPreview(input));
            }
        });
    }

    public void nodeOutput(String executionId, String nodeId, String nodeName, Object output) {
        Platform.runLater(() -> {
            ExecutionSession session = sessions.get(executionId);
            if (session != null) {
                int depth = session.getNodeDepth(nodeId);
                logEntry(executionId, depth + 1, LogEntryType.TRACE, "📤 Output: " + nodeName,
                        formatDataPreview(output));
            }
        });
    }

    public void nodeSkip(String executionId, String nodeId, String nodeName, String reason) {
        Platform.runLater(() -> {
            ExecutionSession session = sessions.get(executionId);
            if (session != null) {
                logEntry(executionId, session.getCurrentDepth(), LogEntryType.DEBUG, "⏭ Skipped: " + nodeName, reason);
            }
        });
    }

    public void error(String executionId, String source, String message, String stackTrace) {
        Platform.runLater(() -> {
            ExecutionSession session = sessions.get(executionId);
            int depth = session != null ? session.getCurrentDepth() : 0;
            logEntry(executionId, depth, LogEntryType.ERROR, "❌ Error in " + source, message);
            if (stackTrace != null && !stackTrace.isEmpty()) {
                logEntry(executionId, depth + 1, LogEntryType.TRACE, "Stack trace:", stackTrace);
            }
            updateSummaryView();
        });
    }

    public void retry(String executionId, String nodeId, int attempt, int maxRetries, long delayMs) {
        Platform.runLater(() -> {
            ExecutionSession session = sessions.get(executionId);
            int depth = session != null ? session.getCurrentDepth() : 0;
            logEntry(executionId, depth, LogEntryType.WARN,
                    String.format("🔄 Retry %d/%d", attempt, maxRetries),
                    String.format("Waiting %dms", delayMs));
        });
    }

    public void rateLimit(String executionId, String bucketId, boolean throttled, long waitMs) {
        Platform.runLater(() -> {
            ExecutionSession session = sessions.get(executionId);
            int depth = session != null ? session.getCurrentDepth() : 0;
            if (throttled) {
                logEntry(executionId, depth, LogEntryType.WARN, "⏱ Rate limited: " + bucketId,
                        "Waited " + waitMs + "ms");
            } else {
                logEntry(executionId, depth, LogEntryType.DEBUG, "⏱ Rate limit passed: " + bucketId, null);
            }
        });
    }

    public void dataFlow(String executionId, String fromNode, String toNode, int dataSize) {
        Platform.runLater(() -> {
            ExecutionSession session = sessions.get(executionId);
            int depth = session != null ? session.getCurrentDepth() : 0;
            logEntry(executionId, depth, LogEntryType.TRACE,
                    String.format("📦 Data: %s → %s", fromNode, toNode), dataSize + " bytes");
        });
    }

    public void info(String executionId, String message, String details) {
        Platform.runLater(() -> {
            ExecutionSession session = sessions.get(executionId);
            int depth = session != null ? session.getCurrentDepth() : 0;
            logEntry(executionId, depth, LogEntryType.INFO, message, details);
        });
    }

    public void debug(String executionId, String message, String details) {
        Platform.runLater(() -> {
            ExecutionSession session = sessions.get(executionId);
            int depth = session != null ? session.getCurrentDepth() : 0;
            logEntry(executionId, depth, LogEntryType.DEBUG, message, details);
        });
    }

    public void clear() {
        Platform.runLater(() -> {
            logContainer.getChildren().clear();
            sessions.clear();
            sessionSelector.getItems().clear();
            selectedSessionId = null;
            updateSessionCount();
            updateSummaryView();
        });
    }

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

    public void addLogListener(ConsoleLogListener listener) {
        listeners.add(listener);
    }

    public void removeLogListener(ConsoleLogListener listener) {
        listeners.remove(listener);
    }

    // ===== Private Methods =====

    private void logEntry(String executionId, int depth, LogEntryType type, String message, String details) {
        ExecutionSession session = sessions.get(executionId);
        LogEntryData data = new LogEntryData(Instant.now(), depth, type, message, details);
        if (session != null)
            session.addEntry(data);
        if (!shouldShowEntry(data))
            return;
        if (selectedSessionId == null || selectedSessionId.equals(executionId)) {
            VBox entryBox = createLogEntryView(data);
            logContainer.getChildren().add(entryBox);
            pendingAutoScroll = true; // Mark that we added a new entry
        }
        for (ConsoleLogListener listener : listeners) {
            listener.onLogEntry(executionId, type, message, details);
        }
    }

    private boolean shouldShowEntry(LogEntryData entry) {
        if (showDebug || showTrace) {
            if (entry.type == LogEntryType.DEBUG && !showDebug)
                return false;
            if (entry.type == LogEntryType.TRACE && !showTrace)
                return false;
            if (entry.type != LogEntryType.DEBUG && entry.type != LogEntryType.TRACE)
                return false;
        }
        if (!filterText.isEmpty()) {
            String searchable = (entry.message + " " + (entry.details != null ? entry.details : "")).toLowerCase();
            if (!searchable.contains(filterText))
                return false;
        }
        return true;
    }

    private HBox createExecutionHeader(String executionId, String workflowName, Instant timestamp) {
        HBox header = new HBox(10);
        header.setPadding(new Insets(10, 12, 10, 12));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: " + BG_MEDIUM + "; -fx-background-radius: 6;");

        FontIcon icon = FontIcon.of(MaterialDesignP.PLAY_CIRCLE, 20, Color.web(COLOR_SUCCESS));
        Text title = new Text("Execution: " + workflowName);
        title.setFont(Font.font("System", FontWeight.BOLD, 14));
        title.setFill(Color.web(COLOR_EXECUTION));
        Text time = new Text(formatTimestamp(timestamp));
        time.setFill(Color.web(COLOR_TIMESTAMP));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Text id = new Text("#" + executionId.substring(0, Math.min(8, executionId.length())));
        id.setFill(Color.web(COLOR_TIMESTAMP));
        id.setFont(Font.font("Monospace", 10));

        header.getChildren().addAll(icon, title, time, spacer, id);
        return header;
    }

    private VBox createLogEntryView(LogEntryData entry) {
        VBox container = new VBox(2);
        container.setPadding(new Insets(4, 10, 4, 10));
        container.setUserData(entry); // Store entry for expand all functionality

        // Alternating row background
        int lineNum = logLineNumber.incrementAndGet();
        if (lineNum % 2 == 0) {
            container.setStyle("-fx-background-color: " + BG_MEDIUM + "33;");
        }

        HBox mainLine = new HBox(6);
        mainLine.setAlignment(Pos.CENTER_LEFT);

        // Line number (if enabled)
        if (showLineNumbers) {
            Label numLabel = new Label(String.format("%4d", lineNum));
            numLabel.setStyle("-fx-text-fill: " + COLOR_TIMESTAMP
                    + "; -fx-font-family: 'Monospace'; -fx-font-size: 10px; -fx-min-width: 35;");
            mainLine.getChildren().add(numLabel);

            Separator sep = new Separator(Orientation.VERTICAL);
            sep.setStyle("-fx-background-color: " + BG_LIGHT + ";");
            mainLine.getChildren().add(sep);
        }

        // Timestamp
        if (showTimestamps) {
            Text timestamp = new Text(formatTimestamp(entry.timestamp) + " ");
            timestamp.setFill(Color.web(COLOR_TIMESTAMP));
            timestamp.setFont(Font.font("Monospace", 10));
            mainLine.getChildren().add(timestamp);
        }

        // Indentation for hierarchy
        if (entry.depth > 0) {
            Region indent = new Region();
            indent.setMinWidth(entry.depth * 16);
            mainLine.getChildren().add(indent);
        }

        // Log level badge (colored pill)
        Label levelBadge = new Label(getLevelBadgeText(entry.type));
        levelBadge.setStyle(String.format(
                "-fx-background-color: %s33; " +
                        "-fx-text-fill: %s; " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 1 6 1 6; " +
                        "-fx-font-size: 10px; " +
                        "-fx-font-weight: bold;",
                getColorForType(entry.type), getColorForType(entry.type)));
        mainLine.getChildren().add(levelBadge);

        // Message text with potential highlighting
        Text msgText = new Text(entry.message);
        msgText.setFill(Color.web(getColorForType(entry.type)));
        msgText.setFont(Font.font("System", 12));
        mainLine.getChildren().add(msgText);

        // Expandable details indicator
        if (entry.details != null && !entry.details.isEmpty()) {
            Label detailsLink = new Label(" ▶");
            detailsLink.setStyle("-fx-text-fill: " + COLOR_TIMESTAMP + "; -fx-cursor: hand;");
            detailsLink.setOnMouseClicked(e -> toggleDetails(container, entry));
            detailsLink.setOnMouseEntered(
                    e -> detailsLink.setStyle("-fx-text-fill: " + COLOR_INFO + "; -fx-cursor: hand;"));
            detailsLink.setOnMouseExited(
                    e -> detailsLink.setStyle("-fx-text-fill: " + COLOR_TIMESTAMP + "; -fx-cursor: hand;"));
            mainLine.getChildren().add(detailsLink);
        }

        container.getChildren().add(mainLine);

        // Error entries get special background
        if (entry.type == LogEntryType.ERROR) {
            container.setStyle("-fx-background-color: rgba(191, 97, 106, 0.1); -fx-border-color: " + COLOR_ERROR
                    + "44; -fx-border-width: 0 0 0 3;");
        } else if (entry.type == LogEntryType.WARN) {
            container.setStyle(
                    container.getStyle() + " -fx-border-color: " + COLOR_WARN + "44; -fx-border-width: 0 0 0 2;");
        }

        // Context menu for right-click
        ContextMenu contextMenu = createLogEntryContextMenu(container, entry);
        container.setOnContextMenuRequested(e -> contextMenu.show(container, e.getScreenX(), e.getScreenY()));

        return container;
    }

    private String getLevelBadgeText(LogEntryType type) {
        return switch (type) {
            case INFO -> "INFO";
            case DEBUG -> "DEBUG";
            case WARN -> "WARN";
            case ERROR -> "ERROR";
            case TRACE -> "TRACE";
            case NODE_START -> "START";
            case NODE_END -> "END";
            case NODE_INPUT -> "INPUT";
            case NODE_OUTPUT -> "OUTPUT";
            case EXPRESSION -> "EXPR";
            case SUCCESS -> "OK";
        };
    }

    private ContextMenu createLogEntryContextMenu(VBox container, LogEntryData entry) {
        ContextMenu menu = new ContextMenu();

        MenuItem copyMsg = new MenuItem("Copy Message");
        copyMsg.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(entry.message);
            Clipboard.getSystemClipboard().setContent(content);
        });

        MenuItem copyAll = new MenuItem("Copy with Details");
        copyAll.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            String text = formatTimestamp(entry.timestamp) + " [" + entry.type + "] " + entry.message;
            if (entry.details != null) {
                text += "\n" + entry.details;
            }
            content.putString(text);
            Clipboard.getSystemClipboard().setContent(content);
        });

        if (entry.details != null && !entry.details.isEmpty()) {
            MenuItem toggleExpand = new MenuItem("Expand Details");
            toggleExpand.setOnAction(e -> toggleDetails(container, entry));
            menu.getItems().add(toggleExpand);
            menu.getItems().add(new SeparatorMenuItem());
        }

        menu.getItems().addAll(copyMsg, copyAll);
        return menu;
    }

    private void toggleDetails(VBox container, LogEntryData entry) {
        if (container.getChildren().size() > 1) {
            container.getChildren().remove(1);
        } else {
            VBox details = new VBox(5);
            details.setPadding(new Insets(8, 20, 8, 40));
            details.setStyle("-fx-background-color: " + BG_LIGHT + "44; -fx-background-radius: 4;");
            Text detailText = new Text(entry.details);
            detailText.setFill(Color.web(COLOR_EXECUTION));
            detailText.setFont(Font.font("Monospace", 11));
            details.getChildren().add(detailText);
            container.getChildren().add(details);
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
            case NODE_INPUT -> "📥 ";
            case NODE_OUTPUT -> "📤 ";
            case EXPRESSION -> "𝑓 ";
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
            case NODE_INPUT, NODE_OUTPUT -> COLOR_DEBUG;
            case EXPRESSION -> COLOR_TRACE;
            case SUCCESS -> COLOR_SUCCESS;
        };
    }

    private String formatTimestamp(Instant timestamp) {
        return LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault()).format(TIME_FORMAT);
    }

    private String formatDataPreview(Object data) {
        if (data == null)
            return "null";
        String str = data.toString();
        return str.length() > 200 ? str.substring(0, 200) + "..." : str;
    }

    private String formatEntryAsText(LogEntryData entry) {
        return formatTimestamp(entry.timestamp) + " " + "  ".repeat(entry.depth) +
                "[" + entry.type.name() + "] " + entry.message +
                (entry.details != null ? " — " + entry.details : "");
    }

    private void updateFilterStatus() {
        StringBuilder status = new StringBuilder();
        if (showDebug)
            status.append("DEBUG ");
        if (showTrace)
            status.append("TRACE ");
        if (!filterText.isEmpty())
            status.append("FILTER: ").append(filterText);
        Platform.runLater(() -> filterStatusLabel.setText(status.toString().trim()));
    }

    private void updateSessionCount() {
        Platform.runLater(() -> sessionCountLabel.setText("Sessions: " + sessions.size()));
    }

    // ===== Inner Classes =====

    public enum LogEntryType {
        INFO, DEBUG, WARN, ERROR, TRACE, NODE_START, NODE_END, NODE_INPUT, NODE_OUTPUT, EXPRESSION, SUCCESS
    }

    public record LogEntryData(Instant timestamp, int depth, LogEntryType type, String message, String details) {
    }

    public interface ConsoleLogListener {
        void onLogEntry(String executionId, LogEntryType type, String message, String details);
    }

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
        private int totalNodes = 0;
        private int completedNodes = 0;

        ExecutionSession(String executionId, String workflowName) {
            this.executionId = executionId;
            this.workflowName = workflowName;
            this.startTime = Instant.now();
        }

        int pushNode(String nodeId) {
            int depth = nodeStack.size();
            nodeStack.add(nodeId);
            nodeDepths.put(nodeId, depth);
            totalNodes++;
            return depth;
        }

        void popNode(String nodeId) {
            nodeStack.remove(nodeId);
            nodeDepths.remove(nodeId);
            completedNodes++;
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

        int getTotalNodes() {
            return totalNodes;
        }

        int getCompletedNodes() {
            return completedNodes;
        }
    }
}
