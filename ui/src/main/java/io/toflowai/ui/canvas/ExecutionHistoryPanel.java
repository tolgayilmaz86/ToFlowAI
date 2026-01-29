package io.toflowai.ui.canvas;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignH;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;

import io.toflowai.common.domain.Execution;
import io.toflowai.common.domain.Execution.NodeExecution;
import io.toflowai.common.enums.ExecutionStatus;
import io.toflowai.common.enums.TriggerType;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

    private final WorkflowCanvas canvas;
    private final TableView<Execution> executionTable;
    private final ObservableList<Execution> executions;
    private final VBox detailsPane;

    public ExecutionHistoryPanel(WorkflowCanvas canvas) {
        this.canvas = canvas;
        this.executions = FXCollections.observableArrayList();

        setSpacing(0);
        setPrefWidth(350);
        setMinWidth(280);
        setMaxWidth(500);
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
        historyIcon.setIconColor(Color.web("#4a9eff"));

        Label title = new Label("Execution History");
        title.getStyleClass().add("execution-history-title");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e5e5e5;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button();
        closeBtn.setGraphic(FontIcon.of(MaterialDesignC.CLOSE, 16));
        closeBtn.getStyleClass().addAll("icon-button", "flat");
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
        toolbar.setStyle("-fx-background-color: #1a1a1a; -fx-border-color: #333333; -fx-border-width: 0 0 1 0;");

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setGraphic(FontIcon.of(MaterialDesignR.REFRESH, 14));
        refreshBtn.getStyleClass().add("flat");
        refreshBtn.setOnAction(e -> refreshExecutions());

        Button clearBtn = new Button("Clear All");
        clearBtn.setGraphic(FontIcon.of(MaterialDesignD.DELETE_OUTLINE, 14));
        clearBtn.getStyleClass().add("flat");
        clearBtn.setOnAction(e -> clearExecutions());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label countLabel = new Label("0 executions");
        countLabel.setStyle("-fx-text-fill: #737373; -fx-font-size: 11px;");
        countLabel.textProperty().bind(
                javafx.beans.binding.Bindings.size(executions).asString().concat(" executions"));

        toolbar.getChildren().addAll(refreshBtn, clearBtn, spacer, countLabel);
        return toolbar;
    }

    @SuppressWarnings("unchecked")
    private TableView<Execution> createExecutionTable() {
        TableView<Execution> table = new TableView<>(executions);
        table.setPlaceholder(new Label("No executions yet"));
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
        timeCol.setPrefWidth(100);
        timeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Instant time, boolean empty) {
                super.updateItem(time, empty);
                if (empty || time == null) {
                    setText(null);
                } else {
                    setText(TIME_FORMATTER.format(time));
                    setStyle("-fx-font-size: 11px;");
                }
            }
        });

        // Duration column
        TableColumn<Execution, Duration> durationCol = new TableColumn<>("Duration");
        durationCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().duration()));
        durationCol.setPrefWidth(70);
        durationCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Duration duration, boolean empty) {
                super.updateItem(duration, empty);
                if (empty || duration == null) {
                    setText(null);
                } else {
                    setText(formatDuration(duration));
                    setStyle("-fx-font-size: 11px; -fx-text-fill: #a3a3a3;");
                }
            }
        });

        // Trigger column
        TableColumn<Execution, TriggerType> triggerCol = new TableColumn<>("Trigger");
        triggerCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().triggerType()));
        triggerCol.setPrefWidth(80);
        triggerCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(TriggerType trigger, boolean empty) {
                super.updateItem(trigger, empty);
                if (empty || trigger == null) {
                    setText(null);
                } else {
                    setText(trigger.getDisplayName());
                    setStyle("-fx-font-size: 11px;");
                }
            }
        });

        table.getColumns().addAll(statusCol, timeCol, durationCol, triggerCol);

        // Selection listener
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showExecutionDetails(newVal);
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
        VBox pane = new VBox(8);
        pane.setPadding(new Insets(12, 16, 12, 16));
        pane.getStyleClass().add("execution-details-pane");
        pane.setStyle("-fx-background-color: #1a1a1a; -fx-border-color: #333333; -fx-border-width: 1 0 0 0;");
        pane.setPrefHeight(200);
        pane.setVisible(false);
        pane.setManaged(false);
        return pane;
    }

    private void showExecutionDetails(Execution execution) {
        detailsPane.getChildren().clear();

        // Header
        Label header = new Label("Execution Details");
        header.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #e5e5e5;");

        // Node executions
        if (execution.nodeExecutions() != null && !execution.nodeExecutions().isEmpty()) {
            VBox nodesBox = new VBox(4);
            Label nodesLabel = new Label("Node Executions:");
            nodesLabel.setStyle("-fx-text-fill: #a3a3a3; -fx-font-size: 11px;");
            nodesBox.getChildren().add(nodesLabel);

            for (NodeExecution nodeExec : execution.nodeExecutions()) {
                HBox nodeRow = createNodeExecutionRow(nodeExec);
                nodesBox.getChildren().add(nodeRow);
            }
            detailsPane.getChildren().addAll(header, nodesBox);
        }

        // Error message
        if (execution.errorMessage() != null && !execution.errorMessage().isEmpty()) {
            Label errorLabel = new Label("Error: " + execution.errorMessage());
            errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");
            errorLabel.setWrapText(true);
            detailsPane.getChildren().add(errorLabel);
        }

        detailsPane.setVisible(true);
        detailsPane.setManaged(true);
    }

    private HBox createNodeExecutionRow(NodeExecution nodeExec) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 8, 4, 8));
        row.setStyle("-fx-background-color: #262626; -fx-background-radius: 4;");

        FontIcon statusIcon = getStatusIcon(nodeExec.status());

        Label nameLabel = new Label(nodeExec.nodeName());
        nameLabel.setStyle("-fx-text-fill: #e5e5e5; -fx-font-size: 11px;");
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        Label durationLabel = new Label(formatDuration(nodeExec.duration()));
        durationLabel.setStyle("-fx-text-fill: #737373; -fx-font-size: 10px;");

        row.getChildren().addAll(statusIcon, nameLabel, durationLabel);
        return row;
    }

    private FontIcon getStatusIcon(ExecutionStatus status) {
        return switch (status) {
            case SUCCESS -> {
                FontIcon icon = FontIcon.of(MaterialDesignC.CHECK_CIRCLE, 14);
                icon.setIconColor(Color.web("#10b981"));
                yield icon;
            }
            case FAILED -> {
                FontIcon icon = FontIcon.of(MaterialDesignA.ALERT_CIRCLE, 14);
                icon.setIconColor(Color.web("#ef4444"));
                yield icon;
            }
            case RUNNING -> {
                FontIcon icon = FontIcon.of(MaterialDesignP.PLAY_CIRCLE, 14);
                icon.setIconColor(Color.web("#3b82f6"));
                yield icon;
            }
            case PENDING -> {
                FontIcon icon = FontIcon.of(MaterialDesignC.CLOCK_OUTLINE, 14);
                icon.setIconColor(Color.web("#6b7280"));
                yield icon;
            }
            case CANCELLED -> {
                FontIcon icon = FontIcon.of(MaterialDesignC.CANCEL, 14);
                icon.setIconColor(Color.web("#f59e0b"));
                yield icon;
            }
            case WAITING -> {
                FontIcon icon = FontIcon.of(MaterialDesignP.PAUSE_CIRCLE, 14);
                icon.setIconColor(Color.web("#8b5cf6"));
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
            return String.format("%.1fs", millis / 1000.0);
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
