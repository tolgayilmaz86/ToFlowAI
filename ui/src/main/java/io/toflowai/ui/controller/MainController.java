package io.toflowai.ui.controller;

import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;
import org.springframework.stereotype.Component;

import io.toflowai.common.dto.ExecutionDTO;
import io.toflowai.common.dto.WorkflowDTO;
import io.toflowai.common.service.CredentialServiceInterface;
import io.toflowai.common.service.ExecutionServiceInterface;
import io.toflowai.common.service.SettingsServiceInterface;
import io.toflowai.common.service.WorkflowServiceInterface;
import io.toflowai.ui.canvas.WorkflowCanvas;
import io.toflowai.ui.console.ExecutionConsoleService;
import io.toflowai.ui.dialog.CredentialManagerDialog;
import io.toflowai.ui.dialog.SettingsDialog;
import io.toflowai.ui.dialog.WorkflowListDialog;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import net.rgielen.fxweaver.core.FxmlView;

/**
 * Main application controller.
 * Manages the primary UI layout and navigation.
 */
@Component
@FxmlView("Main.fxml")
public class MainController implements Initializable {

    private final WorkflowServiceInterface workflowService;
    private final ExecutionServiceInterface executionService;
    private final CredentialServiceInterface credentialService;
    private final SettingsServiceInterface settingsService;

    @FXML
    private BorderPane rootPane;

    @FXML
    private VBox sidebarNav;

    @FXML
    private StackPane contentArea;

    @FXML
    private Label statusLabel;

    @FXML
    private Button btnWorkflows;

    @FXML
    private Button btnExecutions;

    @FXML
    private Button btnCredentials;

    @FXML
    private Button btnSettings;

    private WorkflowCanvas workflowCanvas;

    public MainController(WorkflowServiceInterface workflowService,
            ExecutionServiceInterface executionService,
            CredentialServiceInterface credentialService,
            SettingsServiceInterface settingsService) {
        this.workflowService = workflowService;
        this.executionService = executionService;
        this.credentialService = credentialService;
        this.settingsService = settingsService;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupSidebarIcons();
        setupSidebarActions();
        showWorkflowsView();
        updateStatus("Ready");
    }

    private void setupSidebarIcons() {
        btnWorkflows.setGraphic(new FontIcon(MaterialDesignF.FILE_TREE));
        btnExecutions.setGraphic(new FontIcon(MaterialDesignP.PLAY_CIRCLE_OUTLINE));
        btnCredentials.setGraphic(new FontIcon(MaterialDesignC.CREDIT_CARD_OUTLINE));
        btnSettings.setGraphic(new FontIcon(MaterialDesignC.COG_OUTLINE));
    }

    private void setupSidebarActions() {
        btnWorkflows.setOnAction(e -> showWorkflowsView());
        btnExecutions.setOnAction(e -> showExecutionsView());
        btnCredentials.setOnAction(e -> showCredentialsView());
        btnSettings.setOnAction(e -> showSettingsView());
    }

    @FXML
    private void showWorkflowsView() {
        clearActiveButton();
        btnWorkflows.getStyleClass().add("active");

        // Create workflow editor view with injected services
        workflowCanvas = new WorkflowCanvas(workflowService, executionService);
        contentArea.getChildren().setAll(workflowCanvas);

        updateStatus("Workflows");
    }

    @FXML
    private void showExecutionsView() {
        clearActiveButton();
        btnExecutions.getStyleClass().add("active");

        // Create executions list view
        VBox executionsView = createExecutionsView();
        contentArea.getChildren().setAll(executionsView);

        updateStatus("Executions");
    }

    private VBox createExecutionsView() {
        VBox container = new VBox(15);
        container.setPadding(new Insets(20));
        container.setStyle("-fx-background-color: #1e1e1e;");

        // Header
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        FontIcon icon = FontIcon.of(MaterialDesignP.PLAY_CIRCLE, 28);
        icon.setIconColor(Color.web("#4ade80"));

        VBox titleBox = new VBox(2);
        Label title = new Label("Workflow Executions");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e5e5e5;");
        Label subtitle = new Label("Monitor and review workflow execution history");
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #a3a3a3;");
        titleBox.getChildren().addAll(title, subtitle);

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setGraphic(FontIcon.of(MaterialDesignR.REFRESH, 14));
        refreshBtn.setOnAction(e -> refreshExecutions(container));

        header.getChildren().addAll(icon, titleBox);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        header.getChildren().add(refreshBtn);

        // Executions table
        TableView<ExecutionDTO> executionsTable = createExecutionsTable();

        // Details panel (initially hidden)
        VBox detailsPanel = createExecutionDetailsPanel();
        detailsPanel.setVisible(false);
        detailsPanel.setManaged(false);

        // Connect table selection to details panel
        executionsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                showExecutionDetails(detailsPanel, newSelection);
                detailsPanel.setVisible(true);
                detailsPanel.setManaged(true);
            } else {
                detailsPanel.setVisible(false);
                detailsPanel.setManaged(false);
            }
        });

        container.getChildren().addAll(header, executionsTable, detailsPanel);
        VBox.setVgrow(executionsTable, Priority.ALWAYS);

        // Load initial data
        refreshExecutions(container);

        return container;
    }

    private TableView<ExecutionDTO> createExecutionsTable() {
        TableView<ExecutionDTO> table = new TableView<>();
        table.setStyle("-fx-background-color: #2d2d2d; -fx-border-color: #404040;");
        table.setPlaceholder(new Label("No executions found"));

        // Workflow Name column
        TableColumn<ExecutionDTO, String> workflowCol = new TableColumn<>("Workflow");
        workflowCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().workflowName()));
        workflowCol.setPrefWidth(200);

        // Status column
        TableColumn<ExecutionDTO, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status().toString()));
        statusCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    switch (status) {
                        case "COMPLETED" -> setStyle("-fx-text-fill: #4ade80;");
                        case "FAILED" -> setStyle("-fx-text-fill: #ef4444;");
                        case "RUNNING" -> setStyle("-fx-text-fill: #fbbf24;");
                        default -> setStyle("-fx-text-fill: #a3a3a3;");
                    }
                }
            }
        });
        statusCol.setPrefWidth(100);

        // Started column
        TableColumn<ExecutionDTO, String> startedCol = new TableColumn<>("Started");
        startedCol.setCellValueFactory(data -> {
            Instant started = data.getValue().startedAt();
            return new SimpleStringProperty(started != null ? DateTimeFormatter.ofPattern("MMM d, HH:mm").format(
                    LocalDateTime.ofInstant(started, ZoneId.systemDefault())) : "N/A");
        });
        startedCol.setPrefWidth(120);

        // Duration column
        TableColumn<ExecutionDTO, String> durationCol = new TableColumn<>("Duration");
        durationCol.setCellValueFactory(data -> {
            Long durationMs = data.getValue().durationMs();
            if (durationMs == null)
                return new SimpleStringProperty("N/A");
            if (durationMs < 1000)
                return new SimpleStringProperty(durationMs + "ms");
            return new SimpleStringProperty(String.format("%.1fs", durationMs / 1000.0));
        });
        durationCol.setPrefWidth(100);

        // Trigger column
        TableColumn<ExecutionDTO, String> triggerCol = new TableColumn<>("Trigger");
        triggerCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().triggerType().toString()));
        triggerCol.setPrefWidth(80);

        table.getColumns().addAll(workflowCol, statusCol, startedCol, durationCol, triggerCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        return table;
    }

    private void refreshExecutions(VBox container) {
        try {
            List<ExecutionDTO> executions = executionService.findAll();
            TableView<ExecutionDTO> table = (TableView<ExecutionDTO>) container.getChildren().get(1);
            table.getItems().clear();
            table.getItems().addAll(executions);
        } catch (Exception e) {
            // Show error if loading fails
            Label errorLabel = new Label("Failed to load executions: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: #ef4444;");
            container.getChildren().set(1, errorLabel);
        }
    }

    private VBox createExecutionDetailsPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(15));
        panel.setStyle("-fx-background-color: #2d2d2d; -fx-border-color: #404040; -fx-border-width: 1;");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        FontIcon detailsIcon = FontIcon.of(MaterialDesignF.FILE_DOCUMENT, 20);
        detailsIcon.setIconColor(Color.web("#60a5fa"));

        Label detailsTitle = new Label("Execution Details");
        detailsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e5e5e5;");

        header.getChildren().addAll(detailsIcon, detailsTitle);

        // Content area with scroll
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #2d2d2d;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox content = new VBox(15);
        content.setPadding(new Insets(10));
        content.setStyle("-fx-background-color: #2d2d2d;");
        scrollPane.setContent(content);

        panel.getChildren().addAll(header, scrollPane);
        VBox.setVgrow(panel, Priority.ALWAYS);

        return panel;
    }

    private void showExecutionDetails(VBox detailsPanel, ExecutionDTO execution) {
        ScrollPane scrollPane = (ScrollPane) detailsPanel.getChildren().get(1);
        VBox content = (VBox) scrollPane.getContent();
        content.getChildren().clear();

        // Execution summary
        VBox summaryBox = new VBox(8);
        summaryBox.setPadding(new Insets(10));
        summaryBox.setStyle("-fx-background-color: #1e1e1e; -fx-border-radius: 5; -fx-background-radius: 5;");

        Label summaryTitle = new Label("Execution Summary");
        summaryTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e5e5e5;");

        HBox summaryGrid = new HBox(20);
        summaryGrid.setPadding(new Insets(5, 0, 0, 0));

        VBox leftCol = new VBox(5);
        addDetailRow(leftCol, "Workflow:", execution.workflowName());
        addDetailRow(leftCol, "Status:", execution.status().toString());
        addDetailRow(leftCol, "Trigger:", execution.triggerType().toString());

        VBox rightCol = new VBox(5);
        addDetailRow(rightCol, "Started:", formatInstant(execution.startedAt()));
        addDetailRow(rightCol, "Finished:", formatInstant(execution.finishedAt()));
        addDetailRow(rightCol, "Duration:", formatDuration(execution.durationMs()));

        summaryGrid.getChildren().addAll(leftCol, rightCol);
        summaryBox.getChildren().addAll(summaryTitle, summaryGrid);

        content.getChildren().add(summaryBox);

        // Error message if present
        if (execution.errorMessage() != null && !execution.errorMessage().isBlank()) {
            VBox errorBox = new VBox(5);
            errorBox.setPadding(new Insets(10));
            errorBox.setStyle(
                    "-fx-background-color: #451a03; -fx-border-color: #dc2626; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5;");

            Label errorTitle = new Label("Error Message");
            errorTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ef4444;");

            Label errorText = new Label(execution.errorMessage());
            errorText.setStyle("-fx-font-size: 12px; -fx-text-fill: #fca5a5; -fx-wrap-text: true;");
            errorText.setMaxWidth(Double.MAX_VALUE);

            errorBox.getChildren().addAll(errorTitle, errorText);
            content.getChildren().add(errorBox);
        }

        // Node executions
        if (execution.nodeExecutions() != null && !execution.nodeExecutions().isEmpty()) {
            Label nodesTitle = new Label("Node Execution Details");
            nodesTitle.setStyle(
                    "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e5e5e5; -fx-padding: 0 0 10 0;");
            content.getChildren().add(nodesTitle);

            for (var nodeExec : execution.nodeExecutions()) {
                VBox nodeBox = createNodeExecutionBox(nodeExec);
                content.getChildren().add(nodeBox);
            }
        }

        // Execution log (if available)
        // Note: The execution_log field might not be populated in the current DTO
        // This would need to be added to the ExecutionDTO if we want to show full logs
    }

    private VBox createNodeExecutionBox(ExecutionDTO.NodeExecutionDTO nodeExec) {
        VBox nodeBox = new VBox(8);
        nodeBox.setPadding(new Insets(10));
        nodeBox.setStyle("-fx-background-color: #374151; -fx-border-radius: 5; -fx-background-radius: 5;");

        // Node header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label nodeName = new Label(nodeExec.nodeName() + " (" + nodeExec.nodeType() + ")");
        nodeName.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e5e5e5;");

        Label statusLabel = new Label(nodeExec.status().toString());
        statusLabel
                .setStyle("-fx-font-size: 11px; -fx-padding: 2 6 2 6; -fx-border-radius: 3; -fx-background-radius: 3;");
        switch (nodeExec.status().toString()) {
            case "COMPLETED" ->
                statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #166534; -fx-text-fill: #4ade80;");
            case "FAILED" ->
                statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #991b1b; -fx-text-fill: #f87171;");
            case "RUNNING" ->
                statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #92400e; -fx-text-fill: #fbbf24;");
            default ->
                statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #374151; -fx-text-fill: #9ca3af;");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label durationLabel = new Label(formatDuration(nodeExec.durationMs()));
        durationLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af;");

        header.getChildren().addAll(nodeName, statusLabel, spacer, durationLabel);

        nodeBox.getChildren().add(header);

        // Node details
        HBox detailsRow = new HBox(15);
        detailsRow.setPadding(new Insets(5, 0, 0, 0));

        VBox timingCol = new VBox(3);
        addDetailRow(timingCol, "Started:", formatInstant(nodeExec.startedAt()), 10);
        addDetailRow(timingCol, "Finished:", formatInstant(nodeExec.finishedAt()), 10);

        detailsRow.getChildren().add(timingCol);

        // Error message for failed nodes
        if (nodeExec.errorMessage() != null && !nodeExec.errorMessage().isBlank()) {
            VBox errorCol = new VBox(3);
            Label errorLabel = new Label("Error:");
            errorLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
            Label errorText = new Label(nodeExec.errorMessage());
            errorText.setStyle("-fx-font-size: 10px; -fx-text-fill: #fca5a5; -fx-wrap-text: true;");
            errorText.setMaxWidth(300);
            errorCol.getChildren().addAll(errorLabel, errorText);
            detailsRow.getChildren().add(errorCol);
        }

        nodeBox.getChildren().add(detailsRow);

        return nodeBox;
    }

    private void addDetailRow(VBox parent, String label, String value) {
        addDetailRow(parent, label, value, 12);
    }

    private void addDetailRow(VBox parent, String label, String value, int fontSize) {
        HBox row = new HBox(8);
        Label labelComp = new Label(label);
        labelComp.setStyle("-fx-font-size: " + fontSize + "px; -fx-text-fill: #9ca3af; -fx-font-weight: bold;");
        labelComp.setMinWidth(60);

        Label valueComp = new Label(value != null ? value : "N/A");
        valueComp.setStyle("-fx-font-size: " + fontSize + "px; -fx-text-fill: #e5e5e5;");

        row.getChildren().addAll(labelComp, valueComp);
        parent.getChildren().add(row);
    }

    private String formatInstant(Instant instant) {
        if (instant == null)
            return "N/A";
        return DateTimeFormatter.ofPattern("MMM d, HH:mm:ss")
                .format(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
    }

    private String formatDuration(Long durationMs) {
        if (durationMs == null)
            return "N/A";
        if (durationMs < 1000)
            return durationMs + "ms";
        return String.format("%.2fs", durationMs / 1000.0);
    }

    @FXML
    private void showCredentialsView() {
        clearActiveButton();
        btnCredentials.getStyleClass().add("active");

        // Show credential manager dialog
        CredentialManagerDialog dialog = new CredentialManagerDialog(credentialService);
        dialog.showAndWait();

        // Return to workflows view after closing dialog
        showWorkflowsView();
    }

    @FXML
    private void showSettingsView() {
        clearActiveButton();
        btnSettings.getStyleClass().add("active");

        // Show settings dialog
        SettingsDialog dialog = new SettingsDialog(settingsService, credentialService);
        dialog.showAndWait();

        // Return to workflows view after closing dialog
        showWorkflowsView();
    }

    private void clearActiveButton() {
        btnWorkflows.getStyleClass().remove("active");
        btnExecutions.getStyleClass().remove("active");
        btnCredentials.getStyleClass().remove("active");
        btnSettings.getStyleClass().remove("active");
    }

    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    @FXML
    private void onNewWorkflow() {
        showWorkflowsView();
        if (workflowCanvas != null) {
            workflowCanvas.newWorkflow();
        }
    }

    @FXML
    private void onOpenWorkflow() {
        List<WorkflowDTO> workflows = workflowService.findAll();
        WorkflowListDialog dialog = new WorkflowListDialog(workflows);
        Optional<WorkflowDTO> selected = dialog.showAndWait();

        selected.ifPresent(workflow -> {
            showWorkflowsView();
            if (workflowCanvas != null) {
                workflowCanvas.loadWorkflow(workflow);
                updateStatus("Loaded: " + workflow.name());
            }
        });
    }

    @FXML
    private void onSaveWorkflow() {
        if (workflowCanvas != null) {
            workflowCanvas.saveWorkflow();
        }
    }

    @FXML
    private void onRunWorkflow() {
        if (workflowCanvas != null) {
            workflowCanvas.runWorkflow();
        }
    }

    @FXML
    private void onImportWorkflow() {
        if (workflowCanvas != null) {
            workflowCanvas.importWorkflow();
        }
    }

    @FXML
    private void onExportWorkflow() {
        if (workflowCanvas != null) {
            workflowCanvas.exportWorkflow();
        }
    }

    @FXML
    private void onZoomIn() {
        if (workflowCanvas != null) {
            workflowCanvas.zoomIn();
            updateStatus("Zoom: " + workflowCanvas.getZoomPercentage() + "%");
        }
    }

    @FXML
    private void onZoomOut() {
        if (workflowCanvas != null) {
            workflowCanvas.zoomOut();
            updateStatus("Zoom: " + workflowCanvas.getZoomPercentage() + "%");
        }
    }

    @FXML
    private void onResetZoom() {
        if (workflowCanvas != null) {
            workflowCanvas.resetZoom();
            updateStatus("Zoom: " + workflowCanvas.getZoomPercentage() + "%");
        }
    }

    @FXML
    private void onFitToWindow() {
        if (workflowCanvas != null) {
            workflowCanvas.fitToView();
            updateStatus("Zoom: " + workflowCanvas.getZoomPercentage() + "%");
        }
    }

    @FXML
    private void onExit() {
        javafx.application.Platform.exit();
    }

    @FXML
    private void onShowExecutionConsole() {
        ExecutionConsoleService.getInstance().showConsole();
    }
}
