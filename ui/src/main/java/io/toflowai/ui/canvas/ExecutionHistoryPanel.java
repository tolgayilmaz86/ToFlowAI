package io.toflowai.ui.canvas;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignH;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import io.toflowai.common.domain.Execution;
import io.toflowai.common.domain.Execution.NodeExecution;
import io.toflowai.common.enums.ExecutionStatus;
import io.toflowai.common.enums.TriggerType;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Panel showing execution history for workflows.
 * Displays a table of past executions with details.
 */
public class ExecutionHistoryPanel extends VBox {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM d, HH:mm:ss")
            .withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DURATION_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final WorkflowCanvas canvas;
    private final TableView<Execution> executionTable;
    private final ObservableList<Execution> executions;
    private final VBox detailsPane;
    private final ObjectMapper objectMapper;

    public ExecutionHistoryPanel(WorkflowCanvas canvas) {
        this.canvas = canvas;
        this.executions = FXCollections.observableArrayList();
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

        setSpacing(0);
        setPrefWidth(450);
        setMinWidth(350);
        setMaxWidth(600);
        getStyleClass().add("execution-history-panel");
        setVisible(false);
        setManaged(false);

        // Header
        HBox header = createHeader();

        // Toolbar with refresh and clear
        HBox toolbar = createToolbar();

        // Execution table
        executionTable = createExecutionTable();
        VBox.setVgrow(executionTable, Priority.ALWAYS);

        // Details pane (shown when execution selected)
        detailsPane = createDetailsPane();

        getChildren().addAll(header, toolbar, executionTable, detailsPane);
    }

    private HBox createHeader() {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 16, 12, 16));
        header.getStyleClass().add("execution-history-header");

        FontIcon historyIcon = FontIcon.of(MaterialDesignH.HISTORY, 18);
        historyIcon.setIconColor(Color.web("#88c0d0"));

        Label title = new Label("Execution History");
        title.getStyleClass().add("title-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button();
        closeBtn.setGraphic(FontIcon.of(MaterialDesignC.CLOSE, 16));
        closeBtn.getStyleClass().addAll("flat-button");
        closeBtn.setTooltip(new Tooltip("Close"));
        closeBtn.setOnAction(e -> hide());

        header.getChildren().addAll(historyIcon, title, spacer, closeBtn);
        return header;
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(8);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(8, 16, 8, 16));
        toolbar.getStyleClass().add("execution-history-toolbar");

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setGraphic(FontIcon.of(MaterialDesignR.REFRESH, 14));
        refreshBtn.getStyleClass().add("flat-button");
        refreshBtn.setOnAction(e -> refreshExecutions());

        Button clearBtn = new Button("Clear All");
        clearBtn.setGraphic(FontIcon.of(MaterialDesignD.DELETE_OUTLINE, 14));
        clearBtn.getStyleClass().add("flat-button");
        clearBtn.setOnAction(e -> clearExecutions());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label countLabel = new Label();
        countLabel.getStyleClass().add("count-label");
        countLabel.textProperty().bind(
                javafx.beans.binding.Bindings.size(executions).asString().concat(" executions"));

        toolbar.getChildren().addAll(refreshBtn, clearBtn, spacer, countLabel);
        return toolbar;
    }

    @SuppressWarnings("unchecked")
    private TableView<Execution> createExecutionTable() {
        TableView<Execution> table = new TableView<>(executions);
        table.setPlaceholder(new Label("No executions yet. Run a workflow to see its history."));
        table.getStyleClass().add("execution-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // Status column with icon
        TableColumn<Execution, ExecutionStatus> statusCol = new TableColumn<>("");
        statusCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().status()));
        statusCol.setPrefWidth(32);
        statusCol.setMaxWidth(40);
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(ExecutionStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    FontIcon icon = getStatusIcon(status);
                    setGraphic(icon);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // Time column
        TableColumn<Execution, Instant> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().startedAt()));
        timeCol.setPrefWidth(120);
        timeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Instant time, boolean empty) {
                super.updateItem(time, empty);
                setText(empty || time == null ? null : TIME_FORMATTER.format(time));
            }
        });

        // Duration column
        TableColumn<Execution, Duration> durationCol = new TableColumn<>("Duration");
        durationCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().duration()));
        durationCol.setPrefWidth(80);
        durationCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Duration duration, boolean empty) {
                super.updateItem(duration, empty);
                if (empty || duration == null) {
                    setText(null);
                } else {
                    setText(formatDuration(duration));
                    getStyleClass().add("duration-cell");
                }
            }
        });

        // Trigger column
        TableColumn<Execution, TriggerType> triggerCol = new TableColumn<>("Trigger");
        triggerCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().triggerType()));
        triggerCol.setPrefWidth(90);
        triggerCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(TriggerType trigger, boolean empty) {
                super.updateItem(trigger, empty);
                setText(empty || trigger == null ? null : trigger.getDisplayName());
            }
        });

        table.getColumns().addAll(statusCol, timeCol, durationCol, triggerCol);

        // Selection listener
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showExecutionDetails(newVal);
            } else {
                detailsPane.setVisible(false);
                detailsPane.setManaged(false);
            }
        });

        // Double-click to show on canvas
        table.setRowFactory(tv -> {
            TableRow<Execution> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    highlightExecutionOnCanvas(row.getItem());
                }
            });
            return row;
        });

        return table;
    }

    private VBox createDetailsPane() {
        VBox pane = new VBox(0);
        pane.getStyleClass().add("execution-details-pane");
        pane.setVisible(false);
        pane.setManaged(false);
        VBox.setVgrow(pane, Priority.ALWAYS);
        return pane;
    }

    private void showExecutionDetails(Execution execution) {
        detailsPane.getChildren().clear();

        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("details-tab-pane");

        // Summary Tab
        Tab summaryTab = new Tab("Summary");
        summaryTab.setContent(createSummaryView(execution));
        summaryTab.setClosable(false);

        // Nodes Tab
        Tab nodesTab = new Tab("Nodes");
        nodesTab.setContent(createNodesView(execution.nodeExecutions()));
        nodesTab.setClosable(false);

        tabPane.getTabs().addAll(summaryTab, nodesTab);

        // Error Tab (conditional)
        if (execution.status() == ExecutionStatus.FAILED && execution.errorMessage() != null) {
            Tab errorTab = new Tab("Error");
            errorTab.setContent(createErrorView(execution.errorMessage()));
            errorTab.setClosable(false);
            tabPane.getTabs().add(errorTab);
            tabPane.getSelectionModel().select(errorTab);
        }

        // Timeline and Variables Tabs
        Tab timelineTab = new Tab("Timeline");
        timelineTab.setContent(createTimelineView(execution));
        timelineTab.setClosable(false);

        Tab variablesTab = new Tab("Variables");
        variablesTab.setContent(createVariablesView(execution.outputData()));
        variablesTab.setClosable(false);

        tabPane.getTabs().addAll(timelineTab, variablesTab);

        detailsPane.getChildren().add(tabPane);
        detailsPane.setVisible(true);
        detailsPane.setManaged(true);
    }

    private Node createVariablesView(Map<String, Object> outputData) {
        TableView<Map.Entry<String, Object>> table = new TableView<>();
        table.getStyleClass().add("variables-table");
        
        if (outputData == null || outputData.isEmpty()) {
            table.setPlaceholder(new Label("No output variables."));
            return table;
        }

        ObservableList<Map.Entry<String, Object>> items = FXCollections.observableArrayList(outputData.entrySet());
        table.setItems(items);

        TableColumn<Map.Entry<String, Object>, String> varCol = new TableColumn<>("Variable");
        varCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getKey()));
        varCol.setPrefWidth(120);

        TableColumn<Map.Entry<String, Object>, String> valCol = new TableColumn<>("Value");
        valCol.setCellValueFactory(p -> new SimpleStringProperty(formatData(p.getValue().getValue())));
        valCol.setPrefWidth(280);

        table.getColumns().addAll(varCol, valCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        return table;
    }
    
    private Node createTimelineView(Execution execution) {
        VBox timelineContainer = new VBox(10);
        timelineContainer.setPadding(new Insets(10));
        timelineContainer.getStyleClass().add("timeline-view");

        List<NodeExecution> nodeExecutions = execution.nodeExecutions();
        if (nodeExecutions == null || nodeExecutions.isEmpty()) {
            timelineContainer.getChildren().add(new Label("No node executions to visualize."));
            return timelineContainer;
        }

        long totalDuration = execution.duration().toMillis();
        if (totalDuration == 0) {
            timelineContainer.getChildren().add(new Label("Total execution time was zero."));
            return timelineContainer;
        }

        final double timelineWidth = 350; // The width of the timeline area
        final double rowHeight = 25;

        for (NodeExecution nodeExec : nodeExecutions) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);

            Label nameLabel = new Label(nodeExec.nodeName());
            nameLabel.setPrefWidth(100);
            nameLabel.getStyleClass().add("timeline-node-label");

            Pane timelinePane = new Pane();
            timelinePane.setPrefHeight(rowHeight);
            HBox.setHgrow(timelinePane, Priority.ALWAYS);

            long nodeStartOffset = Duration.between(execution.startedAt(), nodeExec.startedAt()).toMillis();
            long nodeDuration = nodeExec.duration().toMillis();

            double barX = (double) nodeStartOffset / totalDuration * timelineWidth;
            double barWidth = (double) nodeDuration / totalDuration * timelineWidth;

            Rectangle bar = new Rectangle(barX, 0, Math.max(2, barWidth), rowHeight);
            bar.getStyleClass().add("timeline-bar");
            
            // Set color based on status
            String statusClass = "status-" + nodeExec.status().name().toLowerCase();
            bar.getStyleClass().add(statusClass);


            Tooltip tooltip = new Tooltip(String.format("%s\nStart: %dms\nDuration: %dms",
                    nodeExec.nodeName(), nodeStartOffset, nodeDuration));
            Tooltip.install(bar, tooltip);

            timelinePane.getChildren().add(bar);
            row.getChildren().addAll(nameLabel, timelinePane);
            timelineContainer.getChildren().add(row);
        }

        return timelineContainer;
    }
    
    private Node createSummaryView(Execution execution) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(16));
        grid.getStyleClass().add("summary-grid");

        // Rows
        grid.add(createLabel("Status:"), 0, 0);
        grid.add(createStatusBadge(execution.status()), 1, 0);

        grid.add(createLabel("Workflow:"), 0, 1);
        grid.add(new Label(execution.workflowName()), 1, 1);

        grid.add(createLabel("Trigger:"), 0, 2);
        grid.add(new Label(execution.triggerType().getDisplayName()), 1, 2);

        grid.add(createLabel("Started:"), 0, 3);
        grid.add(new Label(TIME_FORMATTER.format(execution.startedAt())), 1, 3);

        grid.add(createLabel("Finished:"), 0, 4);
        grid.add(new Label(execution.finishedAt() != null ? TIME_FORMATTER.format(execution.finishedAt()) : "-"), 1, 4);

        grid.add(createLabel("Duration:"), 0, 5);
        grid.add(new Label(formatDuration(execution.duration())), 1, 5);

        grid.add(createLabel("Execution ID:"), 0, 6);
        Label idLabel = new Label(execution.id().toString());
        idLabel.getStyleClass().add("code");
        grid.add(idLabel, 1, 6);


        return grid;
    }

    private Node createNodesView(List<NodeExecution> nodeExecutions) {
        VBox nodesBox = new VBox(5);
        nodesBox.setPadding(new Insets(10));
        nodesBox.getStyleClass().add("nodes-view");

        if (nodeExecutions == null || nodeExecutions.isEmpty()) {
            nodesBox.getChildren().add(new Label("No node executions recorded."));
            return nodesBox;
        }

        for (NodeExecution nodeExec : nodeExecutions) {
            nodesBox.getChildren().add(createNodeExecutionRow(nodeExec));
        }

        return nodesBox;
    }

    private Node createErrorView(String errorMessage) {
        VBox errorBox = new VBox(5);
        errorBox.setPadding(new Insets(10));
        errorBox.getStyleClass().add("error-view");

        Label title = new Label("Execution Failed");
        title.getStyleClass().add("error-title");

        TextArea errorText = new TextArea(errorMessage);
        errorText.setWrapText(true);
        errorText.setEditable(false);
        errorText.getStyleClass().add("error-textarea");
        VBox.setVgrow(errorText, Priority.ALWAYS);

        errorBox.getChildren().addAll(title, errorText);
        return errorBox;
    }


    private Label createLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("summary-label");
        return label;
    }

    private HBox createStatusBadge(ExecutionStatus status) {
        HBox badge = new HBox(5);
        badge.setAlignment(Pos.CENTER_LEFT);
        badge.getStyleClass().addAll("status-badge", "status-" + status.name().toLowerCase());

        FontIcon icon = getStatusIcon(status);
        Label label = new Label(status.name());

        badge.getChildren().addAll(icon, label);
        return badge;
    }


    private VBox createNodeExecutionRow(NodeExecution nodeExec) {
        VBox container = new VBox();
        HBox mainRow = new HBox(8);
        mainRow.setAlignment(Pos.CENTER_LEFT);
        mainRow.setPadding(new Insets(6, 10, 6, 10));
        mainRow.getStyleClass().add("node-execution-row");

        FontIcon statusIcon = getStatusIcon(nodeExec.status());

        Label nameLabel = new Label(nodeExec.nodeName());
        nameLabel.getStyleClass().add("node-name-label");
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        Label durationLabel = new Label(formatDuration(nodeExec.duration()));
        durationLabel.getStyleClass().add("duration-label");

        FontIcon expandIcon = FontIcon.of(MaterialDesignM.MENU_DOWN, 16);
        expandIcon.getStyleClass().add("expand-icon");

        mainRow.getChildren().addAll(statusIcon, nameLabel, durationLabel, expandIcon);
        
        VBox detailsView = createNodeDetailsView(nodeExec);
        detailsView.setVisible(false);
        detailsView.setManaged(false);

        mainRow.setOnMouseClicked(e -> {
            boolean isVisible = detailsView.isVisible();
            detailsView.setVisible(!isVisible);
            detailsView.setManaged(!isVisible);
            expandIcon.setRotate(isVisible ? 0 : 180);
        });
        
        container.getChildren().addAll(mainRow, detailsView);
        return container;
    }

    private VBox createNodeDetailsView(NodeExecution nodeExec) {
        VBox details = new VBox(10);
        details.setPadding(new Insets(10, 10, 10, 30));
        details.getStyleClass().add("node-details-view");

        // Input Data
        details.getChildren().add(createDataSection("Input", nodeExec.inputData()));

        // Output Data
        details.getChildren().add(createDataSection("Output", nodeExec.outputData()));

        return details;
    }

    private VBox createDataSection(String title, Object data) {
        VBox section = new VBox(5);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("data-section-title");

        TextArea dataArea = new TextArea(formatData(data));
        dataArea.setEditable(false);
        dataArea.setWrapText(true);
        dataArea.getStyleClass().add("data-textarea");
        dataArea.setPrefRowCount(5);

        section.getChildren().addAll(titleLabel, dataArea);
        return section;
    }
    
    private String formatData(Object data) {
        if (data == null) {
            return "null";
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "Error formatting data: " + e.getMessage();
        }
    }

    private FontIcon getStatusIcon(ExecutionStatus status) {
        return switch (status) {
            case SUCCESS -> {
                FontIcon icon = FontIcon.of(MaterialDesignC.CHECK_CIRCLE_OUTLINE, 16);
                icon.getStyleClass().add("status-icon-success");
                yield icon;
            }
            case FAILED -> {
                FontIcon icon = FontIcon.of(MaterialDesignA.ALERT_CIRCLE_OUTLINE, 16);
                icon.getStyleClass().add("status-icon-failed");
                yield icon;
            }
            case RUNNING -> {
                FontIcon icon = FontIcon.of(MaterialDesignP.PLAY_CIRCLE_OUTLINE, 16);
                icon.getStyleClass().add("status-icon-running");
                yield icon;
            }
            case PENDING -> {
                FontIcon icon = FontIcon.of(MaterialDesignC.CLOCK_OUTLINE, 16);
                icon.getStyleClass().add("status-icon-pending");
                yield icon;
            }
            case CANCELLED -> {
                FontIcon icon = FontIcon.of(MaterialDesignC.CANCEL, 16);
                icon.getStyleClass().add("status-icon-cancelled");
                yield icon;
            }
            case WAITING -> {
                FontIcon icon = FontIcon.of(MaterialDesignP.PAUSE_CIRCLE_OUTLINE, 16);
                icon.getStyleClass().add("status-icon-waiting");
                yield icon;
            }
        };
    }

    private String formatDuration(Duration duration) {
        if (duration == null)
            return "-";

        long millis = duration.toMillis();
        if (millis < 1000) {
            return millis + "ms";
        } else if (millis < 60000) {
            return String.format("%.2fs", millis / 1000.0);
        } else {
            long mins = millis / 60000;
            long secs = (millis % 60000) / 1000;
            return String.format("%dm %ds", mins, secs);
        }
    }

    private void refreshExecutions() {
        // Get current workflow
        var workflow = canvas.getWorkflow();
        if (workflow.id() != null) {
            try {
                // Load executions from service
                var executionDTOs = canvas.getExecutionService().findByWorkflowId(workflow.id());

                // Convert DTOs to domain objects
                executions.clear();
                for (var dto : executionDTOs) {
                    var nodeExecutions = dto.nodeExecutions().stream()
                            .map(nodeDto -> new Execution.NodeExecution(
                                    nodeDto.nodeId(),
                                    nodeDto.nodeName(),
                                    nodeDto.nodeType(),
                                    nodeDto.status(),
                                    nodeDto.startedAt(),
                                    nodeDto.finishedAt(),
                                    nodeDto.inputData(),
                                    nodeDto.outputData(),
                                    nodeDto.errorMessage()))
                            .toList();

                    var execution = new Execution(
                            dto.id(),
                            dto.workflowId(),
                            dto.workflowName(),
                            dto.status(),
                            dto.triggerType(),
                            dto.startedAt(),
                            dto.finishedAt(),
                            dto.inputData(),
                            dto.outputData(),
                            dto.errorMessage(),
                            nodeExecutions);

                    executions.add(execution);
                }

                // Sort by start time (newest first)
                executions.sort((a, b) -> b.startedAt().compareTo(a.startedAt()));

            } catch (Exception e) {
                System.err.println("Failed to load executions: " + e.getMessage());
                // Keep existing executions on error
            }
        }

        executionTable.refresh();
    }

    private void clearExecutions() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear Execution History");
        confirm.setHeaderText("Clear all execution history?");
        confirm.setContentText("This action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                executions.clear();
                detailsPane.setVisible(false);
                detailsPane.setManaged(false);
            }
        });
    }

    private void highlightExecutionOnCanvas(Execution execution) {
        // Reset all node states first
        canvas.resetAllNodeExecutionStates();

        // Highlight nodes based on their execution status
        if (execution.nodeExecutions() != null) {
            for (NodeExecution nodeExec : execution.nodeExecutions()) {
                NodeView nodeView = canvas.getNodeViewById(nodeExec.nodeId());
                if (nodeView != null) {
                    NodeView.ExecutionState state = switch (nodeExec.status()) {
                        case SUCCESS -> NodeView.ExecutionState.SUCCESS;
                        case FAILED -> NodeView.ExecutionState.ERROR;
                        case RUNNING, WAITING -> NodeView.ExecutionState.RUNNING;
                        case PENDING -> NodeView.ExecutionState.QUEUED;
                        case CANCELLED -> NodeView.ExecutionState.SKIPPED;
                    };
                    nodeView.setExecutionState(state);
                }
            }
        }
    }

    /**
     * Add an execution to the history.
     */
    public void addExecution(Execution execution) {
        // Add at the beginning (newest first)
        executions.addFirst(execution);

        // Limit history size
        if (executions.size() > 100) {
            executions.removeLast();
        }
    }

    /**
     * Add sample executions for demo purposes.
     */
    public void addSampleExecutions() {
        // Sample execution 1: Success
        Execution exec1 = new Execution(
                1L, 1L, "Sample Workflow", ExecutionStatus.SUCCESS, TriggerType.MANUAL,
                Instant.now().minusSeconds(300), Instant.now().minusSeconds(295),
                Map.of(), Map.of("result", "ok"), null,
                List.of(
                        new NodeExecution("1", "HTTP Request", "httpRequest", ExecutionStatus.SUCCESS,
                                Instant.now().minusSeconds(300), Instant.now().minusSeconds(298), Map.of(), Map.of(),
                                null),
                        new NodeExecution("2", "Transform Data", "code", ExecutionStatus.SUCCESS,
                                Instant.now().minusSeconds(298), Instant.now().minusSeconds(295), Map.of(), Map.of(),
                                null)));

        // Sample execution 2: Failed
        Execution exec2 = new Execution(
                2L, 1L, "Sample Workflow", ExecutionStatus.FAILED, TriggerType.SCHEDULE,
                Instant.now().minusSeconds(120), Instant.now().minusSeconds(118),
                Map.of(), Map.of(), "Connection timeout",
                List.of(
                        new NodeExecution("1", "HTTP Request", "httpRequest", ExecutionStatus.FAILED,
                                Instant.now().minusSeconds(120), Instant.now().minusSeconds(118), Map.of(), Map.of(),
                                "Timeout")));

        executions.addAll(exec1, exec2);
    }

    /**
     * Show the history panel.
     */
    public void show() {
        setVisible(true);
        setManaged(true);
    }

    /**
     * Hide the history panel.
     */
    public void hide() {
        setVisible(false);
        setManaged(false);
    }

    /**
     * Toggle visibility.
     */
    public void toggle() {
        if (isVisible()) {
            hide();
        } else {
            show();
        }
    }
}
