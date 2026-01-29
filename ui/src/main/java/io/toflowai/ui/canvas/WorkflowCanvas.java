package io.toflowai.ui.canvas;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignB;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignH;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;
import org.kordamp.ikonli.materialdesign2.MaterialDesignV;
import org.kordamp.ikonli.materialdesign2.MaterialDesignW;

import io.toflowai.common.domain.Connection;
import io.toflowai.common.domain.Node;
import io.toflowai.common.dto.ExecutionDTO;
import io.toflowai.common.dto.WorkflowDTO;
import io.toflowai.common.enums.ExecutionStatus;
import io.toflowai.common.service.ExecutionServiceInterface;
import io.toflowai.common.service.WorkflowServiceInterface;
import io.toflowai.ui.canvas.NodeHelpProvider.NodeHelp;
import io.toflowai.ui.canvas.NodeView.ExecutionState;
import io.toflowai.ui.console.ExecutionConsoleService;
import io.toflowai.ui.console.ExecutionConsoleService.NodeState;
import io.toflowai.ui.console.ExecutionConsoleService.NodeStateListener;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;

/**
 * Workflow canvas for visual node editing.
 * Supports drag-drop, zoom, pan, and node connections.
 */
public class WorkflowCanvas extends BorderPane implements NodeStateListener {

    private static final double GRID_SIZE = 20;
    private static final double MIN_ZOOM = 0.25;
    private static final double MAX_ZOOM = 2.0;
    private static final double ZOOM_FACTOR = 0.1;

    private final WorkflowServiceInterface workflowService;
    private final ExecutionServiceInterface executionService;

    private final Pane canvasPane;
    private final Pane nodeLayer;
    private final Pane connectionLayer;
    private final Pane gridLayer;
    private final ScrollPane paletteScrollPane;
    private final ScrollPane canvasScrollPane;
    private final VBox nodePalette;
    private final Label statusLabel;
    private final NodePropertiesPanel propertiesPanel;
    private final ExecutionHistoryPanel executionHistoryPanel;
    private final CanvasMinimap minimap;
    private final UndoRedoManager undoRedoManager;

    private WorkflowDTO workflow;
    private final Map<String, NodeView> nodeViews = new HashMap<>();
    private final Map<String, ConnectionLine> connectionLines = new HashMap<>();

    private double scale = 1.0;
    private double translateX = 0;
    private double translateY = 0;

    // Drag state
    private double dragStartX, dragStartY;
    private boolean isPanning = false;

    // Connection dragging state
    private boolean isConnectionDragging = false;
    private NodeView connectionSource = null;
    private NodeView hoveredTarget = null;
    private CubicCurve tempConnectionLine = null;

    // Selection
    private final java.util.Set<NodeView> selectedNodes = new java.util.HashSet<>();

    // Clipboard
    private final java.util.List<Node> clipboardNodes = new java.util.ArrayList<>();

    // File chooser state
    private java.io.File lastImportDirectory = null;
    private java.io.File lastExportDirectory = null;

    public WorkflowCanvas(WorkflowServiceInterface workflowService, ExecutionServiceInterface executionService) {
        this.workflowService = workflowService;
        this.executionService = executionService;

        // Initialize layers
        gridLayer = new Pane();
        connectionLayer = new Pane();
        nodeLayer = new Pane();

        canvasPane = new Pane(gridLayer, connectionLayer, nodeLayer);
        canvasPane.getStyleClass().add("canvas-pane");

        // Status label for feedback
        statusLabel = new Label("");
        statusLabel.getStyleClass().add("canvas-status");

        // Wrap in scroll pane
        canvasScrollPane = new ScrollPane(canvasPane);
        canvasScrollPane.setPannable(false);
        canvasScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        canvasScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        canvasScrollPane.setFitToWidth(true);
        canvasScrollPane.setFitToHeight(true);
        canvasScrollPane.getStyleClass().add("canvas-scroll");

        // Node palette on the left with scroll
        nodePalette = createNodePalette();
        paletteScrollPane = new ScrollPane(nodePalette);
        paletteScrollPane.setFitToWidth(true);
        paletteScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        paletteScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        paletteScrollPane.getStyleClass().add("palette-scroll");

        // Properties panel on the right
        propertiesPanel = new NodePropertiesPanel(this);

        // Execution history panel (toggleable on the right side below properties)
        executionHistoryPanel = new ExecutionHistoryPanel(this);

        // Create right side container for properties and history
        VBox rightContainer = new VBox();
        rightContainer.getChildren().addAll(propertiesPanel, executionHistoryPanel);
        VBox.setVgrow(propertiesPanel, Priority.NEVER);
        VBox.setVgrow(executionHistoryPanel, Priority.ALWAYS);

        // Minimap in bottom-right corner (positioned over canvas)
        minimap = new CanvasMinimap(this);
        minimap.setVisible(true);
        minimap.setManaged(true);

        // Create a container for center with minimap overlay
        StackPane centerContainer = new StackPane(canvasScrollPane, minimap);
        StackPane.setAlignment(minimap, javafx.geometry.Pos.BOTTOM_RIGHT);
        StackPane.setMargin(minimap, new Insets(0, 16, 16, 0));

        // Initialize undo/redo manager
        undoRedoManager = new UndoRedoManager();

        // Layout
        setCenter(centerContainer);
        setLeft(paletteScrollPane);
        setRight(rightContainer);

        // Setup interactions
        setupCanvasInteraction();
        setupKeyboardShortcuts();

        // Register for node state changes from ExecutionConsoleService
        ExecutionConsoleService.getInstance().addNodeStateListener(this);

        // Draw grid
        drawGrid();

        // Create empty workflow
        newWorkflow();
    }

    private VBox createNodePalette() {
        VBox palette = new VBox(10);
        palette.setPadding(new Insets(10));
        palette.setPrefWidth(200);
        palette.getStyleClass().add("node-palette");

        Label title = new Label("Nodes");
        title.getStyleClass().add("palette-title");
        palette.getChildren().add(title);

        // Triggers section
        palette.getChildren().add(createPaletteSection("Triggers",
                createPaletteItem("Manual Trigger", "manualTrigger", MaterialDesignP.PLAY_CIRCLE),
                createPaletteItem("Schedule", "scheduleTrigger", MaterialDesignC.CLOCK_OUTLINE),
                createPaletteItem("Webhook", "webhookTrigger", MaterialDesignW.WEBHOOK)));

        // Actions section
        palette.getChildren().add(createPaletteSection("Actions",
                createPaletteItem("HTTP Request", "httpRequest", MaterialDesignW.WEB),
                createPaletteItem("Code", "code", MaterialDesignC.CODE_TAGS),
                createPaletteItem("Execute Command", "executeCommand", MaterialDesignC.CONSOLE)));

        // Flow section
        palette.getChildren().add(createPaletteSection("Flow Control",
                createPaletteItem("If", "if", MaterialDesignC.CALL_SPLIT),
                createPaletteItem("Switch", "switch", MaterialDesignS.SWAP_HORIZONTAL),
                createPaletteItem("Merge", "merge", MaterialDesignC.CALL_MERGE),
                createPaletteItem("Loop", "loop", MaterialDesignR.REPEAT)));

        // Data section
        palette.getChildren().add(createPaletteSection("Data",
                createPaletteItem("Set", "set", MaterialDesignP.PENCIL),
                createPaletteItem("Filter", "filter", MaterialDesignF.FILTER_OUTLINE),
                createPaletteItem("Sort", "sort", MaterialDesignS.SORT_ASCENDING)));

        // AI section
        palette.getChildren().add(createPaletteSection("AI",
                createPaletteItem("LLM Chat", "llmChat", MaterialDesignR.ROBOT),
                createPaletteItem("Text Classifier", "textClassifier", MaterialDesignT.TAG_TEXT_OUTLINE),
                createPaletteItem("Embedding", "embedding", MaterialDesignV.VECTOR_BEZIER),
                createPaletteItem("RAG", "rag", MaterialDesignB.BOOK_SEARCH)));

        // Advanced section (Phase 6 executors)
        palette.getChildren().add(createPaletteSection("Advanced",
                createPaletteItem("Subworkflow", "subworkflow", MaterialDesignS.SITEMAP),
                createPaletteItem("Parallel", "parallel", MaterialDesignF.FORMAT_ALIGN_JUSTIFY),
                createPaletteItem("Try/Catch", "tryCatch", MaterialDesignS.SHIELD_CHECK),
                createPaletteItem("Retry", "retry", MaterialDesignR.REFRESH),
                createPaletteItem("Rate Limit", "rate_limit", MaterialDesignS.SPEEDOMETER)));

        return palette;
    }

    private TitledPane createPaletteSection(String title, javafx.scene.Node... items) {
        VBox content = new VBox(2);
        content.setPadding(Insets.EMPTY);
        content.getChildren().addAll(items);

        TitledPane section = new TitledPane(title, content);
        section.setCollapsible(true);
        section.setExpanded(true);
        section.getStyleClass().add("palette-section");

        return section;
    }

    private HBox createPaletteItem(String name, String nodeType, org.kordamp.ikonli.Ikon icon) {
        HBox item = new HBox(8);
        item.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        item.setPadding(new Insets(5, 10, 5, 10));
        item.getStyleClass().add("palette-item");

        FontIcon fontIcon = FontIcon.of(icon, 16);

        Label label = new Label(name);

        item.getChildren().addAll(fontIcon, label);

        // Drag to add node
        item.setOnDragDetected(e -> {
            item.startFullDrag();
            e.consume();
        });

        item.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                addNodeAtCenter(nodeType, name);
            }
        });

        // Enable dragging onto canvas
        item.setOnMouseReleased(e -> {
            if (e.getSceneX() > nodePalette.getWidth()) {
                Point2D canvasPoint = canvasPane.sceneToLocal(e.getSceneX(), e.getSceneY());
                addNode(nodeType, name, canvasPoint.getX(), canvasPoint.getY());
            }
        });

        return item;
    }

    private void setupCanvasInteraction() {
        // Pan with middle mouse or space+drag
        canvasPane.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.MIDDLE ||
                    (e.getButton() == MouseButton.PRIMARY && e.isShiftDown())) {
                isPanning = true;
                dragStartX = e.getX();
                dragStartY = e.getY();
                canvasPane.setCursor(javafx.scene.Cursor.CLOSED_HAND);
            }
        });

        canvasPane.setOnMouseDragged(e -> {
            if (isPanning) {
                double deltaX = e.getX() - dragStartX;
                double deltaY = e.getY() - dragStartY;
                translateX += deltaX;
                translateY += deltaY;
                updateCanvasTransform();
                dragStartX = e.getX();
                dragStartY = e.getY();
            }
            // Update connection drag if active
            if (isConnectionDragging) {
                updateConnectionDrag(e.getSceneX(), e.getSceneY());
            }
        });

        canvasPane.setOnMouseReleased(e -> {
            if (isPanning) {
                isPanning = false;
                canvasPane.setCursor(javafx.scene.Cursor.DEFAULT);
            }
            // Cancel connection drag if released on empty space
            if (isConnectionDragging) {
                endConnectionDrag();
            }
        });

        // Also track mouse movement for connection dragging
        canvasPane.setOnMouseMoved(e -> {
            if (isConnectionDragging) {
                updateConnectionDrag(e.getSceneX(), e.getSceneY());
            }
        });

        // Zoom with scroll
        canvasPane.setOnScroll(this::handleScroll);

        // Click on canvas to deselect or show context menu
        canvasPane.setOnMouseClicked(e -> {
            if (e.getTarget() == canvasPane || e.getTarget() == nodeLayer ||
                    e.getTarget() == connectionLayer || e.getTarget() == gridLayer) {
                if (e.getButton() == MouseButton.PRIMARY) {
                    deselectAll();
                } else if (e.getButton() == MouseButton.SECONDARY) {
                    showCanvasContextMenu(e.getScreenX(), e.getScreenY(), e.getX(), e.getY());
                }
            }
        });
    }

    /**
     * Show context menu for canvas (empty space).
     */
    private void showCanvasContextMenu(double screenX, double screenY, double canvasX, double canvasY) {
        ContextMenu contextMenu = new ContextMenu();

        // Add Node submenu
        Menu addNodeMenu = new Menu("Add Node");
        addNodeMenu.setGraphic(FontIcon.of(MaterialDesignP.PLUS, 14));

        // Triggers
        Menu triggersMenu = new Menu("Triggers");
        triggersMenu.getItems().addAll(
                createAddNodeMenuItem("Manual Trigger", "manualTrigger", canvasX, canvasY),
                createAddNodeMenuItem("Schedule Trigger", "scheduleTrigger", canvasX, canvasY),
                createAddNodeMenuItem("Webhook Trigger", "webhookTrigger", canvasX, canvasY));

        // Actions
        Menu actionsMenu = new Menu("Actions");
        actionsMenu.getItems().addAll(
                createAddNodeMenuItem("HTTP Request", "httpRequest", canvasX, canvasY),
                createAddNodeMenuItem("Code", "code", canvasX, canvasY),
                createAddNodeMenuItem("Execute Command", "executeCommand", canvasX, canvasY));

        // Flow Control
        Menu flowMenu = new Menu("Flow Control");
        flowMenu.getItems().addAll(
                createAddNodeMenuItem("If", "if", canvasX, canvasY),
                createAddNodeMenuItem("Switch", "switch", canvasX, canvasY),
                createAddNodeMenuItem("Merge", "merge", canvasX, canvasY),
                createAddNodeMenuItem("Loop", "loop", canvasX, canvasY));

        // Data
        Menu dataMenu = new Menu("Data");
        dataMenu.getItems().addAll(
                createAddNodeMenuItem("Set", "set", canvasX, canvasY),
                createAddNodeMenuItem("Filter", "filter", canvasX, canvasY),
                createAddNodeMenuItem("Sort", "sort", canvasX, canvasY));

        // AI
        Menu aiMenu = new Menu("AI");
        aiMenu.getItems().addAll(
                createAddNodeMenuItem("LLM Chat", "llmChat", canvasX, canvasY),
                createAddNodeMenuItem("Text Classifier", "textClassifier", canvasX, canvasY));

        addNodeMenu.getItems().addAll(triggersMenu, actionsMenu, flowMenu, dataMenu, aiMenu);

        // Auto-layout
        MenuItem autoLayoutItem = new MenuItem("Auto Layout");
        autoLayoutItem.setGraphic(FontIcon.of(MaterialDesignA.AUTO_FIX, 14));
        autoLayoutItem.setOnAction(e -> autoLayoutNodes());

        // Fit to View
        MenuItem fitViewItem = new MenuItem("Fit to View");
        fitViewItem.setGraphic(FontIcon.of(MaterialDesignF.FIT_TO_PAGE, 14));
        fitViewItem.setOnAction(e -> fitToView());

        // Select All
        MenuItem selectAllItem = new MenuItem("Select All");
        selectAllItem.setOnAction(e -> selectAll());

        // Deselect All
        MenuItem deselectAllItem = new MenuItem("Deselect All");
        deselectAllItem.setOnAction(e -> deselectAll());

        // Copy
        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setGraphic(FontIcon.of(MaterialDesignC.CONTENT_COPY, 14));
        copyItem.setOnAction(e -> copySelected());

        // Paste (if something in clipboard)
        MenuItem pasteItem = new MenuItem("Paste");
        pasteItem.setGraphic(FontIcon.of(MaterialDesignC.CONTENT_PASTE, 14));
        pasteItem.setDisable(clipboardNodes.isEmpty());
        pasteItem.setOnAction(e -> pasteNodes());

        contextMenu.getItems().addAll(
                addNodeMenu,
                new SeparatorMenuItem(),
                autoLayoutItem,
                fitViewItem,
                new SeparatorMenuItem(),
                selectAllItem,
                deselectAllItem,
                new SeparatorMenuItem(),
                copyItem,
                pasteItem);

        contextMenu.show(canvasPane, screenX, screenY);
    }

    private MenuItem createAddNodeMenuItem(String name, String type, double x, double y) {
        MenuItem item = new MenuItem(name);
        item.setOnAction(e -> addNode(type, name, x, y));
        return item;
    }

    private void handleScroll(ScrollEvent e) {
        if (e.isControlDown()) {
            // Zoom
            double delta = e.getDeltaY() > 0 ? ZOOM_FACTOR : -ZOOM_FACTOR;
            double newScale = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, scale + delta));

            if (newScale != scale) {
                scale = newScale;
                updateCanvasTransform();
            }
            e.consume();
        }
    }

    private void updateCanvasTransform() {
        nodeLayer.setScaleX(scale);
        nodeLayer.setScaleY(scale);
        connectionLayer.setScaleX(scale);
        connectionLayer.setScaleY(scale);

        nodeLayer.setTranslateX(translateX);
        nodeLayer.setTranslateY(translateY);
        connectionLayer.setTranslateX(translateX);
        connectionLayer.setTranslateY(translateY);
    }

    private void setupKeyboardShortcuts() {
        setOnKeyPressed(e -> {
            if (e.isControlDown()) {
                switch (e.getCode()) {
                    case C -> {
                        if (!selectedNodes.isEmpty()) {
                            copySelected();
                        }
                        e.consume();
                    }
                    case V -> {
                        pasteNodes();
                        e.consume();
                    }
                    case A -> {
                        selectAll();
                        e.consume();
                    }
                }
            } else {
                switch (e.getCode()) {
                    case DELETE, BACK_SPACE -> deleteSelected();
                    case ESCAPE -> deselectAll();
                    default -> {
                    }
                }
            }
        });
        setFocusTraversable(true);
    }

    private void drawGrid() {
        gridLayer.getChildren().clear();

        double width = 2000;
        double height = 2000;

        // Draw grid lines
        for (double x = 0; x < width; x += GRID_SIZE) {
            javafx.scene.shape.Line line = new javafx.scene.shape.Line(x, 0, x, height);
            line.setStroke(Color.gray(0.3, 0.3));
            line.setStrokeWidth(0.5);
            gridLayer.getChildren().add(line);
        }

        for (double y = 0; y < height; y += GRID_SIZE) {
            javafx.scene.shape.Line line = new javafx.scene.shape.Line(0, y, width, y);
            line.setStroke(Color.gray(0.3, 0.3));
            line.setStrokeWidth(0.5);
            gridLayer.getChildren().add(line);
        }
    }

    public void newWorkflow() {
        workflow = WorkflowDTO.create("New Workflow");
        nodeViews.clear();
        connectionLines.clear();
        nodeLayer.getChildren().clear();
        connectionLayer.getChildren().clear();

        // Add a default trigger node
        addNode("manualTrigger", "Manual Trigger", 100, 100);
    }

    public void loadWorkflow(WorkflowDTO workflow) {
        this.workflow = workflow;
        nodeViews.clear();
        connectionLines.clear();
        nodeLayer.getChildren().clear();
        connectionLayer.getChildren().clear();

        // Load nodes
        for (Node node : workflow.nodes()) {
            NodeView nodeView = new NodeView(node, this);
            nodeViews.put(node.id(), nodeView);
            nodeLayer.getChildren().add(nodeView);
        }

        // Load connections
        for (Connection connection : workflow.connections()) {
            createConnectionLine(connection);
        }

        showStatus("Loaded: " + workflow.name());
    }

    public void saveWorkflow() {
        try {
            WorkflowDTO saved;
            if (workflow.id() == null) {
                // New workflow - prompt for name if needed
                String name = workflow.name();
                if (name == null || name.isBlank() || name.equals("New Workflow")) {
                    TextInputDialog dialog = new TextInputDialog("My Workflow");
                    dialog.setTitle("Save Workflow");
                    dialog.setHeaderText("Enter workflow name:");
                    dialog.setContentText("Name:");

                    var result = dialog.showAndWait();
                    if (result.isEmpty()) {
                        return; // User cancelled
                    }
                    name = result.get();
                    workflow = workflow.withName(name);
                }
                saved = workflowService.create(workflow);
            } else {
                // Update existing workflow
                saved = workflowService.update(workflow);
            }
            this.workflow = saved;
            showStatus("Saved: " + saved.name());

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Workflow Saved");
            alert.setHeaderText(null);
            alert.setContentText("Workflow '" + saved.name() + "' saved successfully!");
            alert.showAndWait();
        } catch (Exception e) {
            showStatus("Error saving: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Save Error");
            alert.setHeaderText("Failed to save workflow");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    public void runWorkflow() {
        if (workflow.id() == null) {
            // Must save first
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Save Required");
            alert.setHeaderText("Workflow must be saved before running");
            alert.setContentText("Please save the workflow first.");
            alert.showAndWait();
            return;
        }

        try {
            showStatus("Running: " + workflow.name() + "...");

            // Execute asynchronously
            executionService.executeAsync(workflow.id(), Map.of())
                    .thenAccept(result -> Platform.runLater(() -> {
                        if (result.status() == ExecutionStatus.SUCCESS) {
                            showStatus("Completed: " + workflow.name());
                            showExecutionResult(result);
                        } else {
                            showStatus("Failed: " + workflow.name());
                            showExecutionError(result);
                        }
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            showStatus("Error: " + ex.getMessage());
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Execution Error");
                            alert.setHeaderText("Workflow execution failed");
                            alert.setContentText(ex.getMessage());
                            alert.showAndWait();
                        });
                        return null;
                    });
        } catch (Exception e) {
            showStatus("Error: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Execution Error");
            alert.setHeaderText("Failed to start workflow execution");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private void showStatus(String message) {
        statusLabel.setText(message);
    }

    private void showExecutionResult(ExecutionDTO result) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Execution Complete");
        alert.setHeaderText("Workflow executed successfully");
        alert.setContentText("Duration: " +
                (result.finishedAt() != null && result.startedAt() != null
                        ? java.time.Duration.between(result.startedAt(), result.finishedAt()).toMillis() + "ms"
                        : "N/A"));
        alert.showAndWait();
    }

    private void showExecutionError(ExecutionDTO result) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Execution Failed");
        alert.setHeaderText("Workflow execution failed");
        alert.setContentText("Status: " + result.status() +
                (result.errorMessage() != null ? "\nError: " + result.errorMessage() : ""));
        alert.showAndWait();
    }

    public void exportWorkflow() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Export Workflow");
        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("JSON Files", "*.json"));
        fileChooser.setInitialFileName(workflow.name().replaceAll("[^a-zA-Z0-9]", "_") + ".json");

        // Set initial directory to last used location
        if (lastExportDirectory != null && lastExportDirectory.exists()) {
            fileChooser.setInitialDirectory(lastExportDirectory);
        }

        java.io.File file = fileChooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            // Remember the directory for next time
            lastExportDirectory = file.getParentFile();

            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
                mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

                mapper.writeValue(file, workflow);
                showStatus("Exported: " + file.getName());

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Successful");
                alert.setHeaderText(null);
                alert.setContentText("Workflow exported to:\n" + file.getAbsolutePath());
                alert.showAndWait();
            } catch (Exception e) {
                showStatus("Export failed: " + e.getMessage());
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Export Error");
                alert.setHeaderText("Failed to export workflow");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
        }
    }

    public void importWorkflow() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Import Workflow");
        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("JSON Files", "*.json"));

        // Set initial directory to last used location
        if (lastImportDirectory != null && lastImportDirectory.exists()) {
            fileChooser.setInitialDirectory(lastImportDirectory);
        }

        java.io.File file = fileChooser.showOpenDialog(getScene().getWindow());
        if (file != null) {
            // Remember the directory for next time
            lastImportDirectory = file.getParentFile();

            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

                WorkflowDTO imported = mapper.readValue(file, WorkflowDTO.class);

                // Create a new workflow (no ID) from the imported data
                WorkflowDTO newWorkflow = new WorkflowDTO(
                        null, // New ID will be assigned on save
                        imported.name() + " (Imported)",
                        imported.description(),
                        imported.nodes(),
                        imported.connections(),
                        imported.settings(),
                        false, // Not active by default
                        imported.triggerType(),
                        imported.cronExpression(),
                        null, null, null, 1);

                loadWorkflow(newWorkflow);
                showStatus("Imported: " + newWorkflow.name());

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Import Successful");
                alert.setHeaderText(null);
                alert.setContentText(
                        "Workflow '" + newWorkflow.name() + "' imported.\nRemember to save to persist changes.");
                alert.showAndWait();
            } catch (Exception e) {
                showStatus("Import failed: " + e.getMessage());
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Import Error");
                alert.setHeaderText("Failed to import workflow");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void addNode(String type, String name, double x, double y) {
        String id = UUID.randomUUID().toString();
        Node node = new Node(
                id, type, name,
                new Node.Position(snapToGrid(x), snapToGrid(y)),
                Map.of(), null, false, null);

        // Update workflow
        workflow = workflow.withAddedNode(node);

        // Create view
        NodeView nodeView = new NodeView(node, this);
        nodeViews.put(id, nodeView);
        nodeLayer.getChildren().add(nodeView);
    }

    private void addNodeAtCenter(String type, String name) {
        double centerX = canvasPane.getWidth() / 2 - translateX;
        double centerY = canvasPane.getHeight() / 2 - translateY;
        addNode(type, name, centerX, centerY);
    }

    private double snapToGrid(double value) {
        return Math.round(value / GRID_SIZE) * GRID_SIZE;
    }

    public void selectNode(NodeView nodeView) {
        selectNode(nodeView, false);
    }

    public void selectNode(NodeView nodeView, boolean multiSelect) {
        if (!multiSelect) {
            deselectAll();
        }

        if (selectedNodes.contains(nodeView)) {
            // Deselect if already selected
            selectedNodes.remove(nodeView);
            nodeView.setSelected(false);
        } else {
            // Select the node
            selectedNodes.add(nodeView);
            nodeView.setSelected(true);
        }

        // Show properties panel for single selection, hide for multi-selection
        if (selectedNodes.size() == 1) {
            propertiesPanel.show(selectedNodes.iterator().next());
        } else {
            propertiesPanel.hide();
        }
    }

    public void deselectAll() {
        for (NodeView nodeView : selectedNodes) {
            nodeView.setSelected(false);
        }
        selectedNodes.clear();
        // Hide properties panel
        propertiesPanel.hide();
    }

    private void deleteSelected() {
        if (!selectedNodes.isEmpty()) {
            // Collect all node IDs to delete
            java.util.Set<String> nodeIdsToDelete = selectedNodes.stream()
                    .map(nodeView -> nodeView.getNode().id())
                    .collect(java.util.stream.Collectors.toSet());

            // Remove from workflow
            var newNodes = workflow.nodes().stream()
                    .filter(n -> !nodeIdsToDelete.contains(n.id()))
                    .toList();
            var newConnections = workflow.connections().stream()
                    .filter(c -> !nodeIdsToDelete.contains(c.sourceNodeId()) &&
                            !nodeIdsToDelete.contains(c.targetNodeId()))
                    .toList();
            workflow = new WorkflowDTO(
                    workflow.id(), workflow.name(), workflow.description(),
                    newNodes, newConnections, workflow.settings(),
                    workflow.isActive(), workflow.triggerType(), workflow.cronExpression(),
                    workflow.createdAt(), workflow.updatedAt(), workflow.lastExecuted(),
                    workflow.version());

            // Remove views
            for (NodeView nodeView : selectedNodes) {
                nodeLayer.getChildren().remove(nodeView);
                nodeViews.remove(nodeView.getNode().id());
            }

            // Remove connections
            connectionLines.entrySet().removeIf(entry -> {
                if (nodeIdsToDelete.stream().anyMatch(entry.getValue()::involvesNode)) {
                    connectionLayer.getChildren().remove(entry.getValue());
                    return true;
                }
                return false;
            });

            selectedNodes.clear();
        }
    }

    public void updateNodePosition(String nodeId, double x, double y) {
        var newNodes = workflow.nodes().stream()
                .map(n -> n.id().equals(nodeId)
                        ? n.withPosition(new Node.Position(snapToGrid(x), snapToGrid(y)))
                        : n)
                .toList();
        workflow = workflow.withNodes(newNodes);

        // Update connection lines
        updateConnectionsForNode(nodeId);
    }

    private void createConnectionLine(Connection connection) {
        NodeView source = nodeViews.get(connection.sourceNodeId());
        NodeView target = nodeViews.get(connection.targetNodeId());

        if (source != null && target != null) {
            ConnectionLine line = new ConnectionLine(connection, source, target);
            connectionLines.put(connection.id(), line);
            connectionLayer.getChildren().add(line);
        }
    }

    private void updateConnectionsForNode(String nodeId) {
        connectionLines.values().stream()
                .filter(line -> line.involvesNode(nodeId))
                .forEach(ConnectionLine::updatePosition);
    }

    // ==================== Connection Dragging System ====================

    /**
     * Check if a connection drag is in progress.
     */
    public boolean isConnectionDragging() {
        return isConnectionDragging;
    }

    /**
     * Start dragging a new connection from a source node's output handle.
     */
    public void startConnectionDrag(NodeView source) {
        if (!source.canBeConnectionSource()) {
            return;
        }

        System.out.println("Starting connection drag from: " + source.getNode().name());

        isConnectionDragging = true;
        connectionSource = source;

        // Create temporary connection line
        tempConnectionLine = new CubicCurve();
        tempConnectionLine.setFill(null);
        tempConnectionLine.setStroke(Color.web("#4a9eff"));
        tempConnectionLine.setStrokeWidth(2);
        tempConnectionLine.setStrokeDashOffset(0);
        tempConnectionLine.getStrokeDashArray().addAll(5.0, 5.0); // Dashed while dragging
        tempConnectionLine.setMouseTransparent(true);

        // Set start position
        double startX = source.getOutputX();
        double startY = source.getOutputY();
        tempConnectionLine.setStartX(startX);
        tempConnectionLine.setStartY(startY);
        tempConnectionLine.setControlX1(startX + 50);
        tempConnectionLine.setControlY1(startY);
        tempConnectionLine.setEndX(startX + 50);
        tempConnectionLine.setEndY(startY);
        tempConnectionLine.setControlX2(startX + 50);
        tempConnectionLine.setControlY2(startY);

        connectionLayer.getChildren().add(tempConnectionLine);

        // Highlight valid targets
        highlightValidTargets(source, true);
    }

    /**
     * Update the temporary connection line position during drag.
     */
    public void updateConnectionDrag(double sceneX, double sceneY) {
        if (!isConnectionDragging || tempConnectionLine == null || connectionSource == null) {
            return;
        }

        // Convert scene coordinates to canvas coordinates
        Point2D canvasPoint = connectionLayer.sceneToLocal(sceneX, sceneY);
        double endX = canvasPoint.getX();
        double endY = canvasPoint.getY();

        double startX = connectionSource.getOutputX();
        double startY = connectionSource.getOutputY();

        // Update bezier curve
        double dx = Math.abs(endX - startX) * 0.5;
        tempConnectionLine.setControlX1(startX + dx);
        tempConnectionLine.setControlY1(startY);
        tempConnectionLine.setControlX2(endX - dx);
        tempConnectionLine.setControlY2(endY);
        tempConnectionLine.setEndX(endX);
        tempConnectionLine.setEndY(endY);
    }

    /**
     * End the connection drag - check if hovering over valid target.
     */
    public void endConnectionDrag() {
        // Check if we're hovering over a valid target
        if (hoveredTarget != null && connectionSource != null && connectionSource.canConnectTo(hoveredTarget)) {
            System.out.println("End drag with hovered target: " + hoveredTarget.getNode().name());
            // Create the connection
            String connectionId = UUID.randomUUID().toString();
            Connection connection = Connection.simple(connectionId,
                    connectionSource.getNode().id(),
                    hoveredTarget.getNode().id());
            workflow = workflow.withAddedConnection(connection);
            createConnectionLine(connection);
            System.out.println("Created connection: " + connectionSource.getNode().name() +
                    " -> " + hoveredTarget.getNode().name());
        } else if (connectionSource != null && tempConnectionLine != null) {
            // Try to find target under the mouse cursor
            NodeView target = findTargetNodeAtPoint(tempConnectionLine.getEndX(), tempConnectionLine.getEndY());
            if (target != null && connectionSource.canConnectTo(target)) {
                System.out.println("Found target at release point: " + target.getNode().name());
                String connectionId = UUID.randomUUID().toString();
                Connection connection = Connection.simple(connectionId,
                        connectionSource.getNode().id(),
                        target.getNode().id());
                workflow = workflow.withAddedConnection(connection);
                createConnectionLine(connection);
                System.out.println("Created connection: " + connectionSource.getNode().name() +
                        " -> " + target.getNode().name());
            }
        }

        // Clean up
        if (tempConnectionLine != null) {
            connectionLayer.getChildren().remove(tempConnectionLine);
            tempConnectionLine = null;
        }

        if (connectionSource != null) {
            highlightValidTargets(connectionSource, false);
        }

        isConnectionDragging = false;
        connectionSource = null;
        hoveredTarget = null;
    }

    /**
     * Find a node at the given canvas coordinates that can be a connection target.
     */
    private NodeView findTargetNodeAtPoint(double x, double y) {
        for (NodeView nodeView : nodeViews.values()) {
            if (nodeView == connectionSource)
                continue;
            if (!nodeView.canBeConnectionTarget())
                continue;

            // Check if point is within the input handle area (left side of node)
            double nodeX = nodeView.getLayoutX();
            double nodeY = nodeView.getLayoutY();
            double inputCenterX = nodeView.getInputX();
            double inputCenterY = nodeView.getInputY();

            // Generous hit area around input handle (25 pixel radius)
            double distance = Math.sqrt(Math.pow(x - inputCenterX, 2) + Math.pow(y - inputCenterY, 2));
            if (distance <= 25) {
                return nodeView;
            }
        }
        return null;
    }

    /**
     * Set the currently hovered target node during connection drag.
     */
    public void setHoveredTarget(NodeView target) {
        this.hoveredTarget = target;
    }

    /**
     * Complete a connection to the target node.
     */
    public void completeConnection(NodeView target) {
        if (!isConnectionDragging || connectionSource == null) {
            return;
        }

        // Validate connection
        if (!connectionSource.canConnectTo(target)) {
            showConnectionError("Cannot connect these nodes");
            endConnectionDrag();
            return;
        }

        // Create the connection
        String connectionId = UUID.randomUUID().toString();
        Connection connection = Connection.simple(connectionId,
                connectionSource.getNode().id(),
                target.getNode().id());

        // Add to workflow
        workflow = workflow.withAddedConnection(connection);

        // Create visual connection
        createConnectionLine(connection);

        System.out.println("Created connection: " + connectionSource.getNode().name() +
                " -> " + target.getNode().name());

        // Clean up
        endConnectionDrag();
    }

    /**
     * Check if a connection already exists between two nodes.
     */
    public boolean hasConnection(String sourceId, String targetId) {
        return workflow.connections().stream()
                .anyMatch(c -> c.sourceNodeId().equals(sourceId) &&
                        c.targetNodeId().equals(targetId));
    }

    /**
     * Highlight all valid connection targets.
     */
    private void highlightValidTargets(NodeView source, boolean highlight) {
        for (NodeView nodeView : nodeViews.values()) {
            if (nodeView != source && source.canConnectTo(nodeView)) {
                if (highlight) {
                    nodeView.getStyleClass().add("valid-target");
                    // Make input handle larger and more visible
                    nodeView.highlightAsTarget(true);
                } else {
                    nodeView.getStyleClass().remove("valid-target");
                    nodeView.highlightAsTarget(false);
                }
            }
        }
    }

    /**
     * Show a connection error message.
     */
    private void showConnectionError(String message) {
        // Create a brief tooltip-style message near mouse
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Connection Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Delete a connection by its ID.
     */
    public void deleteConnection(String connectionId) {
        ConnectionLine line = connectionLines.remove(connectionId);
        if (line != null) {
            connectionLayer.getChildren().remove(line);

            // Remove from workflow
            var newConnections = workflow.connections().stream()
                    .filter(c -> !c.id().equals(connectionId))
                    .toList();
            workflow = workflow.withConnections(newConnections);
        }
    }

    // ==================== Node Action Methods ====================

    /**
     * Open the node editor panel/dialog.
     */
    public void openNodeEditor(NodeView nodeView) {
        Node node = nodeView.getNode();
        System.out.println("Opening editor for: " + node.name() + " [" + node.type() + "]");

        // Create and show editor dialog
        Dialog<Map<String, Object>> dialog = new Dialog<>();
        dialog.setTitle("Edit Node: " + node.name());
        dialog.setHeaderText("Configure " + node.type());

        // Create content
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setPrefWidth(450);

        // Header with name and help button
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label nameLabel = new Label("Name:");
        TextField nameField = new TextField(node.name());
        nameField.setPrefWidth(280);

        // Help button
        Button helpButton = new Button();
        FontIcon helpIcon = FontIcon.of(MaterialDesignH.HELP_CIRCLE_OUTLINE, 18);
        helpIcon.setIconColor(Color.web("#60a5fa"));
        helpButton.setGraphic(helpIcon);
        helpButton.setStyle("""
                -fx-background-color: transparent;
                -fx-padding: 5;
                -fx-cursor: hand;
                """);
        Tooltip helpTooltip = new Tooltip("Show help & examples for this node");
        helpTooltip.setShowDelay(javafx.util.Duration.millis(300));
        Tooltip.install(helpButton, helpTooltip);
        helpButton.setOnAction(e -> showNodeHelpDialog(node.type()));

        // Spacer to push help button to right
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        headerBox.getChildren().addAll(nameLabel, nameField, spacer, helpButton);

        // Node-specific parameters based on type
        Label paramsLabel = new Label("Parameters:");
        TextArea paramsArea = new TextArea();
        paramsArea.setPromptText("JSON parameters for this node...");
        paramsArea.setPrefRowCount(6);

        // Format existing parameters as JSON
        if (node.parameters() != null && !node.parameters().isEmpty()) {
            try {
                StringBuilder sb = new StringBuilder();
                node.parameters().forEach((k, v) -> sb.append(k).append(": ").append(v).append("\n"));
                paramsArea.setText(sb.toString());
            } catch (Exception ignored) {
            }
        }

        // Add node type specific help (short version)
        Label helpLabel = new Label(getNodeTypeHelp(node.type()));
        helpLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 11;");
        helpLabel.setWrapText(true);

        content.getChildren().addAll(headerBox, paramsLabel, paramsArea, helpLabel);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                // Return the updated values
                return Map.of("name", nameField.getText());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            String newName = (String) result.get("name");
            if (newName != null && !newName.equals(node.name())) {
                // Update node name
                updateNodeName(node.id(), newName);
                // Refresh the view
                nodeView.getNode(); // Need to rebuild the view
            }
        });
    }

    private String getNodeTypeHelp(String type) {
        return switch (type) {
            case "manualTrigger" -> "Starts the workflow when manually triggered.";
            case "scheduleTrigger" -> "Runs on a schedule. Set cron expression in parameters.";
            case "webhookTrigger" -> "Listens for HTTP webhook calls.";
            case "httpRequest" -> "Make HTTP requests. Set url, method, headers, body.";
            case "code" -> "Execute JavaScript or Python code. Access input via 'items'.";
            case "executeCommand" -> "Run shell commands on the system.";
            case "if" -> "Branch based on conditions. Set condition in parameters.";
            case "switch" -> "Multiple branches based on value matching.";
            case "merge" -> "Merge multiple input branches into one.";
            case "loop" -> "Iterate over items. Set batchSize for parallel processing.";
            case "set" -> "Set or modify data values.";
            case "filter" -> "Filter items based on conditions.";
            case "sort" -> "Sort items by specified field.";
            case "llmChat" -> "Chat with LLM models. Set model, prompt, temperature.";
            case "textClassifier" -> "Classify text into categories using AI.";
            case "embedding" -> "Generate vector embeddings for semantic search.";
            case "rag" -> "Retrieval-Augmented Generation for document Q&A.";
            default -> "Configure this node's behavior.";
        };
    }

    /**
     * Show detailed help dialog for a node type.
     */
    private void showNodeHelpDialog(String nodeType) {
        NodeHelp help = NodeHelpProvider.getHelp(nodeType);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Help: " + help.title());
        dialog.setHeaderText(help.shortDescription());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // Create content
        VBox content = new VBox(15);
        content.setPrefWidth(600);
        content.setStyle("-fx-padding: 10;");

        // Description section
        Label descTitle = new Label("Description");
        descTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        TextArea descArea = new TextArea(help.detailedDescription());
        descArea.setWrapText(true);
        descArea.setEditable(false);
        descArea.setPrefRowCount(8);
        descArea.setStyle("-fx-font-family: 'Segoe UI', sans-serif;");

        // Sample code section
        Label codeTitle = new Label("Sample Usage / Code");
        codeTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        TextArea codeArea = new TextArea(help.sampleCode());
        codeArea.setWrapText(true);
        codeArea.setEditable(false);
        codeArea.setPrefRowCount(12);
        codeArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 12px;");

        content.getChildren().addAll(descTitle, descArea, codeTitle, codeArea);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(450);

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().setPrefSize(650, 550);

        dialog.showAndWait();
    }

    private void updateNodeName(String nodeId, String newName) {
        var newNodes = workflow.nodes().stream()
                .map(n -> n.id().equals(nodeId)
                        ? new Node(n.id(), n.type(), newName, n.position(), n.parameters(),
                                n.credentialId(), n.disabled(), n.notes())
                        : n)
                .toList();
        workflow = workflow.withNodes(newNodes);
    }

    /**
     * Execute a single node for testing.
     */
    public void executeNode(NodeView nodeView) {
        Node node = nodeView.getNode();
        System.out.println("Executing node: " + node.name());

        // Create temporary workflow with single node for execution
        WorkflowDTO tempWorkflow = WorkflowDTO.create("Single Node Test")
                .withAddedNode(node);

        try {
            // Execute asynchronously
            executionService.executeAsync(tempWorkflow.id(), Map.of())
                    .thenAccept(result -> Platform.runLater(() -> {
                        if (result.status() == ExecutionStatus.SUCCESS) {
                            showExecutionResult(result);
                        } else {
                            showExecutionError(result);
                        }
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Execution Error");
                            alert.setHeaderText("Failed to execute node");
                            alert.setContentText(ex.getMessage());
                            alert.showAndWait();
                        });
                        return null;
                    });
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Execution Error");
            alert.setHeaderText("Failed to start node execution");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Duplicate a node.
     */
    public void duplicateNode(NodeView nodeView) {
        Node original = nodeView.getNode();

        // Create new node with offset position
        String newId = UUID.randomUUID().toString();
        Node duplicate = new Node(
                newId,
                original.type(),
                original.name() + " (copy)",
                new Node.Position(original.position().x() + 50, original.position().y() + 50),
                original.parameters() != null ? new java.util.HashMap<>(original.parameters()) : Map.of(),
                original.credentialId(),
                original.disabled(),
                original.notes());

        // Add to workflow
        workflow = workflow.withAddedNode(duplicate);

        // Create view
        NodeView newView = new NodeView(duplicate, this);
        nodeViews.put(newId, newView);
        nodeLayer.getChildren().add(newView);

        // Select the new node
        selectNode(newView);

        System.out.println("Duplicated node: " + original.name());
    }

    /**
     * Copy selected nodes to clipboard.
     */
    public void copySelected() {
        clipboardNodes.clear();
        for (NodeView nodeView : selectedNodes) {
            clipboardNodes.add(nodeView.getNode());
        }
        updatePasteMenuState();
    }

    /**
     * Paste nodes from clipboard.
     */
    public void pasteNodes() {
        if (clipboardNodes.isEmpty()) {
            return;
        }

        // Calculate paste position (slightly offset from original positions)
        double baseX = 100;
        double baseY = 100;

        // Clear selection before pasting
        deselectAll();

        for (Node node : clipboardNodes) {
            String newId = UUID.randomUUID().toString();
            Node pastedNode = new Node(
                    newId,
                    node.type(),
                    node.name() + " (copy)",
                    new Node.Position(baseX, baseY),
                    node.parameters() != null ? new java.util.HashMap<>(node.parameters()) : Map.of(),
                    node.credentialId(),
                    node.disabled(),
                    node.notes());

            // Add to workflow
            workflow = workflow.withAddedNode(pastedNode);

            // Create view
            NodeView newView = new NodeView(pastedNode, this);
            nodeViews.put(newId, newView);
            nodeLayer.getChildren().add(newView);

            // Select the pasted node
            selectedNodes.add(newView);
            newView.setSelected(true);

            // Offset next node position
            baseX += 50;
            baseY += 50;
        }

        // Update properties panel for multi-selection
        if (selectedNodes.size() == 1) {
            propertiesPanel.show(selectedNodes.iterator().next());
        } else {
            propertiesPanel.hide();
        }

        updateMinimap();
    }

    /**
     * Update paste menu item state based on clipboard contents.
     */
    private void updatePasteMenuState() {
        // This will be called when the context menu is shown
    }

    /**
     * Toggle node enabled/disabled state.
     */
    public void toggleNodeEnabled(NodeView nodeView) {
        Node node = nodeView.getNode();
        boolean newState = !node.disabled();

        var newNodes = workflow.nodes().stream()
                .map(n -> n.id().equals(node.id())
                        ? new Node(n.id(), n.type(), n.name(), n.position(), n.parameters(),
                                n.credentialId(), newState, n.notes())
                        : n)
                .toList();
        workflow = workflow.withNodes(newNodes);

        // Update visual
        if (newState) {
            nodeView.setOpacity(0.5);
            nodeView.getStyleClass().add("disabled");
        } else {
            nodeView.setOpacity(1.0);
            nodeView.getStyleClass().remove("disabled");
        }

        System.out.println("Node " + node.name() + " " + (newState ? "disabled" : "enabled"));
    }

    /**
     * Rename a node.
     */
    public void renameNode(NodeView nodeView) {
        Node node = nodeView.getNode();

        TextInputDialog dialog = new TextInputDialog(node.name());
        dialog.setTitle("Rename Node");
        dialog.setHeaderText("Enter new name:");
        dialog.setContentText("Name:");

        dialog.showAndWait().ifPresent(newName -> {
            if (!newName.isBlank() && !newName.equals(node.name())) {
                updateNodeName(node.id(), newName);
                // Need to refresh the node view - recreate it
                nodeLayer.getChildren().remove(nodeView);
                nodeViews.remove(node.id());

                Node updatedNode = workflow.findNode(node.id());
                if (updatedNode != null) {
                    NodeView newView = new NodeView(updatedNode, this);
                    nodeViews.put(node.id(), newView);
                    nodeLayer.getChildren().add(newView);
                    selectNode(newView);
                }
            }
        });
    }

    /**
     * Delete a specific node.
     */
    public void deleteNode(NodeView nodeView) {
        String nodeId = nodeView.getNode().id();

        // Remove from workflow
        var newNodes = workflow.nodes().stream()
                .filter(n -> !n.id().equals(nodeId))
                .toList();
        var newConnections = workflow.connections().stream()
                .filter(c -> !c.involvesNode(nodeId))
                .toList();
        workflow = new WorkflowDTO(
                workflow.id(), workflow.name(), workflow.description(),
                newNodes, newConnections, workflow.settings(),
                workflow.isActive(), workflow.triggerType(), workflow.cronExpression(),
                workflow.createdAt(), workflow.updatedAt(), workflow.lastExecuted(),
                workflow.version());

        // Remove view
        nodeLayer.getChildren().remove(nodeView);
        nodeViews.remove(nodeId);

        // Remove connections
        connectionLines.entrySet().removeIf(entry -> {
            if (entry.getValue().involvesNode(nodeId)) {
                connectionLayer.getChildren().remove(entry.getValue());
                return true;
            }
            return false;
        });

        selectedNodes.remove(nodeView);

        System.out.println("Deleted node: " + nodeView.getNode().name());
    }

    /**
     * Select all nodes.
     */
    public void selectAll() {
        deselectAll();
        for (NodeView nodeView : nodeViews.values()) {
            selectedNodes.add(nodeView);
            nodeView.setSelected(true);
        }
        // Hide properties panel for multi-selection
        propertiesPanel.hide();
    }

    /**
     * Auto-layout nodes in a nice arrangement.
     */
    public void autoLayoutNodes() {
        // Simple left-to-right layout based on connections
        System.out.println("Auto-layout nodes");

        // Find trigger nodes (no incoming connections)
        var triggerNodes = workflow.getTriggerNodes();

        double startX = 100;
        double startY = 100;
        double xSpacing = 250;
        double ySpacing = 120;

        // Layout connected nodes in columns using topological sort
        java.util.Set<String> visited = new java.util.HashSet<>();
        java.util.Queue<Node> queue = new java.util.LinkedList<>();
        java.util.Map<String, Integer> nodeColumns = new java.util.HashMap<>();

        // Mark trigger nodes as visited and in column 0
        for (Node trigger : triggerNodes) {
            visited.add(trigger.id());
            nodeColumns.put(trigger.id(), 0);
        }

        // Add trigger nodes to queue
        queue.addAll(triggerNodes);

        // Process nodes level by level (breadth-first)
        while (!queue.isEmpty()) {
            Node current = queue.poll();
            int currentColumn = nodeColumns.get(current.id());

            // Find all nodes that this node connects to (outgoing connections)
            for (Connection connection : workflow.connections()) {
                if (connection.sourceNodeId().equals(current.id())) {
                    String targetId = connection.targetNodeId();
                    if (!visited.contains(targetId)) {
                        visited.add(targetId);
                        nodeColumns.put(targetId, currentColumn + 1);
                        Node targetNode = workflow.findNode(targetId);
                        if (targetNode != null) {
                            queue.add(targetNode);
                        }
                    }
                }
            }
        }

        // Group nodes by column
        java.util.Map<Integer, java.util.List<Node>> columnGroups = new java.util.HashMap<>();
        for (Node node : workflow.nodes()) {
            int column = nodeColumns.getOrDefault(node.id(), 0);
            columnGroups.computeIfAbsent(column, k -> new java.util.ArrayList<>()).add(node);
        }

        // Layout nodes in each column
        for (java.util.Map.Entry<Integer, java.util.List<Node>> entry : columnGroups.entrySet()) {
            int column = entry.getKey();
            java.util.List<Node> nodesInColumn = entry.getValue();

            double columnX = startX + column * xSpacing;
            double columnY = startY;

            for (Node node : nodesInColumn) {
                NodeView view = nodeViews.get(node.id());
                if (view != null) {
                    view.setLayoutX(columnX);
                    view.setLayoutY(columnY);
                    updateNodePosition(node.id(), columnX, columnY);
                    columnY += ySpacing;
                }
            }
        }
    }

    /**
     * Fit all nodes into view.
     */
    public void fitToView() {
        if (nodeViews.isEmpty())
            return;

        // Calculate bounds of all nodes
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;

        for (NodeView view : nodeViews.values()) {
            minX = Math.min(minX, view.getLayoutX());
            minY = Math.min(minY, view.getLayoutY());
            maxX = Math.max(maxX, view.getLayoutX() + view.getWidth());
            maxY = Math.max(maxY, view.getLayoutY() + view.getHeight());
        }

        // Calculate required translation
        translateX = -minX + 50;
        translateY = -minY + 50;

        // Calculate required scale
        double contentWidth = maxX - minX + 100;
        double contentHeight = maxY - minY + 100;
        double viewWidth = canvasPane.getWidth();
        double viewHeight = canvasPane.getHeight();

        scale = Math.min(viewWidth / contentWidth, viewHeight / contentHeight);
        scale = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, scale));

        updateCanvasTransform();
    }

    public WorkflowDTO getWorkflow() {
        return workflow;
    }

    /**
     * Get the execution service.
     */
    public ExecutionServiceInterface getExecutionService() {
        return executionService;
    }

    /**
     * Update a node with new properties.
     */
    public void updateNode(NodeView nodeView, Node updatedNode) {
        String nodeId = updatedNode.id();

        // Update workflow nodes list
        var newNodes = workflow.nodes().stream()
                .map(n -> n.id().equals(nodeId) ? updatedNode : n)
                .toList();
        workflow = workflow.withNodes(newNodes);

        // Rebuild the node view
        nodeLayer.getChildren().remove(nodeView);
        nodeViews.remove(nodeId);

        NodeView newView = new NodeView(updatedNode, this);
        nodeViews.put(nodeId, newView);
        nodeLayer.getChildren().add(newView);

        // Re-select the new view
        selectNode(newView);

        // Update connections for this node
        updateConnectionsForNode(nodeId);
    }

    /**
     * Get the properties panel.
     */
    public NodePropertiesPanel getPropertiesPanel() {
        return propertiesPanel;
    }

    /**
     * Get a node view by its ID.
     */
    public NodeView getNodeViewById(String nodeId) {
        return nodeViews.get(nodeId);
    }

    /**
     * Reset all node execution states to IDLE.
     */
    public void resetAllNodeExecutionStates() {
        for (NodeView nodeView : nodeViews.values()) {
            nodeView.resetExecutionState();
        }
    }

    /**
     * Handle node state changes from ExecutionConsoleService.
     * This updates the visual state of nodes during execution.
     */
    @Override
    public void onNodeStateChanged(String nodeId, NodeState state) {
        Platform.runLater(() -> {
            NodeView nodeView = getNodeViewById(nodeId);
            if (nodeView != null) {
                ExecutionState executionState = switch (state) {
                    case RUNNING -> ExecutionState.RUNNING;
                    case SUCCESS -> ExecutionState.SUCCESS;
                    case FAILED -> ExecutionState.ERROR;
                    case SKIPPED -> ExecutionState.SKIPPED;
                    case IDLE -> ExecutionState.IDLE;
                };
                nodeView.setExecutionState(executionState);
            }
        });
    }

    /**
     * Get the execution history panel.
     */
    public ExecutionHistoryPanel getExecutionHistoryPanel() {
        return executionHistoryPanel;
    }

    /**
     * Toggle the execution history panel visibility.
     */
    public void toggleExecutionHistory() {
        executionHistoryPanel.toggle();
    }

    /**
     * Get all node views.
     */
    public java.util.Collection<NodeView> getNodeViews() {
        return nodeViews.values();
    }

    /**
     * Get the current viewport bounds.
     */
    public javafx.geometry.Bounds getViewportBounds() {
        return canvasScrollPane.getViewportBounds();
    }

    /**
     * Get current scroll X position.
     */
    public double getScrollX() {
        double hvalue = canvasScrollPane.getHvalue();
        double contentWidth = canvasPane.getWidth() - canvasScrollPane.getViewportBounds().getWidth();
        return hvalue * contentWidth;
    }

    /**
     * Get current scroll Y position.
     */
    public double getScrollY() {
        double vvalue = canvasScrollPane.getVvalue();
        double contentHeight = canvasPane.getHeight() - canvasScrollPane.getViewportBounds().getHeight();
        return vvalue * contentHeight;
    }

    /**
     * Center the canvas view on a specific position.
     */
    public void centerOn(double x, double y) {
        double viewportWidth = canvasScrollPane.getViewportBounds().getWidth();
        double viewportHeight = canvasScrollPane.getViewportBounds().getHeight();
        double contentWidth = canvasPane.getWidth();
        double contentHeight = canvasPane.getHeight();

        // Calculate scroll values to center on (x, y)
        double hvalue = (x - viewportWidth / 2) / (contentWidth - viewportWidth);
        double vvalue = (y - viewportHeight / 2) / (contentHeight - viewportHeight);

        // Clamp values
        hvalue = Math.max(0, Math.min(1, hvalue));
        vvalue = Math.max(0, Math.min(1, vvalue));

        canvasScrollPane.setHvalue(hvalue);
        canvasScrollPane.setVvalue(vvalue);
    }

    /**
     * Get the minimap component.
     */
    public CanvasMinimap getMinimap() {
        return minimap;
    }

    /**
     * Toggle minimap visibility.
     */
    public void toggleMinimap() {
        minimap.toggle();
    }

    /**
     * Update the minimap (call after node changes).
     */
    public void updateMinimap() {
        if (minimap != null && minimap.isVisible()) {
            minimap.update();
        }
    }

    // ==================== Undo/Redo Support ====================

    /**
     * Get the undo/redo manager.
     */
    public UndoRedoManager getUndoRedoManager() {
        return undoRedoManager;
    }

    /**
     * Undo the last action.
     */
    public void undo() {
        undoRedoManager.undo();
    }

    /**
     * Redo the last undone action.
     */
    public void redo() {
        undoRedoManager.redo();
    }

    /**
     * Create a node internally (for command pattern).
     */
    public io.toflowai.common.domain.Node createNodeInternal(String type, double x, double y) {
        String id = UUID.randomUUID().toString();
        String name = getDefaultNameForType(type);
        io.toflowai.common.domain.Node node = new io.toflowai.common.domain.Node(
                id, type, name, new io.toflowai.common.domain.Node.Position(x, y),
                java.util.Map.of(), null, false, "");

        workflow = workflow.withAddedNode(node);
        NodeView nodeView = new NodeView(node, this);
        nodeViews.put(id, nodeView);
        nodeLayer.getChildren().add(nodeView);
        updateMinimap();
        return node;
    }

    /**
     * Delete a node internally (for command pattern).
     */
    public void deleteNodeInternal(String nodeId) {
        NodeView nodeView = nodeViews.remove(nodeId);
        if (nodeView != null) {
            nodeLayer.getChildren().remove(nodeView);
        }

        // Remove connections involving this node
        workflow.connections().stream()
                .filter(c -> c.sourceNodeId().equals(nodeId) || c.targetNodeId().equals(nodeId))
                .map(io.toflowai.common.domain.Connection::id)
                .toList()
                .forEach(this::deleteConnectionInternal);

        workflow = workflow.withRemovedNode(nodeId);
        updateMinimap();
    }

    /**
     * Restore a deleted node (for undo).
     */
    public void restoreNode(io.toflowai.common.domain.Node node) {
        workflow = workflow.withAddedNode(node);
        NodeView nodeView = new NodeView(node, this);
        nodeViews.put(node.id(), nodeView);
        nodeLayer.getChildren().add(nodeView);
        updateMinimap();
    }

    /**
     * Get a node by ID.
     */
    public io.toflowai.common.domain.Node getNodeById(String nodeId) {
        return workflow.nodes().stream()
                .filter(n -> n.id().equals(nodeId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Set node position (for move command).
     */
    public void setNodePosition(String nodeId, double x, double y) {
        NodeView nodeView = nodeViews.get(nodeId);
        if (nodeView != null) {
            nodeView.setLayoutX(x);
            nodeView.setLayoutY(y);
            updateNodePosition(nodeId, x, y);
        }
    }

    /**
     * Create a connection internally (for command pattern).
     */
    public String createConnectionInternal(String sourceNodeId, String targetNodeId) {
        String id = UUID.randomUUID().toString();
        io.toflowai.common.domain.Connection connection = new io.toflowai.common.domain.Connection(
                id, sourceNodeId, "main", targetNodeId, "main");

        workflow = workflow.withAddedConnection(connection);

        NodeView source = nodeViews.get(sourceNodeId);
        NodeView target = nodeViews.get(targetNodeId);
        if (source != null && target != null) {
            ConnectionLine line = new ConnectionLine(connection, source, target);
            connectionLines.put(id, line);
            connectionLayer.getChildren().add(line);
        }

        return id;
    }

    /**
     * Delete a connection internally (for command pattern).
     */
    public void deleteConnectionInternal(String connectionId) {
        ConnectionLine line = connectionLines.remove(connectionId);
        if (line != null) {
            connectionLayer.getChildren().remove(line);
        }
        workflow = workflow.withRemovedConnection(connectionId);
    }

    /**
     * Get a connection by ID.
     */
    public io.toflowai.common.domain.Connection getConnectionById(String connectionId) {
        return workflow.connections().stream()
                .filter(c -> c.id().equals(connectionId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Restore a deleted connection (for undo).
     */
    public void restoreConnection(io.toflowai.common.domain.Connection connection) {
        workflow = workflow.withAddedConnection(connection);

        NodeView source = nodeViews.get(connection.sourceNodeId());
        NodeView target = nodeViews.get(connection.targetNodeId());
        if (source != null && target != null) {
            ConnectionLine line = new ConnectionLine(connection, source, target);
            connectionLines.put(connection.id(), line);
            connectionLayer.getChildren().add(line);
        }
    }

    /**
     * Get default name for node type.
     */
    private String getDefaultNameForType(String type) {
        return switch (type) {
            case "manualTrigger" -> "Manual Trigger";
            case "scheduleTrigger" -> "Schedule";
            case "webhookTrigger" -> "Webhook";
            case "httpRequest" -> "HTTP Request";
            case "code" -> "Code";
            case "executeCommand" -> "Execute Command";
            case "if" -> "IF";
            case "switch" -> "Switch";
            case "merge" -> "Merge";
            case "loop" -> "Loop";
            case "set" -> "Set";
            case "filter" -> "Filter";
            case "sort" -> "Sort";
            case "llmChat" -> "AI Chat";
            case "textClassifier" -> "Text Classifier";
            case "embedding" -> "Embedding";
            case "rag" -> "RAG Query";
            default -> type;
        };
    }
}
