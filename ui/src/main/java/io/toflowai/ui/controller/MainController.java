package io.toflowai.ui.controller;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.springframework.stereotype.Component;

import io.toflowai.common.dto.WorkflowDTO;
import io.toflowai.common.service.CredentialServiceInterface;
import io.toflowai.common.service.ExecutionServiceInterface;
import io.toflowai.common.service.WorkflowServiceInterface;
import io.toflowai.ui.canvas.WorkflowCanvas;
import io.toflowai.ui.console.ExecutionConsoleService;
import io.toflowai.ui.dialog.CredentialManagerDialog;
import io.toflowai.ui.dialog.WorkflowListDialog;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
            CredentialServiceInterface credentialService) {
        this.workflowService = workflowService;
        this.executionService = executionService;
        this.credentialService = credentialService;
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

        // Placeholder for executions view
        Label placeholder = new Label("Executions View");
        placeholder.setStyle("-fx-font-size: 24px;");
        contentArea.getChildren().setAll(placeholder);

        updateStatus("Executions");
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

        // Placeholder for settings view
        Label placeholder = new Label("Settings View");
        placeholder.setStyle("-fx-font-size: 24px;");
        contentArea.getChildren().setAll(placeholder);

        updateStatus("Settings");
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
    private void onExit() {
        javafx.application.Platform.exit();
    }

    @FXML
    private void onShowExecutionConsole() {
        ExecutionConsoleService.getInstance().showConsole();
    }
}
