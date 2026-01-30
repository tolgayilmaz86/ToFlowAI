package io.toflowai.ui.canvas;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignB;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignE;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;
import org.kordamp.ikonli.materialdesign2.MaterialDesignV;
import org.kordamp.ikonli.materialdesign2.MaterialDesignW;

import io.toflowai.common.domain.Node;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

/**
 * Visual representation of a workflow node on the canvas.
 * Styled similar to n8n with centered icon and label below.
 */
public class NodeView extends StackPane {

    // n8n-style dimensions - square node box
    private static final double NODE_SIZE = 70;
    private static final double HANDLE_RADIUS = 8;
    private static final double CORNER_RADIUS = 10;

    // Handle colors - distinct colors for input/output
    private static final Color INPUT_HANDLE_COLOR = Color.web("#40c057"); // Green for input
    private static final Color OUTPUT_HANDLE_COLOR = Color.web("#4a9eff"); // Blue for output
    private static final Color HANDLE_HOVER_COLOR = Color.web("#ffd43b"); // Yellow on hover
    private static final Color HANDLE_ACTIVE_COLOR = Color.web("#ffd43b"); // Yellow when dragging

    // Node background
    private static final Color NODE_BG = Color.web("#262626");
    private static final Color NODE_BORDER = Color.web("#404040");

    private final Node node;
    private final WorkflowCanvas canvas;

    private final VBox container;
    private final StackPane nodeBox;
    private final Rectangle background;
    private final FontIcon icon;
    private final Label nameLabel;
    private final Label subtitleLabel;
    private final Circle inputHandle;
    private final Circle outputHandle;
    private final javafx.scene.control.ProgressIndicator executionIndicator;
    private final Label executionBadge;
    private final Label errorLabel;
    private final Tooltip errorTooltip;

    private double dragOffsetX, dragOffsetY;
    private boolean selected = false;
    private ExecutionState executionState = ExecutionState.IDLE;
    private String errorMessage = null;

    // Context menu state
    private ContextMenu currentContextMenu = null;

    /** Execution states for visual feedback */
    public enum ExecutionState {
        IDLE, // Normal state
        QUEUED, // Waiting to execute
        RUNNING, // Currently executing
        SUCCESS, // Executed successfully
        ERROR, // Execution failed
        SKIPPED // Skipped (disabled or conditional)
    }

    public NodeView(Node node, WorkflowCanvas canvas) {
        this.node = node;
        this.canvas = canvas;

        getStyleClass().add("node-view");

        // Position on canvas
        setLayoutX(node.position().x());
        setLayoutY(node.position().y());

        // === Create the node box (square with icon) ===
        nodeBox = new StackPane();
        nodeBox.setPrefSize(NODE_SIZE, NODE_SIZE);
        nodeBox.setMinSize(NODE_SIZE, NODE_SIZE);
        nodeBox.setMaxSize(NODE_SIZE, NODE_SIZE);
        nodeBox.getStyleClass().add("node-box");

        // Background rectangle with rounded corners and border
        background = new Rectangle(NODE_SIZE, NODE_SIZE);
        background.setArcWidth(CORNER_RADIUS * 2);
        background.setArcHeight(CORNER_RADIUS * 2);
        background.setFill(NODE_BG);
        background.setStroke(NODE_BORDER);
        background.setStrokeWidth(1.5);

        // Large icon centered in the box
        icon = FontIcon.of(getIconForType(node.type()), 28);
        icon.setIconColor(getIconColorForType(node.type()));

        // === Execution indicator (spinning progress for RUNNING state) ===
        executionIndicator = new javafx.scene.control.ProgressIndicator();
        executionIndicator.setPrefSize(24, 24);
        executionIndicator.setMaxSize(24, 24);
        executionIndicator.setVisible(false);
        executionIndicator.getStyleClass().add("execution-indicator");

        // === Execution badge (for SUCCESS, ERROR, SKIPPED states) ===
        executionBadge = new Label();
        executionBadge.setPrefSize(20, 20);
        executionBadge.setMinSize(20, 20);
        executionBadge.setMaxSize(20, 20);
        executionBadge.setAlignment(Pos.CENTER);
        executionBadge.setVisible(false);
        executionBadge.getStyleClass().add("execution-badge");

        nodeBox.getChildren().addAll(background, icon, executionIndicator, executionBadge);
        StackPane.setAlignment(executionIndicator, Pos.TOP_RIGHT);
        StackPane.setAlignment(executionBadge, Pos.TOP_RIGHT);
        executionIndicator.setTranslateX(8);
        executionIndicator.setTranslateY(-8);
        executionBadge.setTranslateX(8);
        executionBadge.setTranslateY(-8);

        // === Create handles ===
        inputHandle = createHandle(true);
        outputHandle = createHandle(false);

        // Wrap node box with handles
        StackPane nodeWithHandles = new StackPane();
        nodeWithHandles.getChildren().addAll(nodeBox, inputHandle, outputHandle);

        StackPane.setAlignment(inputHandle, Pos.CENTER_LEFT);
        inputHandle.setTranslateX(-HANDLE_RADIUS);

        StackPane.setAlignment(outputHandle, Pos.CENTER_RIGHT);
        outputHandle.setTranslateX(HANDLE_RADIUS);

        // === Labels below the node ===
        nameLabel = new Label(node.name());
        nameLabel.getStyleClass().add("node-name-label");
        nameLabel.setStyle("-fx-text-fill: #e5e5e5; -fx-font-size: 12px; -fx-font-weight: bold;");
        nameLabel.setMaxWidth(120);
        nameLabel.setAlignment(Pos.CENTER);
        nameLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);

        subtitleLabel = new Label(getSubtitleForType(node.type()));
        subtitleLabel.getStyleClass().add("node-subtitle-label");
        subtitleLabel.setStyle("-fx-text-fill: #737373; -fx-font-size: 10px;");
        subtitleLabel.setMaxWidth(120);
        subtitleLabel.setAlignment(Pos.CENTER);

        // === Error label (hidden by default) ===
        errorLabel = new Label();
        errorLabel.getStyleClass().add("node-error-label");
        errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 10px; -fx-padding: 2 6 2 6; " +
                "-fx-background-color: rgba(239, 68, 68, 0.15); -fx-background-radius: 4;");
        errorLabel.setMaxWidth(140);
        errorLabel.setAlignment(Pos.CENTER);
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // Error tooltip for longer messages
        errorTooltip = new Tooltip();
        errorTooltip.setStyle("-fx-background-color: #1a1a1a; -fx-text-fill: #ef4444; -fx-font-size: 11px; " +
                "-fx-background-radius: 4; -fx-padding: 8;");
        errorTooltip.setWrapText(true);
        errorTooltip.setMaxWidth(300);

        // === Main container: node + labels ===
        container = new VBox(4);
        container.setAlignment(Pos.TOP_CENTER);
        container.getChildren().addAll(nodeWithHandles, nameLabel, subtitleLabel, errorLabel);

        getChildren().add(container);

        // Setup interactions
        setupDragBehavior();
        setupClickBehavior();
        setupHandleInteractions();

        // Apply drop shadow
        applyNodeShadow();

        // Apply type-specific border color
        applyNodeTypeStyle();
    }

    private Circle createHandle(boolean isInput) {
        Circle handle = new Circle(HANDLE_RADIUS);
        Color baseColor = isInput ? INPUT_HANDLE_COLOR : OUTPUT_HANDLE_COLOR;
        handle.setFill(baseColor);
        handle.setStroke(Color.WHITE);
        handle.setStrokeWidth(2);
        handle.setCursor(Cursor.CROSSHAIR);
        handle.getStyleClass().add(isInput ? "input-handle" : "output-handle");

        // Tooltip
        String tooltipText = isInput ? "⬅ Input - drop connection here" : "Output ➡ - drag to connect";
        Tooltip tooltip = new Tooltip(tooltipText);
        tooltip.setShowDelay(javafx.util.Duration.millis(200));
        Tooltip.install(handle, tooltip);

        // Add glow effect
        DropShadow glow = new DropShadow();
        glow.setColor(baseColor);
        glow.setRadius(10);
        glow.setSpread(0.3);
        handle.setEffect(glow);

        // Hover effects
        handle.setOnMouseEntered(e -> {
            if (!canvas.isConnectionDragging() || !isInput) {
                handle.setFill(HANDLE_HOVER_COLOR);
                handle.setScaleX(1.3);
                handle.setScaleY(1.3);
            }
        });

        handle.setOnMouseExited(e -> {
            if (!canvas.isConnectionDragging() || !isInput) {
                handle.setFill(baseColor);
                handle.setScaleX(1.0);
                handle.setScaleY(1.0);
            }
        });

        return handle;
    }

    private void setupHandleInteractions() {
        // Output handle: start connection drag
        outputHandle.setOnMousePressed(this::startConnectionDrag);
        outputHandle.setOnMouseDragged(this::updateConnectionDrag);
        outputHandle.setOnMouseReleased(this::endConnectionDrag);

        // Input handle: receive connection
        inputHandle.setOnMousePressed(e -> e.consume());

        inputHandle.setOnMouseReleased(e -> {
            if (canvas.isConnectionDragging()) {
                canvas.completeConnection(this);
            }
            e.consume();
        });

        // Hover effects for input handle during drag
        inputHandle.setOnMouseEntered(e -> {
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

            // Pulse glow effect on handle
            DropShadow handleGlow = new DropShadow();
            handleGlow.setColor(HANDLE_ACTIVE_COLOR);
            handleGlow.setRadius(15);
            handleGlow.setSpread(0.5);
            inputHandle.setEffect(handleGlow);

            // Glow on node
            DropShadow glow = new DropShadow();
            glow.setColor(HANDLE_ACTIVE_COLOR);
            glow.setRadius(12);
            glow.setSpread(0.3);
            nodeBox.setEffect(glow);
        } else {
            inputHandle.setFill(INPUT_HANDLE_COLOR);
            inputHandle.setScaleX(1.0);
            inputHandle.setScaleY(1.0);

            // Restore normal glow
            DropShadow handleGlow = new DropShadow();
            handleGlow.setColor(INPUT_HANDLE_COLOR);
            handleGlow.setRadius(10);
            handleGlow.setSpread(0.3);
            inputHandle.setEffect(handleGlow);

            applyNodeShadow();
        }
    }

    private void setupDragBehavior() {
        nodeBox.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                dragOffsetX = e.getSceneX() - getLayoutX();
                dragOffsetY = e.getSceneY() - getLayoutY();
                toFront();
                canvas.selectNode(this);
                e.consume();
            }
        });

        nodeBox.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY && !canvas.isConnectionDragging()) {
                double newX = e.getSceneX() - dragOffsetX;
                double newY = e.getSceneY() - dragOffsetY;

                // Apply snap-to-grid if enabled
                if (canvas.isSnapToGrid()) {
                    newX = canvas.snapPositionToGrid(newX);
                    newY = canvas.snapPositionToGrid(newY);
                }

                setLayoutX(newX);
                setLayoutY(newY);

                canvas.updateNodePosition(node.id(), newX, newY);
                e.consume();
            }
        });

        nodeBox.setCursor(Cursor.HAND);
    }

    private void setupClickBehavior() {
        nodeBox.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                canvas.selectNode(this, e.isControlDown());
                // Hide context menu when clicking on node
                if (currentContextMenu != null) {
                    currentContextMenu.hide();
                    currentContextMenu = null;
                }
                if (e.getClickCount() == 2) {
                    canvas.openNodeEditor(this);
                }
                e.consume();
            } else if (e.getButton() == MouseButton.SECONDARY) {
                showContextMenu(e.getScreenX(), e.getScreenY());
                e.consume();
            }
        });
    }

    private void showContextMenu(double screenX, double screenY) {
        // Hide any existing context menu
        if (currentContextMenu != null) {
            currentContextMenu.hide();
        }

        ContextMenu contextMenu = new ContextMenu();
        currentContextMenu = contextMenu;

        MenuItem editItem = new MenuItem("Open Editor");
        editItem.setGraphic(FontIcon.of(MaterialDesignP.PENCIL, 14));
        editItem.setOnAction(e -> canvas.openNodeEditor(this));

        MenuItem executeItem = new MenuItem("Execute Node");
        executeItem.setGraphic(FontIcon.of(MaterialDesignP.PLAY, 14));
        executeItem.setOnAction(e -> canvas.executeNode(this));

        MenuItem duplicateItem = new MenuItem("Duplicate");
        duplicateItem.setGraphic(FontIcon.of(MaterialDesignC.CONTENT_COPY, 14));
        duplicateItem.setOnAction(e -> canvas.duplicateNode(this));

        MenuItem toggleItem = new MenuItem(node.disabled() ? "Enable" : "Disable");
        toggleItem.setGraphic(FontIcon.of(node.disabled() ? MaterialDesignE.EYE : MaterialDesignE.EYE_OFF, 14));
        toggleItem.setOnAction(e -> canvas.toggleNodeEnabled(this));

        MenuItem renameItem = new MenuItem("Rename");
        renameItem.setGraphic(FontIcon.of(MaterialDesignR.RENAME_BOX, 14));
        renameItem.setOnAction(e -> canvas.renameNode(this));

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

    private void applyNodeShadow() {
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.6));
        shadow.setRadius(6);
        shadow.setOffsetY(2);
        nodeBox.setEffect(shadow);
    }

    private void applyNodeTypeStyle() {
        // Apply colored border based on node type
        Color borderColor = getBorderColorForType(node.type());
        background.setStroke(borderColor);

        String styleClass = switch (node.type()) {
            case "manualTrigger", "scheduleTrigger", "webhookTrigger" -> "node-trigger";
            case "httpRequest", "code", "executeCommand" -> "node-action";
            case "if", "switch", "merge", "loop" -> "node-flow";
            case "set", "filter", "sort" -> "node-data";
            case "llmChat", "textClassifier", "embedding", "rag" -> "node-ai";
            default -> "node-default";
        };
        getStyleClass().add(styleClass);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        if (selected) {
            background.setStroke(Color.web("#60a5fa"));
            background.setStrokeWidth(2.5);

            DropShadow glow = new DropShadow();
            glow.setColor(Color.web("#60a5fa"));
            glow.setRadius(10);
            glow.setSpread(0.2);
            nodeBox.setEffect(glow);
        } else {
            background.setStroke(getBorderColorForType(node.type()));
            background.setStrokeWidth(1.5);
            applyNodeShadow();
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

    // Connection point coordinates - center of the handles
    public double getInputX() {
        // Input handle is at left edge minus the handle radius
        return getLayoutX() - HANDLE_RADIUS;
    }

    public double getInputY() {
        return getLayoutY() + NODE_SIZE / 2;
    }

    public double getOutputX() {
        // Output handle is at right edge plus the handle radius
        return getLayoutX() + NODE_SIZE + HANDLE_RADIUS;
    }

    public double getOutputY() {
        return getLayoutY() + NODE_SIZE / 2;
    }

    /**
     * Set the execution state and update visual indicators.
     */
    public void setExecutionState(ExecutionState state) {
        this.executionState = state;

        // Reset visuals
        executionIndicator.setVisible(false);
        executionBadge.setVisible(false);
        nodeBox.getStyleClass().removeAll("node-running", "node-queued", "node-success", "node-error", "node-skipped");

        switch (state) {
            case IDLE -> {
                // Default state, no indicator
            }
            case QUEUED -> {
                nodeBox.getStyleClass().add("node-queued");
                executionBadge.setVisible(true);
                executionBadge.setText("⏱");
                executionBadge.setStyle(
                        "-fx-background-color: #6b7280; -fx-background-radius: 10; -fx-text-fill: white; -fx-font-size: 11px;");
            }
            case RUNNING -> {
                nodeBox.getStyleClass().add("node-running");
                executionIndicator.setVisible(true);

                // Add pulsing border animation
                DropShadow runningGlow = new DropShadow();
                runningGlow.setColor(Color.web("#3b82f6"));
                runningGlow.setRadius(15);
                runningGlow.setSpread(0.4);
                nodeBox.setEffect(runningGlow);
            }
            case SUCCESS -> {
                nodeBox.getStyleClass().add("node-success");
                executionBadge.setVisible(true);
                executionBadge.setText("✓");
                executionBadge.setStyle(
                        "-fx-background-color: #10b981; -fx-background-radius: 10; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");

                // Green glow briefly
                DropShadow successGlow = new DropShadow();
                successGlow.setColor(Color.web("#10b981"));
                successGlow.setRadius(10);
                successGlow.setSpread(0.3);
                nodeBox.setEffect(successGlow);
            }
            case ERROR -> {
                nodeBox.getStyleClass().add("node-error");
                executionBadge.setVisible(true);
                executionBadge.setText("✕");
                executionBadge.setStyle(
                        "-fx-background-color: #ef4444; -fx-background-radius: 10; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");

                // Red glow
                DropShadow errorGlow = new DropShadow();
                errorGlow.setColor(Color.web("#ef4444"));
                errorGlow.setRadius(12);
                errorGlow.setSpread(0.4);
                nodeBox.setEffect(errorGlow);
            }
            case SKIPPED -> {
                nodeBox.getStyleClass().add("node-skipped");
                executionBadge.setVisible(true);
                executionBadge.setText("⊘");
                executionBadge.setStyle(
                        "-fx-background-color: #737373; -fx-background-radius: 10; -fx-text-fill: white; -fx-font-size: 11px;");
            }
        }
    }

    /**
     * Get the current execution state.
     */
    public ExecutionState getExecutionState() {
        return executionState;
    }

    /**
     * Reset execution state to IDLE and restore normal shadow.
     */
    public void resetExecutionState() {
        setExecutionState(ExecutionState.IDLE);
        clearError();
        applyNodeShadow();
    }

    /**
     * Set an error message to display on the node.
     */
    public void setError(String message) {
        this.errorMessage = message;
        if (message != null && !message.isEmpty()) {
            // Show truncated message in label
            String displayText = message.length() > 30
                    ? message.substring(0, 27) + "..."
                    : message;
            errorLabel.setText("⚠ " + displayText);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);

            // Full message in tooltip
            errorTooltip.setText(message);
            Tooltip.install(errorLabel, errorTooltip);

            // Set error execution state
            setExecutionState(ExecutionState.ERROR);
        } else {
            clearError();
        }
    }

    /**
     * Get the current error message.
     */
    public String getError() {
        return errorMessage;
    }

    /**
     * Clear the error message from the node.
     */
    public void clearError() {
        this.errorMessage = null;
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        Tooltip.uninstall(errorLabel, errorTooltip);
    }

    /**
     * Check if the node has an error.
     */
    public boolean hasError() {
        return errorMessage != null && !errorMessage.isEmpty();
    }

    public boolean canBeConnectionSource() {
        return true;
    }

    public boolean canBeConnectionTarget() {
        return !node.type().endsWith("Trigger");
    }

    public boolean canConnectTo(NodeView target) {
        if (target == this)
            return false;
        if (!target.canBeConnectionTarget())
            return false;
        return !canvas.hasConnection(this.node.id(), target.node.id());
    }

    private Color getBorderColorForType(String type) {
        return switch (type) {
            case "manualTrigger", "scheduleTrigger", "webhookTrigger" -> Color.web("#f59e0b"); // Amber
            case "httpRequest", "code", "executeCommand" -> Color.web("#525252"); // Gray
            case "if", "switch", "merge", "loop" -> Color.web("#10b981"); // Emerald
            case "set", "filter", "sort" -> Color.web("#8b5cf6"); // Violet
            case "llmChat", "textClassifier", "embedding", "rag" -> Color.web("#ec4899"); // Pink
            case "subworkflow", "parallel", "tryCatch", "retry", "rate_limit" -> Color.web("#f97316"); // Orange
            default -> Color.web("#525252"); // Gray
        };
    }

    private Color getIconColorForType(String type) {
        return switch (type) {
            case "manualTrigger", "scheduleTrigger", "webhookTrigger" -> Color.web("#fbbf24"); // Amber bright
            case "httpRequest", "code", "executeCommand" -> Color.web("#60a5fa"); // Blue
            case "if", "switch", "merge", "loop" -> Color.web("#34d399"); // Emerald bright
            case "set", "filter", "sort" -> Color.web("#a78bfa"); // Violet bright
            case "llmChat", "textClassifier", "embedding", "rag" -> Color.web("#f472b6"); // Pink bright
            case "subworkflow", "parallel", "tryCatch", "retry", "rate_limit" -> Color.web("#fb923c"); // Orange bright
            default -> Color.web("#a1a1aa"); // Gray
        };
    }

    private org.kordamp.ikonli.Ikon getIconForType(String type) {
        return switch (type) {
            case "manualTrigger" -> MaterialDesignP.PLAY_CIRCLE;
            case "scheduleTrigger" -> MaterialDesignC.CLOCK_OUTLINE;
            case "webhookTrigger" -> MaterialDesignW.WEBHOOK;
            case "httpRequest" -> MaterialDesignW.WEB;
            case "code" -> MaterialDesignC.CODE_BRACES;
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
            case "embedding" -> MaterialDesignV.VECTOR_BEZIER;
            case "rag" -> MaterialDesignB.BOOK_SEARCH;
            case "subworkflow" -> MaterialDesignS.SITEMAP;
            case "parallel" -> MaterialDesignF.FORMAT_ALIGN_JUSTIFY;
            case "tryCatch" -> MaterialDesignS.SHIELD_CHECK;
            case "retry" -> MaterialDesignR.REFRESH;
            case "rate_limit" -> MaterialDesignS.SPEEDOMETER;
            default -> MaterialDesignC.CUBE_OUTLINE;
        };
    }

    private String getSubtitleForType(String type) {
        return switch (type) {
            case "manualTrigger" -> "trigger";
            case "scheduleTrigger" -> "schedule";
            case "webhookTrigger" -> "webhook";
            case "httpRequest" -> "HTTP";
            case "code" -> "code";
            case "executeCommand" -> "command";
            case "if" -> "condition";
            case "switch" -> "router";
            case "merge" -> "merge";
            case "loop" -> "loop";
            case "set" -> "transform";
            case "filter" -> "filter";
            case "sort" -> "sort";
            case "llmChat" -> "AI chat";
            case "textClassifier" -> "classifier";
            case "embedding" -> "vectors";
            case "rag" -> "retrieval";
            case "subworkflow" -> "nested";
            case "parallel" -> "parallel";
            case "tryCatch" -> "error handling";
            case "retry" -> "retry";
            case "rate_limit" -> "throttle";
            default -> type;
        };
    }
}
