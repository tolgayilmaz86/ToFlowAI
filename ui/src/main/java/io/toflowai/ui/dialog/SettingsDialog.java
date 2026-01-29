package io.toflowai.ui.dialog;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignB;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignE;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignI;
import org.kordamp.ikonli.materialdesign2.MaterialDesignK;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.kordamp.ikonli.materialdesign2.MaterialDesignW;

import io.toflowai.common.dto.SettingDTO;
import io.toflowai.common.enums.SettingCategory;
import io.toflowai.common.service.CredentialServiceInterface;
import io.toflowai.common.service.SettingsServiceInterface;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Settings Dialog - Comprehensive settings management with tabbed categories.
 */
public class SettingsDialog extends Dialog<Void> {

    private final SettingsServiceInterface settingsService;
    private final CredentialServiceInterface credentialService;
    private final ListView<CategoryItem> categoryList;
    private final StackPane contentPane;
    private final Map<String, Node> controlMap = new HashMap<>();
    private final Map<String, String> pendingChanges = new HashMap<>();

    private CategoryItem selectedCategory;

    public SettingsDialog(SettingsServiceInterface settingsService, CredentialServiceInterface credentialService) {
        this.settingsService = settingsService;
        this.credentialService = credentialService;

        setTitle("Settings");
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);
        setResizable(true);

        // Main layout: sidebar + content
        HBox mainLayout = new HBox(0);
        mainLayout.setPrefSize(900, 650);
        mainLayout.setStyle("-fx-background-color: #2e3440;");
        HBox.setHgrow(mainLayout, Priority.ALWAYS);
        VBox.setVgrow(mainLayout, Priority.ALWAYS);

        // Category sidebar
        categoryList = createCategorySidebar();

        // Content area
        contentPane = new StackPane();
        contentPane.setStyle("-fx-background-color: #3b4252;");
        contentPane.setMinWidth(400); // Ensure minimum width to prevent collapse
        HBox.setHgrow(contentPane, Priority.ALWAYS);

        mainLayout.getChildren().addAll(categoryList, contentPane);

        // Wrap mainLayout in a container that fills the entire dialog
        StackPane dialogContainer = new StackPane();
        dialogContainer.setStyle("-fx-background-color: #2e3440;");
        dialogContainer.getChildren().add(mainLayout);
        StackPane.setAlignment(mainLayout, Pos.TOP_LEFT); // Align to top-left to fill entire area
        StackPane.setMargin(mainLayout, new Insets(0));

        getDialogPane().setContent(dialogContainer);
        getDialogPane().getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL);
        getDialogPane().setStyle(
                "-fx-background-color: #2e3440; -fx-padding: 0; -fx-border-width: 0; -fx-background-insets: 0;");

        // Ensure the dialog pane fills the entire dialog
        getDialogPane().setPrefSize(900, 650);
        getDialogPane().setMinSize(800, 500);

        // Override any default dialog styling
        setOnShowing(e -> {
            Stage stage = (Stage) getDialogPane().getScene().getWindow();
            if (stage.getScene() != null) {
                stage.getScene().setFill(Color.web("#2e3440"));
                stage.getScene().getRoot().setStyle("-fx-background-color: #2e3440;");
            }
        });

        // Style buttons
        Button applyBtn = (Button) getDialogPane().lookupButton(ButtonType.APPLY);
        applyBtn.setText("Save Changes");
        applyBtn.setStyle("-fx-background-color: #5e81ac; -fx-text-fill: white;");
        applyBtn.setOnAction(e -> saveChanges());

        Button cancelBtn = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelBtn.setStyle("-fx-background-color: #4c566a; -fx-text-fill: white;");

        // Select first category
        if (!categoryList.getItems().isEmpty()) {
            CategoryItem firstItem = categoryList.getItems().get(0);
            categoryList.getSelectionModel().selectFirst();
            // Force content load immediately to prevent blank display
            showCategoryContent(firstItem);
        }

        // Set dialog size
        Stage stage = (Stage) getDialogPane().getScene().getWindow();
        stage.setMinWidth(800);
        stage.setMinHeight(500);
    }

    private ListView<CategoryItem> createCategorySidebar() {
        ListView<CategoryItem> list = new ListView<>();
        list.setPrefWidth(220);
        list.setMinWidth(200);
        list.setStyle("-fx-background-color: #2e3440; -fx-border-width: 0;");

        // Add categories
        list.getItems().addAll(
                new CategoryItem("General", MaterialDesignC.COG, SettingCategory.GENERAL),
                new CategoryItem("Editor", MaterialDesignP.PENCIL, SettingCategory.EDITOR),
                new CategoryItem("Execution", MaterialDesignP.PLAY_CIRCLE, SettingCategory.EXECUTION),
                new CategoryItem("AI Providers", MaterialDesignR.ROBOT, SettingCategory.AI_PROVIDERS),
                new CategoryItem("Credentials", MaterialDesignK.KEY_VARIANT, null), // Special case
                new CategoryItem("HTTP & Network", MaterialDesignW.WEB, SettingCategory.HTTP_NETWORK),
                new CategoryItem("Database & Storage", MaterialDesignD.DATABASE, SettingCategory.DATABASE_STORAGE),
                new CategoryItem("Webhook & Server", MaterialDesignS.SERVER, SettingCategory.WEBHOOK_SERVER),
                new CategoryItem("Notifications", MaterialDesignB.BELL, SettingCategory.NOTIFICATIONS),
                new CategoryItem("Advanced", MaterialDesignC.CODE_TAGS, SettingCategory.ADVANCED));

        list.setCellFactory(lv -> new CategoryCell());

        list.getSelectionModel().selectedItemProperty().addListener((obs, old, item) -> {
            if (item != null) {
                selectedCategory = item;
                showCategoryContent(item);
            }
        });

        return list;
    }

    private void showCategoryContent(CategoryItem item) {
        VBox content = new VBox(0);
        content.setStyle("-fx-background-color: #3b4252;");

        // Header
        HBox header = createCategoryHeader(item);
        content.getChildren().add(header);

        // Content scroll area
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #3b4252; -fx-background-color: #3b4252;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox settingsContainer = new VBox(20);
        settingsContainer.setPadding(new Insets(20));
        settingsContainer.setStyle("-fx-background-color: #3b4252;");

        if (item.category == null && "Credentials".equals(item.name)) {
            // Special handling for credentials
            settingsContainer.getChildren().add(createCredentialsSection());
        } else if (item.category != null) {
            // Load settings for category
            List<SettingDTO> settings = settingsService.findByCategory(item.category);

            // Group settings by section (based on key prefix)
            Map<String, VBox> sections = new HashMap<>();

            for (SettingDTO setting : settings) {
                if (!setting.visible())
                    continue;

                String sectionName = getSectionName(setting.key());
                VBox section = sections.computeIfAbsent(sectionName, name -> {
                    VBox box = createSection(name);
                    settingsContainer.getChildren().add(box);
                    return box;
                });

                Node control = createSettingControl(setting);
                if (control != null) {
                    section.getChildren().add(control);
                    controlMap.put(setting.key(), control);
                }
            }
        }

        // Add reset button at bottom
        settingsContainer.getChildren().add(createResetSection(item));

        scrollPane.setContent(settingsContainer);
        content.getChildren().add(scrollPane);

        contentPane.getChildren().clear();
        contentPane.getChildren().add(content);
    }

    private HBox createCategoryHeader(CategoryItem item) {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(25, 25, 20, 25));
        header.setStyle("-fx-background-color: #434c5e;");

        FontIcon icon = FontIcon.of(item.icon, 32);
        icon.setIconColor(Color.web("#88c0d0"));

        VBox titleBox = new VBox(3);
        Label title = new Label(item.name);
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #eceff4;");
        Label subtitle = new Label(getCategoryDescription(item.name));
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #d8dee9;");
        titleBox.getChildren().addAll(title, subtitle);

        header.getChildren().addAll(icon, titleBox);
        return header;
    }

    private String getCategoryDescription(String name) {
        return switch (name) {
            case "General" -> "Application behavior and appearance";
            case "Editor" -> "Canvas and workflow editor settings";
            case "Execution" -> "Workflow execution parameters";
            case "AI Providers" -> "Configure AI service connections";
            case "Credentials" -> "Manage API keys and secure credentials";
            case "HTTP & Network" -> "HTTP client and proxy settings";
            case "Database & Storage" -> "Data persistence and backups";
            case "Webhook & Server" -> "HTTP server and webhook configuration";
            case "Notifications" -> "Alerts and notification preferences";
            case "Advanced" -> "Developer options and diagnostics";
            default -> "";
        };
    }

    private VBox createSection(String title) {
        VBox section = new VBox(12);
        section.setPadding(new Insets(0, 0, 10, 0));

        Label titleLabel = new Label(formatSectionTitle(title));
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #88c0d0;");

        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: #4c566a;");

        section.getChildren().addAll(titleLabel, separator);
        return section;
    }

    private String formatSectionTitle(String key) {
        // Convert key like "openai" to "OpenAI"
        return switch (key.toLowerCase()) {
            case "openai" -> "OpenAI";
            case "anthropic" -> "Anthropic";
            case "ollama" -> "Ollama";
            case "azure" -> "Azure OpenAI";
            case "smtp" -> "SMTP Settings";
            default -> key.substring(0, 1).toUpperCase() + key.substring(1).replace("_", " ");
        };
    }

    private String getSectionName(String key) {
        // Extract section from key like "ai.openai.api_key" -> "openai"
        String[] parts = key.split("\\.");
        if (parts.length >= 2) {
            return parts[1];
        }
        return "general";
    }

    private Node createSettingControl(SettingDTO setting) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 15, 8, 15));
        row.setStyle("-fx-background-color: #434c5e; -fx-background-radius: 6;");

        // Label
        VBox labelBox = new VBox(2);
        labelBox.setMinWidth(200);
        labelBox.setMaxWidth(250);

        Label label = new Label(setting.label() != null ? setting.label() : formatLabel(setting.key()));
        label.setStyle("-fx-font-size: 13px; -fx-text-fill: #eceff4;");

        if (setting.requiresRestart()) {
            label.setText(label.getText() + " ⟳");
            label.setTooltip(new Tooltip("Requires restart to apply"));
        }

        if (setting.description() != null && !setting.description().isEmpty()) {
            Label desc = new Label(setting.description());
            desc.setStyle("-fx-font-size: 11px; -fx-text-fill: #a3be8c;");
            desc.setWrapText(true);
            labelBox.getChildren().addAll(label, desc);
        } else {
            labelBox.getChildren().add(label);
        }

        row.getChildren().add(labelBox);

        // Control
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().add(spacer);

        Node control = createControl(setting);
        if (control != null) {
            row.getChildren().add(control);
        }

        return row;
    }

    private Node createControl(SettingDTO setting) {
        return switch (setting.type()) {
            case BOOLEAN -> createToggle(setting);
            case INTEGER, LONG -> createNumberField(setting);
            case DOUBLE -> createDecimalField(setting);
            case PASSWORD -> createPasswordField(setting);
            case PATH -> createPathField(setting);
            case ENUM -> createDropdown(setting);
            default -> createTextField(setting);
        };
    }

    private CheckBox createToggle(SettingDTO setting) {
        CheckBox checkBox = new CheckBox();
        checkBox.setSelected(Boolean.parseBoolean(setting.value()));
        checkBox.setStyle("-fx-text-fill: white;");
        checkBox.selectedProperty()
                .addListener((obs, old, val) -> pendingChanges.put(setting.key(), String.valueOf(val)));
        return checkBox;
    }

    private Spinner<Integer> createNumberField(SettingDTO setting) {
        int min = 0, max = Integer.MAX_VALUE, value = 0;
        try {
            value = Integer.parseInt(setting.value());
        } catch (Exception ignored) {
        }

        // Parse validation rules for min/max
        if (setting.validationRules() != null) {
            try {
                var rules = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                        .readValue(setting.validationRules(), Map.class);
                if (rules.containsKey("min"))
                    min = ((Number) rules.get("min")).intValue();
                if (rules.containsKey("max"))
                    max = ((Number) rules.get("max")).intValue();
            } catch (Exception ignored) {
            }
        }

        Spinner<Integer> spinner = new Spinner<>(min, max, value);
        spinner.setPrefWidth(120);
        spinner.setEditable(true);
        spinner.setStyle("-fx-background-color: #4c566a;");
        spinner.valueProperty().addListener((obs, old, val) -> pendingChanges.put(setting.key(), String.valueOf(val)));
        return spinner;
    }

    private Spinner<Double> createDecimalField(SettingDTO setting) {
        double min = 0, max = Double.MAX_VALUE, value = 0;
        try {
            value = Double.parseDouble(setting.value());
        } catch (Exception ignored) {
        }

        Spinner<Double> spinner = new Spinner<>(min, max, value, 0.1);
        spinner.setPrefWidth(120);
        spinner.setEditable(true);
        spinner.setStyle("-fx-background-color: #4c566a;");
        spinner.valueProperty().addListener((obs, old, val) -> pendingChanges.put(setting.key(), String.valueOf(val)));
        return spinner;
    }

    private HBox createPasswordField(SettingDTO setting) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);

        PasswordField passwordField = new PasswordField();
        passwordField.setText(setting.value() != null ? setting.value() : "");
        passwordField.setPrefWidth(200);
        passwordField.setStyle("-fx-background-color: #4c566a; -fx-text-fill: #eceff4;");
        passwordField.textProperty().addListener((obs, old, val) -> pendingChanges.put(setting.key(), val));

        TextField visibleField = new TextField();
        visibleField.setPrefWidth(200);
        visibleField.setStyle("-fx-background-color: #4c566a; -fx-text-fill: #eceff4;");
        visibleField.setVisible(false);
        visibleField.setManaged(false);
        visibleField.textProperty().bindBidirectional(passwordField.textProperty());

        Button toggleBtn = new Button();
        FontIcon eyeIcon = FontIcon.of(MaterialDesignE.EYE, 16);
        eyeIcon.setIconColor(Color.web("#d8dee9"));
        toggleBtn.setGraphic(eyeIcon);
        toggleBtn.setStyle("-fx-background-color: #4c566a;");
        toggleBtn.setOnAction(e -> {
            boolean show = !visibleField.isVisible();
            visibleField.setVisible(show);
            visibleField.setManaged(show);
            passwordField.setVisible(!show);
            passwordField.setManaged(!show);
            eyeIcon.setIconCode(show ? MaterialDesignE.EYE_OFF : MaterialDesignE.EYE);
        });

        box.getChildren().addAll(passwordField, visibleField, toggleBtn);
        return box;
    }

    private HBox createPathField(SettingDTO setting) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);

        TextField textField = new TextField(setting.value() != null ? setting.value() : "");
        textField.setPrefWidth(200);
        textField.setStyle("-fx-background-color: #4c566a; -fx-text-fill: #eceff4;");
        textField.textProperty().addListener((obs, old, val) -> pendingChanges.put(setting.key(), val));

        Button browseBtn = new Button();
        FontIcon folderIcon = FontIcon.of(MaterialDesignF.FOLDER_OPEN, 16);
        folderIcon.setIconColor(Color.web("#d8dee9"));
        browseBtn.setGraphic(folderIcon);
        browseBtn.setStyle("-fx-background-color: #4c566a;");
        browseBtn.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Directory");
            if (textField.getText() != null && !textField.getText().isEmpty()) {
                File current = new File(textField.getText());
                if (current.exists()) {
                    chooser.setInitialDirectory(current);
                }
            }
            Stage stage = (Stage) getDialogPane().getScene().getWindow();
            File selected = chooser.showDialog(stage);
            if (selected != null) {
                textField.setText(selected.getAbsolutePath());
            }
        });

        box.getChildren().addAll(textField, browseBtn);
        return box;
    }

    private ComboBox<String> createDropdown(SettingDTO setting) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setPrefWidth(150);
        comboBox.setStyle("-fx-background-color: #4c566a;");

        // Parse options from validation rules
        if (setting.validationRules() != null) {
            try {
                var rules = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                        .readValue(setting.validationRules(), Map.class);
                if (rules.containsKey("options")) {
                    @SuppressWarnings("unchecked")
                    List<String> options = (List<String>) rules.get("options");
                    comboBox.getItems().addAll(options);
                }
            } catch (Exception ignored) {
            }
        }

        comboBox.setValue(setting.value());
        comboBox.valueProperty().addListener((obs, old, val) -> pendingChanges.put(setting.key(), val));
        return comboBox;
    }

    private TextField createTextField(SettingDTO setting) {
        TextField textField = new TextField(setting.value() != null ? setting.value() : "");
        textField.setPrefWidth(200);
        textField.setStyle("-fx-background-color: #4c566a; -fx-text-fill: #eceff4;");
        textField.textProperty().addListener((obs, old, val) -> pendingChanges.put(setting.key(), val));
        return textField;
    }

    private Node createCredentialsSection() {
        VBox section = new VBox(15);
        section.setPadding(new Insets(10));

        Label info = new Label("Credentials are managed through the Credential Manager.\n" +
                "Click the button below to open the manager.");
        info.setStyle("-fx-font-size: 13px; -fx-text-fill: #d8dee9;");
        info.setWrapText(true);

        Button openBtn = new Button("Open Credential Manager");
        openBtn.setStyle("-fx-background-color: #5e81ac; -fx-text-fill: white; -fx-font-size: 13px;");
        FontIcon keyIcon = FontIcon.of(MaterialDesignK.KEY_VARIANT, 16);
        keyIcon.setIconColor(Color.WHITE);
        openBtn.setGraphic(keyIcon);
        openBtn.setOnAction(e -> {
            CredentialManagerDialog dialog = new CredentialManagerDialog(credentialService);
            dialog.showAndWait();
        });

        section.getChildren().addAll(info, openBtn);
        return section;
    }

    private Node createResetSection(CategoryItem item) {
        VBox section = new VBox(10);
        section.setPadding(new Insets(20, 0, 0, 0));

        Separator separator = new Separator();

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        Button resetBtn = new Button("Reset to Defaults");
        resetBtn.setStyle("-fx-background-color: #bf616a; -fx-text-fill: white;");
        FontIcon resetIcon = FontIcon.of(MaterialDesignR.REFRESH, 14);
        resetIcon.setIconColor(Color.WHITE);
        resetBtn.setGraphic(resetIcon);

        resetBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Reset Settings");
            confirm.setHeaderText("Reset " + item.name + " to defaults?");
            confirm.setContentText("This will restore all settings in this category to their default values.");
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK && item.category != null) {
                    settingsService.resetCategoryToDefaults(item.category);
                    showCategoryContent(item); // Refresh
                    pendingChanges.clear();
                }
            });
        });

        // Spacer to push import/export to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Export button
        Button exportBtn = new Button("Export");
        exportBtn.setStyle("-fx-background-color: #5e81ac; -fx-text-fill: white;");
        FontIcon exportIcon = FontIcon.of(MaterialDesignE.EXPORT, 14);
        exportIcon.setIconColor(Color.WHITE);
        exportBtn.setGraphic(exportIcon);
        exportBtn.setOnAction(e -> exportSettings());

        // Import button
        Button importBtn = new Button("Import");
        importBtn.setStyle("-fx-background-color: #a3be8c; -fx-text-fill: white;");
        FontIcon importIcon = FontIcon.of(MaterialDesignI.IMPORT, 14);
        importIcon.setIconColor(Color.WHITE);
        importBtn.setGraphic(importIcon);
        importBtn.setOnAction(e -> importSettings());

        buttonBox.getChildren().addAll(resetBtn, spacer, exportBtn, importBtn);
        section.getChildren().addAll(separator, buttonBox);
        return section;
    }

    /**
     * Export all settings to a JSON file.
     */
    private void exportSettings() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Settings");
        fileChooser.setInitialFileName("toflowai-settings.json");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JSON Files", "*.json"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));

        Stage stage = (Stage) getDialogPane().getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                String json = settingsService.exportAsJson();
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(json);
                }

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Successful");
                alert.setHeaderText(null);
                alert.setContentText("Settings exported to:\n" + file.getAbsolutePath());
                alert.showAndWait();
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Export Failed");
                alert.setHeaderText("Failed to export settings");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
        }
    }

    /**
     * Import settings from a JSON file.
     */
    private void importSettings() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Settings");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JSON Files", "*.json"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));

        Stage stage = (Stage) getDialogPane().getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            // Confirm before importing
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Import Settings");
            confirm.setHeaderText("Import settings from file?");
            confirm.setContentText("This will overwrite existing settings with values from:\n" + file.getName());

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        String json = Files.readString(file.toPath());
                        settingsService.importFromJson(json);

                        // Refresh the current view
                        if (selectedCategory != null) {
                            showCategoryContent(selectedCategory);
                        }
                        pendingChanges.clear();

                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Import Successful");
                        alert.setHeaderText(null);
                        alert.setContentText("Settings imported successfully.");
                        alert.showAndWait();
                    } catch (IOException e) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Import Failed");
                        alert.setHeaderText("Failed to read file");
                        alert.setContentText(e.getMessage());
                        alert.showAndWait();
                    } catch (Exception e) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Import Failed");
                        alert.setHeaderText("Failed to import settings");
                        alert.setContentText(e.getMessage());
                        alert.showAndWait();
                    }
                }
            });
        }
    }

    private String formatLabel(String key) {
        // Convert "general.auto_save" to "Auto Save"
        String[] parts = key.split("\\.");
        String last = parts[parts.length - 1];
        return last.replace("_", " ").substring(0, 1).toUpperCase() +
                last.replace("_", " ").substring(1);
    }

    private void saveChanges() {
        if (!pendingChanges.isEmpty()) {
            settingsService.setValues(pendingChanges);
            pendingChanges.clear();
        }
    }

    // Category list cell
    private static class CategoryCell extends ListCell<CategoryItem> {
        @Override
        protected void updateItem(CategoryItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setStyle("-fx-background-color: transparent;");
            } else {
                HBox box = new HBox(12);
                box.setAlignment(Pos.CENTER_LEFT);
                box.setPadding(new Insets(12, 15, 12, 15));

                FontIcon icon = FontIcon.of(item.icon, 18);
                icon.setIconColor(isSelected() ? Color.web("#88c0d0") : Color.web("#d8dee9"));

                Label label = new Label(item.name);
                label.setStyle("-fx-font-size: 13px; -fx-text-fill: " +
                        (isSelected() ? "#eceff4" : "#d8dee9") + ";");

                box.getChildren().addAll(icon, label);
                setGraphic(box);
                setStyle("-fx-background-color: " +
                        (isSelected() ? "#434c5e" : "transparent") + ";");
            }
        }
    }

    // Category item record
    private record CategoryItem(String name, Ikon icon, SettingCategory category) {
    }
}
