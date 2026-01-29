package io.toflowai.ui.canvas;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.*;

import io.toflowai.common.domain.Connection;
import io.toflowai.common.domain.Node;
import io.toflowai.common.dto.WorkflowDTO;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;

/**
 * Workflow canvas for visual node editing.
 * Supports drag-drop, zoom, pan, and node connections.
 */
public class WorkflowCanvas extends BorderPane {

    private static final double GRID_SIZE = 20;
    private static final double MIN_ZOOM = 0.25;
    private static final double MAX_ZOOM = 2.0;
    private static final double ZOOM_FACTOR = 0.1;

    private final Pane canvasPane;
    private final Pane nodeLayer;
    private final Pane connectionLayer;
    private final Pane gridLayer;
    private final VBox nodePalette;

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
    private NodeView selectedNode = null;

    public WorkflowCanvas() {
        // Initialize layers
        gridLayer = new Pane();
        connectionLayer = new Pane();
        nodeLayer = new Pane();

        canvasPane = new Pane(gridLayer, connectionLayer, nodeLayer);
        canvasPane.getStyleClass().add("canvas-pane");

        // Wrap in scroll pane
        ScrollPane scrollPane = new ScrollPane(canvasPane);
        scrollPane.setPannable(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.getStyleClass().add("canvas-scroll");

        // Node palette on the left
        nodePalette = createNodePalette();

        // Layout
        setCenter(scrollPane);
        setLeft(nodePalette);

        // Setup interactions
        setupCanvasInteraction();
        setupKeyboardShortcuts();

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
                createPaletteItem("Text Classifier", "textClassifier", MaterialDesignT.TAG_TEXT_OUTLINE)));

        return palette;
    }

    private TitledPane createPaletteSection(String title, javafx.scene.Node... items) {
        VBox content = new VBox(5);
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

        // Paste (if something in clipboard)
        MenuItem pasteItem = new MenuItem("Paste");
        pasteItem.setGraphic(FontIcon.of(MaterialDesignC.CONTENT_PASTE, 14));
        pasteItem.setDisable(true); // TODO: implement clipboard

        contextMenu.getItems().addAll(
                addNodeMenu,
                new SeparatorMenuItem(),
                autoLayoutItem,
                fitViewItem,
                new SeparatorMenuItem(),
                selectAllItem,
                deselectAllItem,
                new SeparatorMenuItem(),
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
            switch (e.getCode()) {
                case DELETE, BACK_SPACE -> deleteSelected();
                case ESCAPE -> deselectAll();
                default -> {
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
    }

    public void saveWorkflow() {
        // TODO: Save workflow to database via service
        System.out.println("Saving workflow: " + workflow.name());
    }

    public void runWorkflow() {
        // TODO: Execute workflow via service
        System.out.println("Running workflow: " + workflow.name());
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
        deselectAll();
        selectedNode = nodeView;
        nodeView.setSelected(true);
    }

    public void deselectAll() {
        if (selectedNode != null) {
            selectedNode.setSelected(false);
            selectedNode = null;
        }
    }

    private void deleteSelected() {
        if (selectedNode != null) {
            String nodeId = selectedNode.getNode().id();

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
            nodeLayer.getChildren().remove(selectedNode);
            nodeViews.remove(nodeId);

            // Remove connections
            connectionLines.entrySet().removeIf(entry -> {
                if (entry.getValue().involvesNode(nodeId)) {
                    connectionLayer.getChildren().remove(entry.getValue());
                    return true;
                }
                return false;
            });

            selectedNode = null;
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
            // Don't call completeConnection here to avoid recursion - do it inline
            String connectionId = UUID.randomUUID().toString();
            Connection connection = Connection.simple(connectionId,
                    connectionSource.getNode().id(),
                    hoveredTarget.getNode().id());
            workflow = workflow.withAddedConnection(connection);
            createConnectionLine(connection);
            System.out.println("Created connection: " + connectionSource.getNode().name() +
                    " -> " + hoveredTarget.getNode().name());
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
        content.setPrefWidth(400);

        // Node name field
        Label nameLabel = new Label("Name:");
        TextField nameField = new TextField(node.name());

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

        // Add node type specific help
        Label helpLabel = new Label(getNodeTypeHelp(node.type()));
        helpLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 11;");
        helpLabel.setWrapText(true);

        content.getChildren().addAll(nameLabel, nameField, paramsLabel, paramsArea, helpLabel);

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
            default -> "Configure this node's behavior.";
        };
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

        // Show execution indicator
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Execute Node");
        info.setHeaderText("Executing: " + node.name());
        info.setContentText("Node execution triggered. Check console for output.");
        info.show();

        // TODO: Actually execute the node via service
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

        if (selectedNode == nodeView) {
            selectedNode = null;
        }

        System.out.println("Deleted node: " + nodeView.getNode().name());
    }

    /**
     * Select all nodes.
     */
    public void selectAll() {
        // TODO: Implement multi-selection
        System.out.println("Select all - not yet implemented");
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

        // Layout trigger nodes first
        for (int i = 0; i < triggerNodes.size(); i++) {
            Node trigger = triggerNodes.get(i);
            NodeView view = nodeViews.get(trigger.id());
            if (view != null) {
                view.setLayoutX(startX);
                view.setLayoutY(startY + i * ySpacing);
                updateNodePosition(trigger.id(), startX, startY + i * ySpacing);
            }
        }

        // TODO: Layout connected nodes in columns
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
}
