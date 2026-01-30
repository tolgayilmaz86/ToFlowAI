package io.toflowai.ui.canvas;

import java.util.Map;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Panel for previewing execution data at node connections.
 * Shows the data flowing between nodes in a formatted view.
 */
public class DataPreviewPanel extends VBox {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private Label titleLabel;
    private Label sourceNodeLabel;
    private Label targetNodeLabel;
    private final TabPane dataTabs;
    private final TextArea jsonView;
    private final TreeView<String> treeView;
    private final TableView<DataEntry> tableView;
    private final Label statusLabel;

    private Object currentData;

    public DataPreviewPanel() {
        setSpacing(10);
        setPadding(new Insets(15));
        setMinWidth(350);
        setMaxWidth(400);
        setPrefWidth(380);
        getStyleClass().add("data-preview-panel");
        setStyle("-fx-background-color: #1e1e1e; -fx-border-color: #404040; -fx-border-width: 1;");

        // Header
        HBox header = createHeader();

        // Connection info
        VBox connectionInfo = createConnectionInfo();

        // Data tabs
        dataTabs = createDataTabs();
        VBox.setVgrow(dataTabs, Priority.ALWAYS);

        // Status bar
        statusLabel = new Label("No data");
        statusLabel.setStyle("-fx-text-fill: #737373; -fx-font-size: 11px;");

        getChildren().addAll(header, connectionInfo, dataTabs, statusLabel);

        // Initialize views
        jsonView = new TextArea();
        treeView = new TreeView<>();
        tableView = new TableView<>();

        setupJsonView();
        setupTreeView();
        setupTableView();
    }

    private HBox createHeader() {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        FontIcon icon = FontIcon.of(MaterialDesignD.DATABASE_SEARCH, 20);
        icon.setIconColor(Color.web("#22c55e"));

        titleLabel = new Label("Data Preview");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e5e5e5;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button();
        closeBtn.setGraphic(FontIcon.of(MaterialDesignC.CLOSE, 14));
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #a3a3a3;");
        closeBtn.setOnAction(e -> setVisible(false));

        header.getChildren().addAll(icon, titleLabel, spacer, closeBtn);
        return header;
    }

    private VBox createConnectionInfo() {
        VBox info = new VBox(5);
        info.setPadding(new Insets(10));
        info.setStyle("-fx-background-color: #2a2a2a; -fx-background-radius: 5;");

        HBox sourceRow = new HBox(8);
        sourceRow.setAlignment(Pos.CENTER_LEFT);
        FontIcon sourceIcon = FontIcon.of(MaterialDesignA.ARROW_RIGHT_CIRCLE, 14);
        sourceIcon.setIconColor(Color.web("#3b82f6"));
        Label sourceLabel = new Label("Source:");
        sourceLabel.setStyle("-fx-text-fill: #a3a3a3; -fx-font-size: 11px;");
        sourceNodeLabel = new Label("None");
        sourceNodeLabel.setStyle("-fx-text-fill: #e5e5e5; -fx-font-weight: bold;");
        sourceRow.getChildren().addAll(sourceIcon, sourceLabel, sourceNodeLabel);

        HBox targetRow = new HBox(8);
        targetRow.setAlignment(Pos.CENTER_LEFT);
        FontIcon targetIcon = FontIcon.of(MaterialDesignA.ARROW_LEFT_CIRCLE, 14);
        targetIcon.setIconColor(Color.web("#22c55e"));
        Label targetLabel = new Label("Target:");
        targetLabel.setStyle("-fx-text-fill: #a3a3a3; -fx-font-size: 11px;");
        targetNodeLabel = new Label("None");
        targetNodeLabel.setStyle("-fx-text-fill: #e5e5e5; -fx-font-weight: bold;");
        targetRow.getChildren().addAll(targetIcon, targetLabel, targetNodeLabel);

        info.getChildren().addAll(sourceRow, targetRow);
        return info;
    }

    private TabPane createDataTabs() {
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setStyle("-fx-background-color: #1e1e1e;");

        Tab jsonTab = new Tab("JSON");
        jsonTab.setGraphic(FontIcon.of(MaterialDesignC.CODE_JSON, 12));

        Tab treeTab = new Tab("Tree");
        treeTab.setGraphic(FontIcon.of(MaterialDesignF.FILE_TREE, 12));

        Tab tableTab = new Tab("Table");
        tableTab.setGraphic(FontIcon.of(MaterialDesignT.TABLE, 12));

        tabs.getTabs().addAll(jsonTab, treeTab, tableTab);
        return tabs;
    }

    private void setupJsonView() {
        jsonView.setEditable(false);
        jsonView.setWrapText(true);
        jsonView.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; " +
                "-fx-font-size: 12px; -fx-control-inner-background: #1e1e1e; " +
                "-fx-text-fill: #d4d4d4;");

        dataTabs.getTabs().get(0).setContent(jsonView);
    }

    private void setupTreeView() {
        treeView.setShowRoot(true);
        treeView.setStyle("-fx-background-color: #1e1e1e;");
        treeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #e5e5e5;");
                }
            }
        });

        dataTabs.getTabs().get(1).setContent(treeView);
    }

    @SuppressWarnings("unchecked")
    private void setupTableView() {
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableView.setStyle("-fx-background-color: #1e1e1e;");
        tableView.setPlaceholder(new Label("No tabular data"));

        TableColumn<DataEntry, String> keyCol = new TableColumn<>("Key");
        keyCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().key()));
        keyCol.setPrefWidth(120);

        TableColumn<DataEntry, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().value()));
        valueCol.setPrefWidth(200);

        TableColumn<DataEntry, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().type()));
        typeCol.setPrefWidth(80);

        tableView.getColumns().addAll(keyCol, valueCol, typeCol);

        dataTabs.getTabs().get(2).setContent(tableView);
    }

    /**
     * Set the connection being previewed.
     */
    public void setConnection(String sourceNode, String targetNode) {
        sourceNodeLabel.setText(sourceNode != null ? sourceNode : "None");
        targetNodeLabel.setText(targetNode != null ? targetNode : "None");
    }

    /**
     * Set the data to preview.
     */
    public void setData(Object data) {
        this.currentData = data;
        updateViews();
    }

    private void updateViews() {
        if (currentData == null) {
            jsonView.setText("null");
            treeView.setRoot(new TreeItem<>("null"));
            tableView.getItems().clear();
            statusLabel.setText("No data");
            return;
        }

        // Update JSON view
        try {
            String json = OBJECT_MAPPER.writeValueAsString(currentData);
            jsonView.setText(json);
            statusLabel.setText("Size: " + json.length() + " bytes");
        } catch (JsonProcessingException e) {
            jsonView.setText("Error: " + e.getMessage());
            statusLabel.setText("Error parsing data");
        }

        // Update tree view
        updateTreeView();

        // Update table view
        updateTableView();
    }

    private void updateTreeView() {
        try {
            JsonNode node = OBJECT_MAPPER.valueToTree(currentData);
            TreeItem<String> root = buildTreeItem("root", node);
            root.setExpanded(true);
            treeView.setRoot(root);
        } catch (Exception e) {
            treeView.setRoot(new TreeItem<>("Error: " + e.getMessage()));
        }
    }

    private TreeItem<String> buildTreeItem(String key, JsonNode node) {
        TreeItem<String> item;

        if (node.isObject()) {
            item = new TreeItem<>(key + " {" + node.size() + "}");
            for (var field : node.properties()) {
                item.getChildren().add(buildTreeItem(field.getKey(), field.getValue()));
            }
        } else if (node.isArray()) {
            item = new TreeItem<>(key + " [" + node.size() + "]");
            for (int i = 0; i < node.size(); i++) {
                item.getChildren().add(buildTreeItem("[" + i + "]", node.get(i)));
            }
        } else if (node.isTextual()) {
            String value = node.asText();
            if (value.length() > 50) {
                value = value.substring(0, 47) + "...";
            }
            item = new TreeItem<>(key + ": \"" + value + "\"");
        } else if (node.isNumber()) {
            item = new TreeItem<>(key + ": " + node.asText());
        } else if (node.isBoolean()) {
            item = new TreeItem<>(key + ": " + node.asBoolean());
        } else if (node.isNull()) {
            item = new TreeItem<>(key + ": null");
        } else {
            item = new TreeItem<>(key + ": " + node.asText());
        }

        return item;
    }

    private void updateTableView() {
        tableView.getItems().clear();

        try {
            if (currentData instanceof Map<?, ?> map) {
                map.forEach((k, v) -> {
                    String valueStr = formatValue(v);
                    String typeStr = getTypeName(v);
                    tableView.getItems().add(new DataEntry(k.toString(), valueStr, typeStr));
                });
            } else {
                JsonNode node = OBJECT_MAPPER.valueToTree(currentData);
                if (node.isObject()) {
                    for (var field : node.properties()) {
                        String valueStr = formatJsonValue(field.getValue());
                        String typeStr = getJsonTypeName(field.getValue());
                        tableView.getItems().add(new DataEntry(field.getKey(), valueStr, typeStr));
                    }
                }
            }
        } catch (Exception e) {
            // Ignore table conversion errors
        }
    }

    private String formatValue(Object value) {
        if (value == null)
            return "null";
        String str = value.toString();
        if (str.length() > 100) {
            return str.substring(0, 97) + "...";
        }
        return str;
    }

    private String getTypeName(Object value) {
        if (value == null)
            return "null";
        if (value instanceof String)
            return "String";
        if (value instanceof Number)
            return "Number";
        if (value instanceof Boolean)
            return "Boolean";
        if (value instanceof java.util.List)
            return "Array";
        if (value instanceof Map)
            return "Object";
        return value.getClass().getSimpleName();
    }

    private String formatJsonValue(JsonNode node) {
        if (node.isTextual())
            return node.asText();
        if (node.isNumber())
            return node.asText();
        if (node.isBoolean())
            return String.valueOf(node.asBoolean());
        if (node.isNull())
            return "null";
        if (node.isArray())
            return "[" + node.size() + " items]";
        if (node.isObject())
            return "{" + node.size() + " fields}";
        return node.asText();
    }

    private String getJsonTypeName(JsonNode node) {
        if (node.isTextual())
            return "String";
        if (node.isNumber())
            return "Number";
        if (node.isBoolean())
            return "Boolean";
        if (node.isNull())
            return "null";
        if (node.isArray())
            return "Array";
        if (node.isObject())
            return "Object";
        return "Unknown";
    }

    /**
     * Clear the preview.
     */
    public void clear() {
        sourceNodeLabel.setText("None");
        targetNodeLabel.setText("None");
        currentData = null;
        jsonView.clear();
        treeView.setRoot(null);
        tableView.getItems().clear();
        statusLabel.setText("No data");
    }

    /**
     * Data entry for table view.
     */
    private record DataEntry(String key, String value, String type) {
    }
}
