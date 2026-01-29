package io.toflowai.ui.canvas;

import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Mini-map component for canvas navigation overview.
 * Shows a scaled-down view of all nodes with a viewport indicator.
 */
public class CanvasMinimap extends StackPane {

    private static final double DEFAULT_WIDTH = 180;
    private static final double DEFAULT_HEIGHT = 120;
    private static final double NODE_DOT_SIZE = 8;
    private static final double PADDING = 8;

    private final WorkflowCanvas canvas;
    private final Pane mapPane;
    private final Rectangle viewportRect;
    private final Rectangle background;
    private final Map<String, Rectangle> nodeDots = new HashMap<>();

    // Canvas bounds tracking
    private final DoubleProperty canvasMinX = new SimpleDoubleProperty(0);
    private final DoubleProperty canvasMinY = new SimpleDoubleProperty(0);
    private final DoubleProperty canvasMaxX = new SimpleDoubleProperty(1000);
    private final DoubleProperty canvasMaxY = new SimpleDoubleProperty(800);

    private boolean isDragging = false;
    private double dragStartX, dragStartY;

    public CanvasMinimap(WorkflowCanvas canvas) {
        this.canvas = canvas;

        setPrefSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setMinSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setMaxSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        getStyleClass().add("canvas-minimap");

        // Background
        background = new Rectangle(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        background.setFill(Color.web("#1a1a1a"));
        background.setStroke(Color.web("#404040"));
        background.setStrokeWidth(1);
        background.setArcWidth(8);
        background.setArcHeight(8);

        // Map pane for node dots
        mapPane = new Pane();
        mapPane.setPrefSize(DEFAULT_WIDTH - PADDING * 2, DEFAULT_HEIGHT - PADDING * 2);
        mapPane.setMaxSize(DEFAULT_WIDTH - PADDING * 2, DEFAULT_HEIGHT - PADDING * 2);

        // Viewport indicator rectangle
        viewportRect = new Rectangle();
        viewportRect.setFill(Color.web("#4a9eff", 0.2));
        viewportRect.setStroke(Color.web("#4a9eff"));
        viewportRect.setStrokeWidth(1.5);
        viewportRect.setCursor(Cursor.MOVE);

        mapPane.getChildren().add(viewportRect);

        getChildren().addAll(background, mapPane);
        StackPane.setMargin(mapPane, new Insets(PADDING));

        setupInteraction();
    }

    private void setupInteraction() {
        // Click to navigate
        mapPane.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                isDragging = true;
                navigateTo(e.getX(), e.getY());
                e.consume();
            }
        });

        mapPane.setOnMouseDragged(e -> {
            if (isDragging) {
                navigateTo(e.getX(), e.getY());
                e.consume();
            }
        });

        mapPane.setOnMouseReleased(e -> {
            isDragging = false;
            e.consume();
        });

        // Viewport dragging
        viewportRect.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                isDragging = true;
                dragStartX = e.getX();
                dragStartY = e.getY();
                e.consume();
            }
        });

        viewportRect.setOnMouseDragged(e -> {
            if (isDragging) {
                double dx = e.getX() - dragStartX;
                double dy = e.getY() - dragStartY;

                double newX = viewportRect.getLayoutX() + dx;
                double newY = viewportRect.getLayoutY() + dy;

                // Clamp to map bounds
                newX = Math.max(0, Math.min(newX, mapPane.getWidth() - viewportRect.getWidth()));
                newY = Math.max(0, Math.min(newY, mapPane.getHeight() - viewportRect.getHeight()));

                viewportRect.setLayoutX(newX);
                viewportRect.setLayoutY(newY);

                // Update canvas position
                updateCanvasFromViewport();
                e.consume();
            }
        });

        viewportRect.setOnMouseReleased(e -> {
            isDragging = false;
            e.consume();
        });
    }

    private void navigateTo(double mapX, double mapY) {
        // Convert map coordinates to canvas coordinates
        double canvasWidth = canvasMaxX.get() - canvasMinX.get();
        double canvasHeight = canvasMaxY.get() - canvasMinY.get();
        double mapWidth = mapPane.getWidth();
        double mapHeight = mapPane.getHeight();

        // Scale factor
        double scaleX = canvasWidth / mapWidth;
        double scaleY = canvasHeight / mapHeight;

        // Target canvas position (centered on click)
        double targetX = canvasMinX.get() + (mapX * scaleX);
        double targetY = canvasMinY.get() + (mapY * scaleY);

        // Navigate canvas to center on this position
        canvas.centerOn(targetX, targetY);
    }

    private void updateCanvasFromViewport() {
        double canvasWidth = canvasMaxX.get() - canvasMinX.get();
        double canvasHeight = canvasMaxY.get() - canvasMinY.get();
        double mapWidth = mapPane.getWidth();
        double mapHeight = mapPane.getHeight();

        double scaleX = canvasWidth / mapWidth;
        double scaleY = canvasHeight / mapHeight;

        double targetX = canvasMinX.get() + (viewportRect.getLayoutX() + viewportRect.getWidth() / 2) * scaleX;
        double targetY = canvasMinY.get() + (viewportRect.getLayoutY() + viewportRect.getHeight() / 2) * scaleY;

        canvas.centerOn(targetX, targetY);
    }

    /**
     * Update the minimap to reflect current canvas state.
     * Call this when nodes are added/removed/moved.
     */
    public void update() {
        recalculateBounds();
        updateNodeDots();
        updateViewport();
    }

    private void recalculateBounds() {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;

        for (NodeView node : canvas.getNodeViews()) {
            double x = node.getLayoutX();
            double y = node.getLayoutY();
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x + 70); // NODE_SIZE
            maxY = Math.max(maxY, y + 70);
        }

        // Add padding
        if (minX != Double.MAX_VALUE) {
            canvasMinX.set(minX - 100);
            canvasMinY.set(minY - 100);
            canvasMaxX.set(maxX + 100);
            canvasMaxY.set(maxY + 100);
        } else {
            // No nodes, default bounds
            canvasMinX.set(0);
            canvasMinY.set(0);
            canvasMaxX.set(1000);
            canvasMaxY.set(800);
        }
    }

    private void updateNodeDots() {
        // Remove old dots
        nodeDots.values().forEach(dot -> mapPane.getChildren().remove(dot));
        nodeDots.clear();

        double canvasWidth = canvasMaxX.get() - canvasMinX.get();
        double canvasHeight = canvasMaxY.get() - canvasMinY.get();
        double mapWidth = mapPane.getWidth() - PADDING * 2;
        double mapHeight = mapPane.getHeight() - PADDING * 2;

        if (canvasWidth <= 0 || canvasHeight <= 0)
            return;

        double scaleX = mapWidth / canvasWidth;
        double scaleY = mapHeight / canvasHeight;

        for (NodeView node : canvas.getNodeViews()) {
            Rectangle dot = new Rectangle(NODE_DOT_SIZE, NODE_DOT_SIZE);
            dot.setArcWidth(3);
            dot.setArcHeight(3);

            // Color based on node state/type
            Color dotColor = getNodeDotColor(node);
            dot.setFill(dotColor);

            // Position
            double x = (node.getLayoutX() - canvasMinX.get()) * scaleX + PADDING;
            double y = (node.getLayoutY() - canvasMinY.get()) * scaleY + PADDING;
            dot.setLayoutX(x);
            dot.setLayoutY(y);

            nodeDots.put(node.getNode().id(), dot);
            mapPane.getChildren().add(dot);
        }

        // Keep viewport on top
        viewportRect.toFront();
    }

    private Color getNodeDotColor(NodeView node) {
        // Check execution state first
        return switch (node.getExecutionState()) {
            case RUNNING -> Color.web("#3b82f6");
            case SUCCESS -> Color.web("#10b981");
            case ERROR -> Color.web("#ef4444");
            case QUEUED -> Color.web("#6b7280");
            case SKIPPED -> Color.web("#737373");
            default -> {
                // Use type color
                String type = node.getNode().type();
                if (type.contains("Trigger")) {
                    yield Color.web("#f59e0b");
                } else if (type.contains("llm") || type.contains("ai") || type.contains("embedding")
                        || type.contains("rag")) {
                    yield Color.web("#ec4899");
                } else if (type.equals("if") || type.equals("switch") || type.equals("merge") || type.equals("loop")) {
                    yield Color.web("#10b981");
                } else {
                    yield Color.web("#4a9eff");
                }
            }
        };
    }

    /**
     * Update viewport rectangle to match canvas scroll position.
     */
    public void updateViewport() {
        Bounds viewportBounds = canvas.getViewportBounds();
        if (viewportBounds == null)
            return;

        double canvasWidth = canvasMaxX.get() - canvasMinX.get();
        double canvasHeight = canvasMaxY.get() - canvasMinY.get();
        double mapWidth = mapPane.getWidth() - PADDING * 2;
        double mapHeight = mapPane.getHeight() - PADDING * 2;

        if (canvasWidth <= 0 || canvasHeight <= 0)
            return;

        double scaleX = mapWidth / canvasWidth;
        double scaleY = mapHeight / canvasHeight;

        // Calculate viewport size and position on minimap
        double vpWidth = Math.min(mapWidth, viewportBounds.getWidth() * scaleX);
        double vpHeight = Math.min(mapHeight, viewportBounds.getHeight() * scaleY);

        // Get current scroll position
        double scrollX = canvas.getScrollX();
        double scrollY = canvas.getScrollY();

        double vpX = (scrollX - canvasMinX.get()) * scaleX + PADDING;
        double vpY = (scrollY - canvasMinY.get()) * scaleY + PADDING;

        // Clamp to map bounds
        vpX = Math.max(PADDING, Math.min(vpX, mapWidth - vpWidth + PADDING));
        vpY = Math.max(PADDING, Math.min(vpY, mapHeight - vpHeight + PADDING));

        viewportRect.setWidth(vpWidth);
        viewportRect.setHeight(vpHeight);
        viewportRect.setLayoutX(vpX);
        viewportRect.setLayoutY(vpY);
    }

    /**
     * Toggle visibility of the minimap.
     */
    public void toggle() {
        setVisible(!isVisible());
        setManaged(isVisible());
        if (isVisible()) {
            update();
        }
    }

    /**
     * Show the minimap.
     */
    public void show() {
        setVisible(true);
        setManaged(true);
        update();
    }

    /**
     * Hide the minimap.
     */
    public void hide() {
        setVisible(false);
        setManaged(false);
    }
}
