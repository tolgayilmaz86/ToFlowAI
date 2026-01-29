package io.toflowai.ui.canvas;

import java.util.HashMap;
import java.util.Map;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignH;

import io.toflowai.common.domain.Node;
import io.toflowai.ui.canvas.NodeHelpProvider.NodeHelp;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Right-side panel for editing node properties.
 * Shows node name, type-specific parameters, and help.
 */
public class NodePropertiesPanel extends VBox {

    private final WorkflowCanvas canvas;

    private Node currentNode;
    private NodeView currentNodeView;

    // Header components
    private final Label titleLabel;
    private final Label subtitleLabel;
    private final Button helpButton;
    private final Button closeButton;

    // Common fields
    private final TextField nameField;
    private final TextArea notesArea;
    private final CheckBox disabledCheckbox;

    // Dynamic parameters container
    private final VBox parametersContainer;

    // Store parameter fields for saving
    private final Map<String, Object> parameterFields = new HashMap<>();

    // Callback for property changes
    private Runnable onPropertyChanged;

    public NodePropertiesPanel(WorkflowCanvas canvas) {
        this.canvas = canvas;

        getStyleClass().add("properties-panel");
        setStyle("-fx-background-color: #2a2a2a; -fx-border-color: #4a4a4a; -fx-border-width: 0 0 0 1;");
        setPrefWidth(300);
        setMinWidth(280);
        setMaxWidth(350);
        setPadding(new Insets(0));
        setSpacing(0);

        // Initialize header components first (needed by createHeader)
        titleLabel = new Label("Node Properties");
        titleLabel.getStyleClass().add("properties-title");

        subtitleLabel = new Label("Select a node to edit");
        subtitleLabel.getStyleClass().add("properties-subtitle");

        helpButton = createHelpButton();
        closeButton = createCloseButton();

        // === Header ===
        HBox header = createHeader();

        // === Common Properties Section ===
        VBox commonSection = new VBox(10);
        commonSection.setPadding(new Insets(15));
        commonSection.getStyleClass().add("properties-section");

        // Name field
        Label nameLabel = new Label("Name");
        nameLabel.getStyleClass().add("property-label");
        nameField = new TextField();
        nameField.setPromptText("Node name");
        nameField.textProperty().addListener((obs, old, val) -> notifyChange());

        // Disabled checkbox
        disabledCheckbox = new CheckBox("Disabled");
        disabledCheckbox.setStyle("-fx-text-fill: #a1a1aa;");
        disabledCheckbox.selectedProperty().addListener((obs, old, val) -> notifyChange());

        commonSection.getChildren().addAll(nameLabel, nameField, disabledCheckbox);

        // === Parameters Section ===
        parametersContainer = new VBox(10);
        parametersContainer.setPadding(new Insets(15));
        parametersContainer.getStyleClass().add("properties-section");

        TitledPane parametersPane = new TitledPane("Parameters", parametersContainer);
        parametersPane.setExpanded(true);
        parametersPane.getStyleClass().add("properties-titled-pane");

        // === Notes Section ===
        VBox notesSection = new VBox(5);
        notesSection.setPadding(new Insets(15));
        notesSection.getStyleClass().add("properties-section");

        Label notesLabel = new Label("Notes");
        notesLabel.getStyleClass().add("property-label");
        notesArea = new TextArea();
        notesArea.setPromptText("Add notes about this node...");
        notesArea.setPrefRowCount(3);
        notesArea.setWrapText(true);
        notesArea.textProperty().addListener((obs, old, val) -> notifyChange());

        notesSection.getChildren().addAll(notesLabel, notesArea);

        // === Action Buttons ===
        HBox actions = createActionButtons();

        // Scroll content
        VBox content = new VBox(0);
        content.getChildren().addAll(commonSection, new Separator(), parametersPane, new Separator(), notesSection);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(header, scrollPane, actions);

        // Initially hidden
        setVisible(false);
        setManaged(false);
    }

    private HBox createHeader() {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 15, 12, 15));
        header.getStyleClass().add("properties-header");

        VBox titleBox = new VBox(2);
        // titleLabel and subtitleLabel are already initialized
        titleBox.getChildren().addAll(titleLabel, subtitleLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // helpButton and closeButton are already initialized
        header.getChildren().addAll(titleBox, spacer, helpButton, closeButton);
        return header;
    }

    private Button createHelpButton() {
        Button btn = new Button();
        FontIcon icon = FontIcon.of(MaterialDesignH.HELP_CIRCLE_OUTLINE, 18);
        icon.setIconColor(Color.web("#60a5fa"));
        btn.setGraphic(icon);
        btn.getStyleClass().add("icon-button");
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        Tooltip.install(btn, new Tooltip("Show help & examples"));
        btn.setOnAction(e -> showHelp());
        return btn;
    }

    private Button createCloseButton() {
        Button btn = new Button();
        FontIcon icon = FontIcon.of(MaterialDesignC.CLOSE, 18);
        icon.setIconColor(Color.web("#a1a1aa"));
        btn.setGraphic(icon);
        btn.getStyleClass().add("icon-button");
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        Tooltip.install(btn, new Tooltip("Close panel"));
        btn.setOnAction(e -> hide());
        return btn;
    }

    private HBox createActionButtons() {
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(10, 15, 10, 15));
        actions.getStyleClass().add("properties-actions");

        Button applyBtn = new Button("Apply");
        applyBtn.getStyleClass().add("accent-button");
        applyBtn.setOnAction(e -> applyChanges());

        Button resetBtn = new Button("Reset");
        resetBtn.setOnAction(e -> loadNode(currentNodeView));

        actions.getChildren().addAll(resetBtn, applyBtn);
        return actions;
    }

    /**
     * Show the panel and load properties for the given node.
     */
    public void show(NodeView nodeView) {
        currentNodeView = nodeView;
        loadNode(nodeView);
        setVisible(true);
        setManaged(true);
    }

    /**
     * Hide the panel.
     */
    public void hide() {
        setVisible(false);
        setManaged(false);
        currentNode = null;
        currentNodeView = null;
    }

    /**
     * Check if panel is currently visible.
     */
    public boolean isShowing() {
        return isVisible();
    }

    /**
     * Load node properties into the panel.
     */
    private void loadNode(NodeView nodeView) {
        if (nodeView == null)
            return;

        currentNode = nodeView.getNode();

        // Update header
        titleLabel.setText(currentNode.name());
        subtitleLabel.setText(getNodeTypeLabel(currentNode.type()));

        // Update common fields
        nameField.setText(currentNode.name());
        disabledCheckbox.setSelected(currentNode.disabled());
        notesArea.setText(currentNode.notes() != null ? currentNode.notes() : "");

        // Build dynamic parameters
        buildParametersUI(currentNode.type(), currentNode.parameters());
    }

    /**
     * Build type-specific parameter fields.
     */
    private void buildParametersUI(String nodeType, Map<String, Object> params) {
        parametersContainer.getChildren().clear();
        parameterFields.clear();

        if (params == null) {
            params = new HashMap<>();
        }

        switch (nodeType) {
            case "httpRequest" -> buildHttpRequestParams(params);
            case "code" -> buildCodeParams(params);
            case "executeCommand" -> buildExecuteCommandParams(params);
            case "if" -> buildIfParams(params);
            case "switch" -> buildSwitchParams(params);
            case "loop" -> buildLoopParams(params);
            case "set" -> buildSetParams(params);
            case "filter" -> buildFilterParams(params);
            case "sort" -> buildSortParams(params);
            case "scheduleTrigger" -> buildScheduleTriggerParams(params);
            case "webhookTrigger" -> buildWebhookTriggerParams(params);
            case "llmChat" -> buildLlmChatParams(params);
            case "textClassifier" -> buildTextClassifierParams(params);
            case "embedding" -> buildEmbeddingParams(params);
            case "rag" -> buildRagParams(params);
            default -> buildGenericParams(params);
        }
    }

    // === Parameter Builders ===

    private void buildHttpRequestParams(Map<String, Object> params) {
        addTextField("url", "URL", params.getOrDefault("url", "").toString(), "https://api.example.com/endpoint");
        addComboBox("method", "Method", new String[] { "GET", "POST", "PUT", "DELETE", "PATCH" },
                params.getOrDefault("method", "GET").toString());
        addTextArea("headers", "Headers (JSON)", params.getOrDefault("headers", "{}").toString());
        addTextArea("body", "Request Body", params.getOrDefault("body", "").toString());
        addSpinner("timeout", "Timeout (seconds)",
                ((Number) params.getOrDefault("timeout", 30)).intValue(), 1, 300);
    }

    private void buildCodeParams(Map<String, Object> params) {
        addComboBox("language", "Language", new String[] { "javascript", "python" },
                params.getOrDefault("language", "javascript").toString());
        addTextArea("code", "Code", params.getOrDefault("code", "").toString());
    }

    private void buildExecuteCommandParams(Map<String, Object> params) {
        addTextField("command", "Command", params.getOrDefault("command", "").toString(), "echo Hello");
        addTextField("args", "Arguments", params.getOrDefault("args", "").toString(), "--flag value");
        addTextField("cwd", "Working Directory", params.getOrDefault("cwd", "").toString(), "/path/to/dir");
        addSpinner("timeout", "Timeout (seconds)",
                ((Number) params.getOrDefault("timeout", 60)).intValue(), 1, 3600);
    }

    private void buildIfParams(Map<String, Object> params) {
        addTextArea("condition", "Condition", params.getOrDefault("condition", "").toString());
        addLabel("Use expressions like: {{value}} > 10");
    }

    private void buildSwitchParams(Map<String, Object> params) {
        addTextField("value", "Value to Switch On", params.getOrDefault("value", "").toString(), "{{status}}");
        addTextArea("cases", "Cases (JSON)", params.getOrDefault("cases", "{}").toString());
    }

    private void buildLoopParams(Map<String, Object> params) {
        addTextField("items", "Items Expression", params.getOrDefault("items", "").toString(), "{{data.items}}");
        addSpinner("batchSize", "Batch Size",
                ((Number) params.getOrDefault("batchSize", 1)).intValue(), 1, 100);
        addCheckBox("parallel", "Run in Parallel",
                Boolean.parseBoolean(params.getOrDefault("parallel", "false").toString()));
    }

    private void buildSetParams(Map<String, Object> params) {
        addTextArea("values", "Values (JSON)", params.getOrDefault("values", "{}").toString());
        addCheckBox("keepOnlySet", "Keep Only Set Values",
                Boolean.parseBoolean(params.getOrDefault("keepOnlySet", "false").toString()));
    }

    private void buildFilterParams(Map<String, Object> params) {
        addTextField("items", "Items Expression", params.getOrDefault("items", "").toString(), "{{data}}");
        addTextArea("condition", "Filter Condition", params.getOrDefault("condition", "").toString());
        addSpinner("limit", "Limit",
                ((Number) params.getOrDefault("limit", 0)).intValue(), 0, 10000);
    }

    private void buildSortParams(Map<String, Object> params) {
        addTextField("items", "Items Expression", params.getOrDefault("items", "").toString(), "{{data}}");
        addTextField("sortBy", "Sort By Field", params.getOrDefault("sortBy", "").toString(), "name");
        addComboBox("order", "Order", new String[] { "asc", "desc" },
                params.getOrDefault("order", "asc").toString());
    }

    private void buildScheduleTriggerParams(Map<String, Object> params) {
        addTextField("cronExpression", "Cron Expression",
                params.getOrDefault("cronExpression", "").toString(), "0 0 * * *");
        addTextField("timezone", "Timezone",
                params.getOrDefault("timezone", "").toString(), "UTC");
        addLabel("Examples: '0 9 * * MON-FRI' (9 AM weekdays)");
    }

    private void buildWebhookTriggerParams(Map<String, Object> params) {
        addTextField("path", "Webhook Path", params.getOrDefault("path", "").toString(), "/my-webhook");
        addComboBox("method", "HTTP Method", new String[] { "GET", "POST", "PUT", "DELETE" },
                params.getOrDefault("method", "POST").toString());
        addSpinner("responseCode", "Response Code",
                ((Number) params.getOrDefault("responseCode", 200)).intValue(), 100, 599);
    }

    private void buildLlmChatParams(Map<String, Object> params) {
        addComboBox("provider", "Provider", new String[] { "openai", "anthropic", "ollama", "azure" },
                params.getOrDefault("provider", "openai").toString());
        addTextField("model", "Model", params.getOrDefault("model", "").toString(), "gpt-4");
        addTextArea("systemPrompt", "System Prompt", params.getOrDefault("systemPrompt", "").toString());
        addTextArea("prompt", "User Prompt", params.getOrDefault("prompt", "").toString());
        addSpinner("temperature", "Temperature (x10)",
                (int) (((Number) params.getOrDefault("temperature", 0.7)).doubleValue() * 10), 0, 20);
        addSpinner("maxTokens", "Max Tokens",
                ((Number) params.getOrDefault("maxTokens", 1000)).intValue(), 1, 128000);
    }

    private void buildTextClassifierParams(Map<String, Object> params) {
        addTextField("text", "Text Expression", params.getOrDefault("text", "").toString(), "{{input.text}}");
        addTextArea("categories", "Categories (comma-separated)",
                params.getOrDefault("categories", "").toString());
        addCheckBox("multiLabel", "Allow Multiple Labels",
                Boolean.parseBoolean(params.getOrDefault("multiLabel", "false").toString()));
    }

    private void buildEmbeddingParams(Map<String, Object> params) {
        addComboBox("provider", "Provider", new String[] { "openai", "ollama", "cohere" },
                params.getOrDefault("provider", "openai").toString());
        addTextField("model", "Model", params.getOrDefault("model", "").toString(), "text-embedding-3-small");
        addTextArea("text", "Text to Embed", params.getOrDefault("text", "").toString());
    }

    private void buildRagParams(Map<String, Object> params) {
        addTextField("query", "Query Expression", params.getOrDefault("query", "").toString(), "{{input.question}}");
        addTextField("documents", "Documents Expression",
                params.getOrDefault("documents", "").toString(), "{{knowledgeBase}}");
        addSpinner("topK", "Top K Results",
                ((Number) params.getOrDefault("topK", 5)).intValue(), 1, 50);
        addComboBox("provider", "LLM Provider", new String[] { "openai", "anthropic", "ollama" },
                params.getOrDefault("provider", "openai").toString());
        addTextField("model", "Model", params.getOrDefault("model", "").toString(), "gpt-4");
    }

    private void buildGenericParams(Map<String, Object> params) {
        if (params.isEmpty()) {
            addLabel("No configurable parameters for this node type.");
        } else {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                addTextField(entry.getKey(), entry.getKey(),
                        entry.getValue() != null ? entry.getValue().toString() : "", "");
            }
        }
    }

    // === Field Helpers ===

    private void addTextField(String key, String label, String value, String placeholder) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("property-label");
        TextField field = new TextField(value);
        field.setPromptText(placeholder);
        parameterFields.put(key, field);
        parametersContainer.getChildren().addAll(lbl, field);
    }

    private void addTextArea(String key, String label, String value) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("property-label");
        TextArea area = new TextArea(value);
        area.setPrefRowCount(4);
        area.setWrapText(true);
        parameterFields.put(key, area);
        parametersContainer.getChildren().addAll(lbl, area);
    }

    private void addComboBox(String key, String label, String[] options, String selected) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("property-label");
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll(options);
        combo.setValue(selected);
        parameterFields.put(key, combo);
        parametersContainer.getChildren().addAll(lbl, combo);
    }

    private void addSpinner(String key, String label, int value, int min, int max) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("property-label");
        Spinner<Integer> spinner = new Spinner<>(min, max, value);
        spinner.setEditable(true);
        parameterFields.put(key, spinner);
        parametersContainer.getChildren().addAll(lbl, spinner);
    }

    private void addCheckBox(String key, String label, boolean value) {
        CheckBox cb = new CheckBox(label);
        cb.setSelected(value);
        cb.setStyle("-fx-text-fill: #e5e5e5;");
        parameterFields.put(key, cb);
        parametersContainer.getChildren().add(cb);
    }

    private void addLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #737373; -fx-font-size: 11px;");
        lbl.setWrapText(true);
        parametersContainer.getChildren().add(lbl);
    }

    // === Actions ===

    private void applyChanges() {
        if (currentNode == null || currentNodeView == null)
            return;

        // Collect parameter values
        Map<String, Object> newParams = new HashMap<>();
        for (Map.Entry<String, Object> entry : parameterFields.entrySet()) {
            Object field = entry.getValue();
            Object value = extractFieldValue(field);
            if (value != null) {
                newParams.put(entry.getKey(), value);
            }
        }

        // Create updated node
        String newName = nameField.getText().trim();
        if (newName.isEmpty())
            newName = currentNode.name();

        Node updatedNode = new Node(
                currentNode.id(),
                currentNode.type(),
                newName,
                currentNode.position(),
                newParams,
                currentNode.credentialId(),
                disabledCheckbox.isSelected(),
                notesArea.getText());

        // Notify canvas to update
        canvas.updateNode(currentNodeView, updatedNode);

        // Reload to reflect changes
        loadNode(currentNodeView);
    }

    private Object extractFieldValue(Object field) {
        if (field instanceof TextField tf) {
            return tf.getText();
        } else if (field instanceof TextArea ta) {
            return ta.getText();
        } else if (field instanceof ComboBox<?> cb) {
            return cb.getValue();
        } else if (field instanceof Spinner<?> sp) {
            return sp.getValue();
        } else if (field instanceof CheckBox cb) {
            return cb.isSelected();
        }
        return null;
    }

    private void showHelp() {
        if (currentNode == null)
            return;

        NodeHelp help = NodeHelpProvider.getHelp(currentNode.type());

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Help: " + help.title());
        dialog.setHeaderText(help.shortDescription());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(15);
        content.setPrefWidth(600);
        content.setStyle("-fx-padding: 10;");

        Label descTitle = new Label("Description");
        descTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        TextArea descArea = new TextArea(help.detailedDescription());
        descArea.setWrapText(true);
        descArea.setEditable(false);
        descArea.setPrefRowCount(8);

        Label codeTitle = new Label("Sample Usage / Code");
        codeTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        TextArea codeArea = new TextArea(help.sampleCode());
        codeArea.setWrapText(true);
        codeArea.setEditable(false);
        codeArea.setPrefRowCount(12);
        codeArea.setStyle("-fx-font-family: 'Consolas', monospace;");

        content.getChildren().addAll(descTitle, descArea, codeTitle, codeArea);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(450);

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().setPrefSize(650, 550);
        dialog.showAndWait();
    }

    private void notifyChange() {
        if (onPropertyChanged != null) {
            onPropertyChanged.run();
        }
    }

    public void setOnPropertyChanged(Runnable callback) {
        this.onPropertyChanged = callback;
    }

    private String getNodeTypeLabel(String type) {
        return switch (type) {
            case "manualTrigger" -> "Manual Trigger";
            case "scheduleTrigger" -> "Schedule Trigger";
            case "webhookTrigger" -> "Webhook Trigger";
            case "httpRequest" -> "HTTP Request";
            case "code" -> "Code";
            case "executeCommand" -> "Execute Command";
            case "if" -> "If Condition";
            case "switch" -> "Switch";
            case "merge" -> "Merge";
            case "loop" -> "Loop";
            case "set" -> "Set";
            case "filter" -> "Filter";
            case "sort" -> "Sort";
            case "llmChat" -> "LLM Chat";
            case "textClassifier" -> "Text Classifier";
            case "embedding" -> "Embedding";
            case "rag" -> "RAG";
            default -> type;
        };
    }
}
