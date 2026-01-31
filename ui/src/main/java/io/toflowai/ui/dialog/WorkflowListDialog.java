package io.toflowai.ui.dialog;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

import io.toflowai.common.dto.WorkflowDTO;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

/**
 * Dialog for selecting a workflow from the list.
 */
public class WorkflowListDialog extends Dialog<WorkflowDTO> {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ListView<WorkflowDTO> workflowList;
    private final Label detailsLabel;

    public WorkflowListDialog(List<WorkflowDTO> workflows) {
        this(workflows, null);
    }

    public WorkflowListDialog(List<WorkflowDTO> workflows, Consumer<WorkflowDTO> onDelete) {
        setTitle("Open Workflow");
        setHeaderText("Select a workflow to open");
        initModality(Modality.APPLICATION_MODAL);

        // Create the list view
        workflowList = new ListView<>();
        workflowList.getItems().addAll(workflows);
        workflowList.setPrefWidth(400);
        workflowList.setPrefHeight(300);

        // Custom cell factory for better display
        workflowList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(WorkflowDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox content = new VBox(2);

                    Label nameLabel = new Label(item.name());
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

                    Label infoLabel = new Label(buildInfoText(item));
                    infoLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");

                    content.getChildren().addAll(nameLabel, infoLabel);
                    setGraphic(content);
                }
            }
        });

        // Details panel
        detailsLabel = new Label("Select a workflow to see details");
        detailsLabel.setWrapText(true);
        detailsLabel.setPrefWidth(300);
        detailsLabel.setStyle("-fx-padding: 10px;");

        // Update details on selection
        workflowList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateDetails(newVal);
            }
        });

        // Layout
        HBox content = new HBox(10);
        content.setPadding(new Insets(10));

        VBox leftPane = new VBox(5);
        leftPane.getChildren().addAll(new Label("Workflows:"), workflowList);

        // Delete button (only if delete callback is provided)
        if (onDelete != null) {
            Button deleteButton = new Button("Delete Selected");
            deleteButton.setStyle("-fx-base: #dc2626;");
            deleteButton.setOnAction(e -> {
                WorkflowDTO selected = workflowList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Delete Workflow");
                    confirm.setHeaderText("Delete workflow: " + selected.name() + "?");
                    confirm.setContentText(
                            "This action cannot be undone. All workflow data will be permanently deleted.");

                    confirm.showAndWait().ifPresent(buttonType -> {
                        if (buttonType == ButtonType.OK) {
                            onDelete.accept(selected);
                            workflowList.getItems().remove(selected);
                            if (workflowList.getItems().isEmpty()) {
                                workflowList.setPlaceholder(
                                        new Label("No workflows found.\nCreate a new workflow to get started."));
                            }
                        }
                    });
                }
            });
            deleteButton.disableProperty().bind(workflowList.getSelectionModel().selectedItemProperty().isNull());
            leftPane.getChildren().add(deleteButton);
        }

        VBox rightPane = new VBox(5);
        rightPane.getChildren().addAll(new Label("Details:"), detailsLabel);
        rightPane.setStyle("-fx-background-color: #2a2a2a; -fx-padding: 10px;");
        rightPane.setPrefWidth(300);

        content.getChildren().addAll(leftPane, rightPane);

        getDialogPane().setContent(content);

        // Buttons
        ButtonType openButton = new ButtonType("Open", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(openButton, cancelButton);

        // Disable open button until selection
        getDialogPane().lookupButton(openButton).setDisable(true);
        workflowList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            getDialogPane().lookupButton(openButton).setDisable(newVal == null);
        });

        // Double-click to open
        workflowList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && workflowList.getSelectionModel().getSelectedItem() != null) {
                setResult(workflowList.getSelectionModel().getSelectedItem());
                close();
            }
        });

        // Result converter
        setResultConverter(buttonType -> {
            if (buttonType == openButton) {
                return workflowList.getSelectionModel().getSelectedItem();
            }
            return null;
        });

        // Empty state
        if (workflows.isEmpty()) {
            workflowList.setPlaceholder(new Label("No workflows found.\nCreate a new workflow to get started."));
        }
    }

    private String buildInfoText(WorkflowDTO workflow) {
        StringBuilder sb = new StringBuilder();
        sb.append(workflow.nodes().size()).append(" nodes");
        sb.append(" • ").append(workflow.triggerType());
        if (workflow.updatedAt() != null) {
            sb.append(" • Updated: ").append(
                    DATE_FORMAT.format(workflow.updatedAt().atZone(java.time.ZoneId.systemDefault())));
        }
        return sb.toString();
    }

    private void updateDetails(WorkflowDTO workflow) {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(workflow.name()).append("\n\n");

        if (workflow.description() != null && !workflow.description().isBlank()) {
            sb.append("Description:\n").append(workflow.description()).append("\n\n");
        }

        sb.append("Trigger: ").append(workflow.triggerType()).append("\n");
        sb.append("Nodes: ").append(workflow.nodes().size()).append("\n");
        sb.append("Connections: ").append(workflow.connections().size()).append("\n");
        sb.append("Active: ").append(workflow.isActive() ? "Yes" : "No").append("\n\n");

        if (workflow.createdAt() != null) {
            sb.append("Created: ").append(
                    DATE_FORMAT.format(workflow.createdAt().atZone(java.time.ZoneId.systemDefault()))).append("\n");
        }
        if (workflow.updatedAt() != null) {
            sb.append("Updated: ").append(
                    DATE_FORMAT.format(workflow.updatedAt().atZone(java.time.ZoneId.systemDefault()))).append("\n");
        }
        if (workflow.lastExecuted() != null) {
            sb.append("Last Run: ").append(
                    DATE_FORMAT.format(workflow.lastExecuted().atZone(java.time.ZoneId.systemDefault()))).append("\n");
        }

        detailsLabel.setText(sb.toString());
    }
}
