package io.toflowai.ui.dialog;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;
import org.kordamp.ikonli.materialdesign2.MaterialDesignV;

import io.toflowai.common.dto.VariableDTO;
import io.toflowai.common.dto.VariableDTO.VariableScope;
import io.toflowai.common.dto.VariableDTO.VariableType;
import io.toflowai.common.service.VariableServiceInterface;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.StageStyle;

/**
 * Dialog for managing workflow variables.
 */
public class VariableManagerDialog extends Dialog<Void> {

    private final VariableServiceInterface variableService;
    private final TableView<VariableDTO> variableTable;
    private final ObservableList<VariableDTO> variables;
    private final ComboBox<VariableScope> scopeFilter;
    private final Long workflowId;

    public VariableManagerDialog(VariableServiceInterface variableService, Long workflowId) {
        this.variableService = variableService;
        this.workflowId = workflowId;
        this.variables = FXCollections.observableArrayList();

        setTitle("Variable Manager");
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);
        setResizable(true);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefSize(750, 550);
        content.setStyle("-fx-background-color: #1e1e1e;");

        HBox header = createHeader();

        HBox toolbar = createToolbar();
        scopeFilter = (ComboBox<VariableScope>) toolbar.lookup("#scopeFilter");

        variableTable = createVariableTable();
        VBox.setVgrow(variableTable, Priority.ALWAYS);

        Label helpLabel = new Label("💡 Use ${variableName} syntax to reference variables in node configurations");
        helpLabel.setStyle("-fx-text-fill: #737373; -fx-font-size: 11px;");

        content.getChildren().addAll(header, toolbar, variableTable, helpLabel);

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        getDialogPane().setStyle("-fx-background-color: #1e1e1e;");

        Button closeBtn = (Button) getDialogPane().lookupButton(ButtonType.CLOSE);
        closeBtn.setStyle("-fx-background-color: #3b3b3b; -fx-text-fill: white;");

        refreshVariables();
    }

    private HBox createHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        FontIcon icon = FontIcon.of(MaterialDesignV.VARIABLE_BOX, 28);
        icon.setIconColor(Color.web("#a855f7"));

        VBox titleBox = new VBox(2);
        Label title = new Label("Variable Manager");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e5e5e5;");
        Label subtitle = new Label("Define reusable values for your workflows");
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #a3a3a3;");
        titleBox.getChildren().addAll(title, subtitle);

        header.getChildren().addAll(icon, titleBox);
        return header;
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(10, 0, 10, 0));

        Button addBtn = new Button("Add Variable");
        addBtn.setGraphic(FontIcon.of(MaterialDesignP.PLUS, 14));
        addBtn.setStyle("-fx-background-color: #a855f7; -fx-text-fill: white;");
        addBtn.setOnAction(e -> showAddVariableDialog());

        Button editBtn = new Button("Edit");
        editBtn.setGraphic(FontIcon.of(MaterialDesignP.PENCIL, 14));
        editBtn.setStyle("-fx-background-color: #3b3b3b; -fx-text-fill: white;");
        editBtn.disableProperty().bind(variableTable.getSelectionModel().selectedItemProperty().isNull());
        editBtn.setOnAction(e -> showEditVariableDialog());

        Button deleteBtn = new Button("Delete");
        deleteBtn.setGraphic(FontIcon.of(MaterialDesignD.DELETE, 14));
        deleteBtn.setStyle("-fx-background-color: #3b3b3b; -fx-text-fill: white;");
        deleteBtn.disableProperty().bind(variableTable.getSelectionModel().selectedItemProperty().isNull());
        deleteBtn.setOnAction(e -> deleteSelectedVariable());

        Separator sep = new Separator();
        sep.setOrientation(javafx.geometry.Orientation.VERTICAL);

        Label filterLabel = new Label("Scope:");
        filterLabel.setStyle("-fx-text-fill: #a3a3a3;");

        ComboBox<VariableScope> scopeFilter = new ComboBox<>();
        scopeFilter.setId("scopeFilter");
        scopeFilter.getItems().add(null); // All
        scopeFilter.getItems().addAll(VariableScope.values());
        scopeFilter.setValue(null);
        scopeFilter.setPromptText("All");
        scopeFilter.setStyle("-fx-background-color: #3b3b3b;");
        scopeFilter.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(VariableScope scope, boolean empty) {
                super.updateItem(scope, empty);
                if (empty || scope == null) {
                    setText("All");
                } else {
                    setText(scope.getDisplayName());
                }
            }
        });
        scopeFilter.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(VariableScope scope, boolean empty) {
                super.updateItem(scope, empty);
                if (empty || scope == null) {
                    setText("All");
                } else {
                    setText(scope.getDisplayName());
                }
            }
        });
        scopeFilter.valueProperty().addListener((obs, oldVal, newVal) -> refreshVariables());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setGraphic(FontIcon.of(MaterialDesignR.REFRESH, 14));
        refreshBtn.setStyle("-fx-background-color: #3b3b3b; -fx-text-fill: white;");
        refreshBtn.setOnAction(e -> refreshVariables());

        toolbar.getChildren().addAll(addBtn, editBtn, deleteBtn, sep, filterLabel, scopeFilter, spacer, refreshBtn);
        return toolbar;
    }

    @SuppressWarnings("unchecked")
    private TableView<VariableDTO> createVariableTable() {
        TableView<VariableDTO> table = new TableView<>(variables);
        table.setPlaceholder(new Label("No variables defined"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setStyle("-fx-background-color: #2a2a2a; -fx-border-color: #404040;");

        TableColumn<VariableDTO, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));
        nameCol.setPrefWidth(150);
        nameCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                if (empty || name == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox box = new HBox(5);
                    box.setAlignment(Pos.CENTER_LEFT);
                    FontIcon icon = FontIcon.of(MaterialDesignV.VARIABLE, 12);
                    icon.setIconColor(Color.web("#a855f7"));
                    Label label = new Label("${" + name + "}");
                    label.setStyle("-fx-text-fill: #d4d4d4; -fx-font-family: 'Consolas', monospace;");
                    box.getChildren().addAll(icon, label);
                    setGraphic(box);
                }
            }
        });

        TableColumn<VariableDTO, VariableType> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().type()));
        typeCol.setPrefWidth(100);
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(VariableType type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label label = new Label(type.getDisplayName());
                    label.setStyle("-fx-text-fill: #e5e5e5; -fx-padding: 2 6; -fx-background-radius: 3; " +
                            "-fx-background-color: " + getTypeColor(type) + ";");
                    setGraphic(label);
                }
            }
        });

        TableColumn<VariableDTO, VariableScope> scopeCol = new TableColumn<>("Scope");
        scopeCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().scope()));
        scopeCol.setPrefWidth(100);
        scopeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(VariableScope scope, boolean empty) {
                super.updateItem(scope, empty);
                if (empty || scope == null) {
                    setText(null);
                } else {
                    setText(scope.getDisplayName());
                    setStyle("-fx-text-fill: #e5e5e5;");
                }
            }
        });

        TableColumn<VariableDTO, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().value()));
        valueCol.setPrefWidth(200);
        valueCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    String display = value.length() > 50 ? value.substring(0, 47) + "..." : value;
                    setText(display);
                    setStyle("-fx-text-fill: #a3a3a3;");
                }
            }
        });

        TableColumn<VariableDTO, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().description() != null ? data.getValue().description() : ""));
        descCol.setPrefWidth(150);

        table.getColumns().addAll(nameCol, typeCol, scopeCol, valueCol, descCol);

        table.setRowFactory(tv -> {
            TableRow<VariableDTO> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    showEditVariableDialog();
                }
            });
            return row;
        });

        return table;
    }

    private String getTypeColor(VariableType type) {
        return switch (type) {
            case STRING -> "#3b82f6";
            case NUMBER -> "#22c55e";
            case BOOLEAN -> "#f59e0b";
            case JSON -> "#8b5cf6";
            case SECRET -> "#ef4444";
        };
    }

    private void refreshVariables() {
        variables.clear();
        VariableScope filter = scopeFilter != null ? scopeFilter.getValue() : null;

        if (filter == null) {
            variables.addAll(variableService.findAll());
        } else if (filter == VariableScope.GLOBAL) {
            variables.addAll(variableService.findGlobalVariables());
        } else if (filter == VariableScope.WORKFLOW && workflowId != null) {
            variables.addAll(variableService.findByWorkflowId(workflowId));
        }
    }

    private void showAddVariableDialog() {
        VariableEditDialog dialog = new VariableEditDialog(null, workflowId);
        dialog.showAndWait().ifPresent(variable -> {
            try {
                variableService.create(variable);
                refreshVariables();
            } catch (Exception e) {
                showError("Failed to create variable", e.getMessage());
            }
        });
    }

    private void showEditVariableDialog() {
        VariableDTO selected = variableTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        VariableEditDialog dialog = new VariableEditDialog(selected, workflowId);
        dialog.showAndWait().ifPresent(variable -> {
            try {
                variableService.update(selected.id(), variable);
                refreshVariables();
            } catch (Exception e) {
                showError("Failed to update variable", e.getMessage());
            }
        });
    }

    private void deleteSelectedVariable() {
        VariableDTO selected = variableTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Variable");
        confirm.setHeaderText("Delete \"" + selected.name() + "\"?");
        confirm.setContentText("Any workflows using this variable may fail.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    variableService.delete(selected.id());
                    refreshVariables();
                } catch (Exception e) {
                    showError("Failed to delete variable", e.getMessage());
                }
            }
        });
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Dialog for adding/editing a variable.
     */
    private static class VariableEditDialog extends Dialog<VariableDTO> {

        private final TextField nameField;
        private final ComboBox<VariableType> typeCombo;
        private final ComboBox<VariableScope> scopeCombo;
        private final TextArea valueArea;
        private final TextField descriptionField;
        private final Long workflowId;

        public VariableEditDialog(VariableDTO existing, Long workflowId) {
            this.workflowId = workflowId;

            setTitle(existing == null ? "Add Variable" : "Edit Variable");
            initModality(Modality.APPLICATION_MODAL);

            GridPane grid = new GridPane();
            grid.setHgap(15);
            grid.setVgap(15);
            grid.setPadding(new Insets(20));
            grid.setStyle("-fx-background-color: #1e1e1e;");

            Label nameLabel = new Label("Name:");
            nameLabel.setStyle("-fx-text-fill: #e5e5e5;");
            nameField = new TextField(existing != null ? existing.name() : "");
            nameField.setPromptText("myVariable (letters, numbers, underscores)");
            nameField.setPrefWidth(300);

            Label typeLabel = new Label("Type:");
            typeLabel.setStyle("-fx-text-fill: #e5e5e5;");
            typeCombo = new ComboBox<>();
            typeCombo.getItems().addAll(VariableType.values());
            typeCombo.setValue(existing != null ? existing.type() : VariableType.STRING);
            typeCombo.setPrefWidth(300);
            typeCombo.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(VariableType type, boolean empty) {
                    super.updateItem(type, empty);
                    if (empty || type == null) {
                        setText(null);
                    } else {
                        setText(type.getDisplayName() + " - " + type.getDescription());
                    }
                }
            });
            typeCombo.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(VariableType type, boolean empty) {
                    super.updateItem(type, empty);
                    setText(empty || type == null ? null : type.getDisplayName());
                }
            });

            Label scopeLabel = new Label("Scope:");
            scopeLabel.setStyle("-fx-text-fill: #e5e5e5;");
            scopeCombo = new ComboBox<>();
            scopeCombo.getItems().addAll(VariableScope.GLOBAL, VariableScope.WORKFLOW);
            scopeCombo.setValue(existing != null ? existing.scope() : VariableScope.GLOBAL);
            scopeCombo.setPrefWidth(300);
            scopeCombo.setDisable(existing != null); // Can't change scope after creation
            scopeCombo.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(VariableScope scope, boolean empty) {
                    super.updateItem(scope, empty);
                    if (empty || scope == null) {
                        setText(null);
                    } else {
                        setText(scope.getDisplayName() + " - " + scope.getDescription());
                    }
                }
            });
            scopeCombo.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(VariableScope scope, boolean empty) {
                    super.updateItem(scope, empty);
                    setText(empty || scope == null ? null : scope.getDisplayName());
                }
            });

            Label valueLabel = new Label(existing == null ? "Value:" : "New Value (leave empty to keep):");
            valueLabel.setStyle("-fx-text-fill: #e5e5e5;");
            valueArea = new TextArea();
            valueArea.setPromptText("Enter value...");
            valueArea.setPrefRowCount(3);
            valueArea.setPrefWidth(300);
            if (existing != null && existing.type() != VariableType.SECRET) {
                valueArea.setText(existing.value());
            }

            Label descLabel = new Label("Description:");
            descLabel.setStyle("-fx-text-fill: #e5e5e5;");
            descriptionField = new TextField(existing != null ? existing.description() : "");
            descriptionField.setPromptText("Optional description");
            descriptionField.setPrefWidth(300);

            grid.add(nameLabel, 0, 0);
            grid.add(nameField, 1, 0);
            grid.add(typeLabel, 0, 1);
            grid.add(typeCombo, 1, 1);
            grid.add(scopeLabel, 0, 2);
            grid.add(scopeCombo, 1, 2);
            grid.add(valueLabel, 0, 3);
            grid.add(valueArea, 1, 3);
            grid.add(descLabel, 0, 4);
            grid.add(descriptionField, 1, 4);

            getDialogPane().setContent(grid);
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            getDialogPane().setStyle("-fx-background-color: #1e1e1e;");

            Button okBtn = (Button) getDialogPane().lookupButton(ButtonType.OK);
            okBtn.setText(existing == null ? "Create" : "Save");
            okBtn.setStyle("-fx-background-color: #a855f7; -fx-text-fill: white;");

            Button cancelBtn = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
            cancelBtn.setStyle("-fx-background-color: #3b3b3b; -fx-text-fill: white;");

            // Validation
            okBtn.disableProperty().bind(
                    nameField.textProperty().isEmpty()
                            .or(existing == null ? valueArea.textProperty().isEmpty()
                                    : javafx.beans.binding.Bindings.createBooleanBinding(() -> false)));

            setResultConverter(buttonType -> {
                if (buttonType == ButtonType.OK) {
                    VariableScope scope = scopeCombo.getValue();
                    Long varWorkflowId = scope == VariableScope.WORKFLOW ? this.workflowId : null;

                    return new VariableDTO(
                            existing != null ? existing.id() : null,
                            nameField.getText().trim(),
                            valueArea.getText(),
                            typeCombo.getValue(),
                            scope,
                            varWorkflowId,
                            descriptionField.getText().trim().isEmpty() ? null : descriptionField.getText().trim(),
                            existing != null ? existing.createdAt() : null,
                            null);
                }
                return null;
            });
        }
    }
}
