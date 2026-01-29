package io.toflowai.ui.canvas;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.*;

import io.toflowai.common.domain.Node;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * Visual representation of a workflow node on the canvas.
 * Supports connection handles for input (left) and output (right).
 */
public class NodeView extends StackPane {

    private static final double NODE_WIDTH = 180;
    private static final double NODE_MIN_HEIGHT = 80;
    private static final double HANDLE_RADIUS = 8;

    // Different colors for input vs output handles
    private static final Color INPUT_HANDLE_COLOR = Color.web("#40c057"); // Green for input
    private static final Color OUTPUT_HANDLE_COLOR = Color.web("#4a9eff"); // Blue for output
    private static final Color HANDLE_HOVER_COLOR = Color.web("#ffd43b"); // Yellow on hover
    private static final Color HANDLE_ACTIVE_COLOR = Color.web("#ffd43b"); // Yellow when dragging

    private final Node node;
    private final WorkflowCanvas canvas;

    private final VBox contentBox;
    private final HBox header;
    private final VBox body;
    private final Circle inputHandle;
    private final Circle outputHandle;

    private double dragOffsetX, dragOffsetY;
    private boolean selected = false;

    public NodeView(Node node, WorkflowCanvas canvas) {
        this.node = node;
        this.canvas = canvas;

        // Setup node appearance using StackPane for handle positioning
        setPrefWidth(NODE_WIDTH);
        setMinHeight(NODE_MIN_HEIGHT);
        getStyleClass().add("node-view");

        // Position
        setLayoutX(node.position().x());
        setLayoutY(node.position().y());

        // Content box (the actual node visual)
        contentBox = new VBox();
        contentBox.setPrefWidth(NODE_WIDTH);
        contentBox.setMinHeight(NODE_MIN_HEIGHT);
        contentBox.getStyleClass().add("node-content");

        // Header with icon and title
        header = createHeader();

        // Body with node info
        body = createBody();

        contentBox.getChildren().addAll(header, body);

        // Create handles
        inputHandle = createHandle(true);
        outputHandle = createHandle(false);

        // Add all to StackPane
        getChildren().addAll(contentBox, inputHandle, outputHandle);

        // Position handles on the edges
        positionHandles();

        // Setup interactions
        setupDragBehavior();
        setupClickBehavior();
        setupHandleInteractions();

        // Apply node type styling
        applyNodeTypeStyle();
    }

    private void positionHandles() {
        // Input handle on left edge, centered vertically
        StackPane.setAlignment(inputHandle, Pos.CENTER_LEFT);
        inputHandle.setTranslateX(-HANDLE_RADIUS);

        // Output handle on right edge, centered vertically
        StackPane.setAlignment(outputHandle, Pos.CENTER_RIGHT);
        outputHandle.setTranslateX(HANDLE_RADIUS);
    }

    private HBox createHeader() {
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(8, 10, 8, 10));
        header.getStyleClass().add("node-header");

        // Icon based on node type
        FontIcon icon = FontIcon.of(getIconForType(node.type()), 16);
        icon.getStyleClass().add("node-icon");

        // Title
        Label title = new Label(node.name());
        title.getStyleClass().add("node-title");
        HBox.setHgrow(title, Priority.ALWAYS);

        header.getChildren().addAll(icon, title);
        return header;
    }

    private VBox createBody() {
        VBox body = new VBox(5);
        body.setPadding(new Insets(10));
        body.getStyleClass().add("node-body");

        // Add node type label
        Label typeLabel = new Label(getDisplayNameForType(node.type()));
        typeLabel.getStyleClass().add("node-type-label");
        body.getChildren().add(typeLabel);

        return body;
    }

    private Circle createHandle(boolean isInput) {
        Circle handle = new Circle(HANDLE_RADIUS);
        handle.getStyleClass().add(isInput ? "input-handle" : "output-handle");

        // Different colors for input vs output
        Color baseColor = isInput ? INPUT_HANDLE_COLOR : OUTPUT_HANDLE_COLOR;
        handle.setFill(baseColor);
        handle.setStroke(Color.WHITE);
        handle.setStrokeWidth(2);
        handle.setCursor(Cursor.CROSSHAIR);

        // Store base color for later
        handle.setUserData(baseColor);

        // Add tooltip
        String tooltipText = isInput ? "⬅ Input - drop connection here" : "Output ➡ - drag to connect";
        Tooltip tooltip = new Tooltip(tooltipText);
        tooltip.setShowDelay(javafx.util.Duration.millis(200));
        Tooltip.install(handle, tooltip);

        // Hover effects
        handle.setOnMouseEntered(e -> {
            handle.setFill(HANDLE_HOVER_COLOR);
            handle.setScaleX(1.3);
            handle.setScaleY(1.3);
        });

        handle.setOnMouseExited(e -> {
            if (!canvas.isConnectionDragging() || !isInput) {
                handle.setFill(baseColor);
            }
            handle.setScaleX(1.0);
            handle.setScaleY(1.0);
        });

        // Add glow effect
        DropShadow glow = new DropShadow();
        glow.setColor(baseColor);
        glow.setRadius(10);
        glow.setSpread(0.3);
        handle.setEffect(glow);

        return handle;
    }

    private void setupHandleInteractions() {
        // Output handle: start connection drag
        outputHandle.setOnMousePressed(this::startConnectionDrag);
        outputHandle.setOnMouseDragged(this::updateConnectionDrag);
        outputHandle.setOnMouseReleased(this::endConnectionDrag);

        // Input handle: receive connection
        inputHandle.setOnMousePressed(e -> {
            e.consume(); // Prevent node drag
        });

        inputHandle.setOnMouseReleased(e -> {
            System.out.println("Input handle released, dragging: " + canvas.isConnectionDragging());
            if (canvas.isConnectionDragging()) {
                canvas.completeConnection(this);
            }
            e.consume();
        });

        // Hover effects for input handle
        inputHandle.setOnMouseEntered(e -> {
            System.out
                    .println("Entered input handle of " + node.name() + ", dragging: " + canvas.isConnectionDragging());
            if (canvas.isConnectionDragging()) {
                highlightAsTarget(true);
                canvas.setHoveredTarget(this);
            } else {
                inputHandle.setFill(HANDLE_HOVER_COLOR);
                inputHandle.setScaleX(1.3);
                inputHandle.setScaleY(1.3);
            }
        });

        inputHandle.setOnMouseExited(e -> {
            if (canvas.isConnectionDragging()) {
                highlightAsTarget(false);
                canvas.setHoveredTarget(null);
            } else {
                inputHandle.setFill(INPUT_HANDLE_COLOR);
                inputHandle.setScaleX(1.0);
                inputHandle.setScaleY(1.0);
            }
        });
    }

    private void startConnectionDrag(javafx.scene.input.MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY) {
            canvas.startConnectionDrag(this);
            outputHandle.setFill(HANDLE_ACTIVE_COLOR);
            e.consume();
        }
    }

    private void updateConnectionDrag(javafx.scene.input.MouseEvent e) {
        if (canvas.isConnectionDragging()) {
            // Convert to canvas coordinates
            javafx.geometry.Point2D scenePoint = outputHandle.localToScene(e.getX(), e.getY());
            canvas.updateConnectionDrag(scenePoint.getX(), scenePoint.getY());
            e.consume();
        }
    }

    private void endConnectionDrag(javafx.scene.input.MouseEvent e) {
        outputHandle.setFill(OUTPUT_HANDLE_COLOR);
        canvas.endConnectionDrag();
        e.consume();
    }

    public void highlightAsTarget(boolean highlight) {
        if (highlight) {
            inputHandle.setFill(HANDLE_ACTIVE_COLOR);
            inputHandle.setScaleX(1.5);
            inputHandle.setScaleY(1.5);

            // Pulse effect
            DropShadow glow = new DropShadow();
            glow.setColor(HANDLE_ACTIVE_COLOR);
            glow.setRadius(15);
            glow.setSpread(0.5);
            inputHandle.setEffect(glow);
        } else {
            inputHandle.setFill(INPUT_HANDLE_COLOR);
            inputHandle.setScaleX(1.0);
            inputHandle.setScaleY(1.0);

            // Restore normal glow
            DropShadow glow = new DropShadow();
            glow.setColor(INPUT_HANDLE_COLOR);
            glow.setRadius(10);
            glow.setSpread(0.3);
            inputHandle.setEffect(glow);
        }
    }

    private void setupDragBehavior() {
        // Drag the entire node (but not when clicking handles)
        contentBox.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                dragOffsetX = e.getX();
                dragOffsetY = e.getY();
                toFront();
                canvas.selectNode(this);
                e.consume();
            }
        });

        contentBox.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY && !canvas.isConnectionDragging()) {
                double newX = getLayoutX() + e.getX() - dragOffsetX;
                double newY = getLayoutY() + e.getY() - dragOffsetY;

                setLayoutX(newX);
                setLayoutY(newY);

                canvas.updateNodePosition(node.id(), newX, newY);
                e.consume();
            }
        });

        contentBox.setCursor(Cursor.HAND);
    }

    private void setupClickBehavior() {
        contentBox.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                canvas.selectNode(this);

                // Double-click to open node editor
                if (e.getClickCount() == 2) {
                    canvas.openNodeEditor(this);
                }
                e.consume();
            } else if (e.getButton() == MouseButton.SECONDARY) {
                // Right-click context menu
                showContextMenu(e.getScreenX(), e.getScreenY());
                e.consume();
            }
        });
    }

    /**
     * Show context menu for this node.
     */
    private void showContextMenu(double screenX, double screenY) {
        ContextMenu contextMenu = new ContextMenu();

        // Open/Edit
        MenuItem editItem = new MenuItem("Open Editor");
        editItem.setGraphic(FontIcon.of(MaterialDesignP.PENCIL, 14));
        editItem.setOnAction(e -> canvas.openNodeEditor(this));

        // Execute this node
        MenuItem executeItem = new MenuItem("Execute Node");
        executeItem.setGraphic(FontIcon.of(MaterialDesignP.PLAY, 14));
        executeItem.setOnAction(e -> canvas.executeNode(this));

        // Duplicate
        MenuItem duplicateItem = new MenuItem("Duplicate");
        duplicateItem.setGraphic(FontIcon.of(MaterialDesignC.CONTENT_COPY, 14));
        duplicateItem.setOnAction(e -> canvas.duplicateNode(this));

        // Enable/Disable
        MenuItem toggleItem = new MenuItem(node.disabled() ? "Enable" : "Disable");
        toggleItem.setGraphic(FontIcon.of(node.disabled() ? MaterialDesignE.EYE : MaterialDesignE.EYE_OFF, 14));
        toggleItem.setOnAction(e -> canvas.toggleNodeEnabled(this));

        // Rename
        MenuItem renameItem = new MenuItem("Rename");
        renameItem.setGraphic(FontIcon.of(MaterialDesignR.RENAME_BOX, 14));
        renameItem.setOnAction(e -> canvas.renameNode(this));

        // Delete
        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setGraphic(FontIcon.of(MaterialDesignD.DELETE, 14));
        deleteItem.setStyle("-fx-text-fill: #fa5252;");
        deleteItem.setOnAction(e -> canvas.deleteNode(this));

        contextMenu.getItems().addAll(
                editItem,
                executeItem,
                new SeparatorMenuItem(),
                duplicateItem,
                toggleItem,
                renameItem,
                new SeparatorMenuItem(),
                deleteItem);

        contextMenu.show(this, screenX, screenY);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        if (selected) {
            getStyleClass().add("selected");
        } else {
            getStyleClass().remove("selected");
        }
    }

    public boolean isSelected() {
        return selected;
    }

    public Node getNode() {
        return node;
    }

    public Circle getInputHandle() {
        return inputHandle;
    }

    public Circle getOutputHandle() {
        return outputHandle;
    }

    /**
     * Get the scene X coordinate of the input handle center.
     */
    public double getInputX() {
        return getLayoutX();
    }

    /**
     * Get the scene Y coordinate of the input handle center.
     */
    public double getInputY() {
        return getLayoutY() + getBoundsInLocal().getHeight() / 2;
    }

    /**
     * Get the scene X coordinate of the output handle center.
     */
    public double getOutputX() {
        return getLayoutX() + NODE_WIDTH;
    }

    /**
     * Get the scene Y coordinate of the output handle center.
     */
    public double getOutputY() {
        return getLayoutY() + getBoundsInLocal().getHeight() / 2;
    }

    /**
     * Check if this node type can be a source for connections.
     * Trigger nodes can only be sources, not targets.
     */
    public boolean canBeConnectionSource() {
        return true; // All nodes can output
    }

    /**
     * Check if this node type can be a target for connections.
     * Trigger nodes typically cannot receive inputs.
     */
    public boolean canBeConnectionTarget() {
        // Trigger nodes cannot receive input connections
        return !node.type().endsWith("Trigger");
    }

    /**
     * Check if a connection from this node to the target is valid.
     */
    public boolean canConnectTo(NodeView target) {
        if (target == this)
            return false; // No self-connections
        if (!target.canBeConnectionTarget())
            return false;

        // Check if connection already exists
        return !canvas.hasConnection(this.node.id(), target.node.id());
    }

    private void applyNodeTypeStyle() {
        String styleClass = switch (node.type()) {
            case "manualTrigger", "scheduleTrigger", "webhookTrigger" -> "node-trigger";
            case "httpRequest", "code", "executeCommand" -> "node-action";
            case "if", "switch", "merge", "loop" -> "node-flow";
            case "set", "filter", "sort" -> "node-data";
            case "llmChat", "textClassifier" -> "node-ai";
            default -> "node-default";
        };
        getStyleClass().add(styleClass);

        // Apply colored border based on node type
        Color borderColor = switch (node.type()) {
            case "manualTrigger", "scheduleTrigger", "webhookTrigger" -> Color.web("#40c057"); // Green
            case "httpRequest", "code", "executeCommand" -> Color.web("#4a9eff"); // Blue
            case "if", "switch", "merge", "loop" -> Color.web("#fcc419"); // Yellow
            case "set", "filter", "sort" -> Color.web("#fa5252"); // Red
            case "llmChat", "textClassifier" -> Color.web("#9775fa"); // Purple
            default -> Color.web("#8b949e"); // Gray
        };

        // Create rounded border
        contentBox.setBorder(new Border(new BorderStroke(
                borderColor,
                BorderStrokeStyle.SOLID,
                new CornerRadii(12),
                new BorderWidths(2))));

        // Add background with matching radius
        contentBox.setBackground(new Background(new BackgroundFill(
                Color.web("#3b4252"),
                new CornerRadii(12),
                Insets.EMPTY)));

        // Add drop shadow effect
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.4));
        shadow.setRadius(10);
        shadow.setOffsetY(3);
        contentBox.setEffect(shadow);
    }

    private org.kordamp.ikonli.Ikon getIconForType(String type) {
        return switch (type) {
            case "manualTrigger" -> MaterialDesignP.PLAY_CIRCLE;
            case "scheduleTrigger" -> MaterialDesignC.CLOCK_OUTLINE;
            case "webhookTrigger" -> MaterialDesignW.WEBHOOK;
            case "httpRequest" -> MaterialDesignW.WEB;
            case "code" -> MaterialDesignC.CODE_TAGS;
            case "executeCommand" -> MaterialDesignC.CONSOLE;
            case "if" -> MaterialDesignC.CALL_SPLIT;
            case "switch" -> MaterialDesignS.SWAP_HORIZONTAL;
            case "merge" -> MaterialDesignC.CALL_MERGE;
            case "loop" -> MaterialDesignR.REPEAT;
            case "set" -> MaterialDesignP.PENCIL;
            case "filter" -> MaterialDesignF.FILTER_OUTLINE;
            case "sort" -> MaterialDesignS.SORT_ASCENDING;
            case "llmChat" -> MaterialDesignR.ROBOT;
            case "textClassifier" -> MaterialDesignT.TAG_TEXT_OUTLINE;
            default -> MaterialDesignC.CUBE_OUTLINE;
        };
    }

    private String getDisplayNameForType(String type) {
        return switch (type) {
            case "manualTrigger" -> "Manual Trigger";
            case "scheduleTrigger" -> "Schedule Trigger";
            case "webhookTrigger" -> "Webhook Trigger";
            case "httpRequest" -> "HTTP Request";
            case "code" -> "Code";
            case "executeCommand" -> "Execute Command";
            case "if" -> "If";
            case "switch" -> "Switch";
            case "merge" -> "Merge";
            case "loop" -> "Loop";
            case "set" -> "Set";
            case "filter" -> "Filter";
            case "sort" -> "Sort";
            case "llmChat" -> "LLM Chat";
            case "textClassifier" -> "Text Classifier";
            default -> type;
        };
    }
}
