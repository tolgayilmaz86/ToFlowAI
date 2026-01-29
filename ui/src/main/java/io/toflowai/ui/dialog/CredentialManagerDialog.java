package io.toflowai.ui.dialog;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignK;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;

import io.toflowai.common.dto.CredentialDTO;
import io.toflowai.common.enums.CredentialType;
import io.toflowai.common.service.CredentialServiceInterface;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Dialog for managing credentials (API keys, passwords, etc.).
 */
public class CredentialManagerDialog extends Dialog<Void> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private final CredentialServiceInterface credentialService;
    private final TableView<CredentialDTO> credentialTable;
    private final ObservableList<CredentialDTO> credentials;

    public CredentialManagerDialog(CredentialServiceInterface credentialService) {
        this.credentialService = credentialService;
        this.credentials = FXCollections.observableArrayList();

        setTitle("Credential Manager");
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);
        setResizable(true);

        // Create content
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefSize(700, 500);
        content.setStyle("-fx-background-color: #1e1e1e;");

        // Header
        HBox header = createHeader();

        // Table
        credentialTable = createCredentialTable();
        VBox.setVgrow(credentialTable, Priority.ALWAYS);

        // Toolbar
        HBox toolbar = createToolbar();

        content.getChildren().addAll(header, toolbar, credentialTable);

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        getDialogPane().setStyle("-fx-background-color: #1e1e1e;");

        // Style the close button
        Button closeBtn = (Button) getDialogPane().lookupButton(ButtonType.CLOSE);
        closeBtn.setStyle("-fx-background-color: #3b3b3b; -fx-text-fill: white;");

        // Load credentials
        refreshCredentials();

        // Set dialog size
        Stage stage = (Stage) getDialogPane().getScene().getWindow();
        stage.setMinWidth(600);
        stage.setMinHeight(400);
    }

    private HBox createHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        FontIcon icon = FontIcon.of(MaterialDesignK.KEY_VARIANT, 28);
        icon.setIconColor(Color.web("#fbbf24"));

        VBox titleBox = new VBox(2);
        Label title = new Label("Credential Manager");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e5e5e5;");
        Label subtitle = new Label("Securely store and manage API keys and credentials");
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #a3a3a3;");
        titleBox.getChildren().addAll(title, subtitle);

        header.getChildren().addAll(icon, titleBox);
        return header;
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(10, 0, 10, 0));

        Button addBtn = new Button("Add Credential");
        addBtn.setGraphic(FontIcon.of(MaterialDesignP.PLUS, 14));
        addBtn.setStyle("-fx-background-color: #4a9eff; -fx-text-fill: white;");
        addBtn.setOnAction(e -> showAddCredentialDialog());

        Button editBtn = new Button("Edit");
        editBtn.setGraphic(FontIcon.of(MaterialDesignP.PENCIL, 14));
        editBtn.setStyle("-fx-background-color: #3b3b3b; -fx-text-fill: white;");
        editBtn.disableProperty().bind(credentialTable.getSelectionModel().selectedItemProperty().isNull());
        editBtn.setOnAction(e -> showEditCredentialDialog());

        Button deleteBtn = new Button("Delete");
        deleteBtn.setGraphic(FontIcon.of(MaterialDesignD.DELETE, 14));
        deleteBtn.setStyle("-fx-background-color: #3b3b3b; -fx-text-fill: white;");
        deleteBtn.disableProperty().bind(credentialTable.getSelectionModel().selectedItemProperty().isNull());
        deleteBtn.setOnAction(e -> deleteSelectedCredential());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setGraphic(FontIcon.of(MaterialDesignR.REFRESH, 14));
        refreshBtn.setStyle("-fx-background-color: #3b3b3b; -fx-text-fill: white;");
        refreshBtn.setOnAction(e -> refreshCredentials());

        toolbar.getChildren().addAll(addBtn, editBtn, deleteBtn, spacer, refreshBtn);
        return toolbar;
    }

    @SuppressWarnings("unchecked")
    private TableView<CredentialDTO> createCredentialTable() {
        TableView<CredentialDTO> table = new TableView<>(credentials);
        table.setPlaceholder(new Label("No credentials stored"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setStyle("-fx-background-color: #2a2a2a; -fx-border-color: #404040;");

        // Name column
        TableColumn<CredentialDTO, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));
        nameCol.setPrefWidth(200);

        // Type column
        TableColumn<CredentialDTO, CredentialType> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().type()));
        typeCol.setPrefWidth(150);
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(CredentialType type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox box = new HBox(5);
                    box.setAlignment(Pos.CENTER_LEFT);
                    FontIcon icon = getIconForType(type);
                    Label label = new Label(type.getDisplayName());
                    label.setStyle("-fx-text-fill: #e5e5e5;");
                    box.getChildren().addAll(icon, label);
                    setGraphic(box);
                }
            }
        });

        // Created column
        TableColumn<CredentialDTO, String> createdCol = new TableColumn<>("Created");
        createdCol.setCellValueFactory(data -> {
            if (data.getValue().createdAt() != null) {
                return new SimpleStringProperty(DATE_FORMATTER.format(data.getValue().createdAt()));
            }
            return new SimpleStringProperty("-");
        });
        createdCol.setPrefWidth(140);

        // Updated column
        TableColumn<CredentialDTO, String> updatedCol = new TableColumn<>("Updated");
        updatedCol.setCellValueFactory(data -> {
            if (data.getValue().updatedAt() != null) {
                return new SimpleStringProperty(DATE_FORMATTER.format(data.getValue().updatedAt()));
            }
            return new SimpleStringProperty("-");
        });
        updatedCol.setPrefWidth(140);

        table.getColumns().addAll(nameCol, typeCol, createdCol, updatedCol);

        // Double-click to edit
        table.setRowFactory(tv -> {
            TableRow<CredentialDTO> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    showEditCredentialDialog();
                }
            });
            return row;
        });

        return table;
    }

    private FontIcon getIconForType(CredentialType type) {
        FontIcon icon = switch (type) {
            case API_KEY -> FontIcon.of(MaterialDesignK.KEY, 14);
            case HTTP_BASIC -> FontIcon.of(MaterialDesignA.ACCOUNT_KEY, 14);
            case HTTP_BEARER -> FontIcon.of(MaterialDesignS.SHIELD_KEY, 14);
            case OAUTH2 -> FontIcon.of(MaterialDesignS.SECURITY, 14);
            case CUSTOM_HEADER -> FontIcon.of(MaterialDesignC.CODE_BRACES, 14);
        };
        icon.setIconColor(Color.web("#fbbf24"));
        return icon;
    }

    private void refreshCredentials() {
        credentials.clear();
        credentials.addAll(credentialService.findAll());
    }

    private void showAddCredentialDialog() {
        CredentialEditDialog dialog = new CredentialEditDialog(null);
        dialog.showAndWait().ifPresent(result -> {
            try {
                credentialService.create(result.dto(), result.data());
                refreshCredentials();
            } catch (Exception e) {
                showError("Failed to create credential", e.getMessage());
            }
        });
    }

    private void showEditCredentialDialog() {
        CredentialDTO selected = credentialTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        CredentialEditDialog dialog = new CredentialEditDialog(selected);
        dialog.showAndWait().ifPresent(result -> {
            try {
                credentialService.update(selected.id(), result.dto(), result.data());
                refreshCredentials();
            } catch (Exception e) {
                showError("Failed to update credential", e.getMessage());
            }
        });
    }

    private void deleteSelectedCredential() {
        CredentialDTO selected = credentialTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Credential");
        confirm.setHeaderText("Delete \"" + selected.name() + "\"?");
        confirm.setContentText("This action cannot be undone. Any workflows using this credential will fail.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    credentialService.delete(selected.id());
                    refreshCredentials();
                } catch (Exception e) {
                    showError("Failed to delete credential", e.getMessage());
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
     * Inner dialog for adding/editing a credential.
     */
    private static class CredentialEditDialog extends Dialog<CredentialEditResult> {

        private final TextField nameField;
        private final ComboBox<CredentialType> typeCombo;
        private final PasswordField dataField;
        private final CheckBox showDataCheckbox;
        private final TextField dataVisibleField;

        public CredentialEditDialog(CredentialDTO existing) {
            setTitle(existing == null ? "Add Credential" : "Edit Credential");
            initModality(Modality.APPLICATION_MODAL);

            GridPane grid = new GridPane();
            grid.setHgap(15);
            grid.setVgap(15);
            grid.setPadding(new Insets(20));
            grid.setStyle("-fx-background-color: #1e1e1e;");

            // Name field
            Label nameLabel = new Label("Name:");
            nameLabel.setStyle("-fx-text-fill: #e5e5e5;");
            nameField = new TextField(existing != null ? existing.name() : "");
            nameField.setPromptText("e.g., OpenAI API Key");
            nameField.setPrefWidth(300);

            // Type combo
            Label typeLabel = new Label("Type:");
            typeLabel.setStyle("-fx-text-fill: #e5e5e5;");
            typeCombo = new ComboBox<>();
            typeCombo.getItems().addAll(CredentialType.values());
            typeCombo.setValue(existing != null ? existing.type() : CredentialType.API_KEY);
            typeCombo.setPrefWidth(300);
            typeCombo.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(CredentialType type, boolean empty) {
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
                protected void updateItem(CredentialType type, boolean empty) {
                    super.updateItem(type, empty);
                    if (empty || type == null) {
                        setText(null);
                    } else {
                        setText(type.getDisplayName());
                    }
                }
            });

            // Data field (password)
            Label dataLabel = new Label(existing == null ? "Credential Data:" : "New Data (leave empty to keep):");
            dataLabel.setStyle("-fx-text-fill: #e5e5e5;");

            dataField = new PasswordField();
            dataField.setPromptText("Enter API key or password...");
            dataField.setPrefWidth(300);

            dataVisibleField = new TextField();
            dataVisibleField.setPromptText("Enter API key or password...");
            dataVisibleField.setPrefWidth(300);
            dataVisibleField.setVisible(false);
            dataVisibleField.setManaged(false);

            // Bind fields
            dataVisibleField.textProperty().bindBidirectional(dataField.textProperty());

            showDataCheckbox = new CheckBox("Show");
            showDataCheckbox.setStyle("-fx-text-fill: #a3a3a3;");
            showDataCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                dataField.setVisible(!newVal);
                dataField.setManaged(!newVal);
                dataVisibleField.setVisible(newVal);
                dataVisibleField.setManaged(newVal);
            });

            HBox dataBox = new HBox(10);
            dataBox.setAlignment(Pos.CENTER_LEFT);
            StackPane fieldStack = new StackPane(dataField, dataVisibleField);
            HBox.setHgrow(fieldStack, Priority.ALWAYS);
            dataBox.getChildren().addAll(fieldStack, showDataCheckbox);

            // Info label
            Label infoLabel = new Label("⚠ Credentials are encrypted and stored securely.");
            infoLabel.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 11px;");

            grid.add(nameLabel, 0, 0);
            grid.add(nameField, 1, 0);
            grid.add(typeLabel, 0, 1);
            grid.add(typeCombo, 1, 1);
            grid.add(dataLabel, 0, 2);
            grid.add(dataBox, 1, 2);
            grid.add(infoLabel, 0, 3, 2, 1);

            getDialogPane().setContent(grid);
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            getDialogPane().setStyle("-fx-background-color: #1e1e1e;");

            // Style buttons
            Button okBtn = (Button) getDialogPane().lookupButton(ButtonType.OK);
            okBtn.setText(existing == null ? "Create" : "Save");
            okBtn.setStyle("-fx-background-color: #4a9eff; -fx-text-fill: white;");

            Button cancelBtn = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
            cancelBtn.setStyle("-fx-background-color: #3b3b3b; -fx-text-fill: white;");

            // Validation
            okBtn.disableProperty().bind(
                    nameField.textProperty().isEmpty()
                            .or(existing == null ? dataField.textProperty().isEmpty()
                                    : javafx.beans.binding.Bindings.createBooleanBinding(() -> false)));

            setResultConverter(buttonType -> {
                if (buttonType == ButtonType.OK) {
                    CredentialDTO dto = new CredentialDTO(
                            existing != null ? existing.id() : null,
                            nameField.getText().trim(),
                            typeCombo.getValue(),
                            existing != null ? existing.createdAt() : null,
                            null);
                    String data = dataField.getText();
                    return new CredentialEditResult(dto, data.isBlank() ? null : data);
                }
                return null;
            });
        }
    }

    /**
     * Result record for credential edit dialog.
     */
    private record CredentialEditResult(CredentialDTO dto, String data) {
    }
}
