package io.toflowai.ui.canvas;

import io.toflowai.common.domain.Connection;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Polygon;

/**
 * Visual representation of a connection between two nodes.
 * Uses a bezier curve with an arrow at the end.
 */
public class ConnectionLine extends Group {

    private static final Color CONNECTION_COLOR = Color.web("#4a9eff");
    private static final Color CONNECTION_SELECTED_COLOR = Color.web("#ffd43b");
    private static final double STROKE_WIDTH = 2;

    private final Connection connection;
    private final NodeView sourceNode;
    private final NodeView targetNode;

    private final CubicCurve curve;
    private final Polygon arrow;

    private boolean selected = false;

    public ConnectionLine(Connection connection, NodeView sourceNode, NodeView targetNode) {
        this.connection = connection;
        this.sourceNode = sourceNode;
        this.targetNode = targetNode;

        // Create bezier curve
        curve = new CubicCurve();
        curve.setFill(null);
        curve.setStroke(CONNECTION_COLOR);
        curve.setStrokeWidth(STROKE_WIDTH);
        curve.getStyleClass().add("connection-line");

        // Create arrow
        arrow = new Polygon();
        arrow.setFill(CONNECTION_COLOR);
        arrow.getStyleClass().add("connection-arrow");

        getChildren().addAll(curve, arrow);

        // Bind positions
        setupBindings();

        // Initial position
        updatePosition();
    }

    private void setupBindings() {
        // Update when source or target moves
        sourceNode.layoutXProperty().addListener((obs, old, val) -> updatePosition());
        sourceNode.layoutYProperty().addListener((obs, old, val) -> updatePosition());
        targetNode.layoutXProperty().addListener((obs, old, val) -> updatePosition());
        targetNode.layoutYProperty().addListener((obs, old, val) -> updatePosition());
    }

    public void updatePosition() {
        // Source point (right side of source node)
        double startX = sourceNode.getOutputX();
        double startY = sourceNode.getOutputY();

        // Target point (left side of target node)
        double endX = targetNode.getInputX();
        double endY = targetNode.getInputY();

        // Control points for bezier curve
        double dx = Math.abs(endX - startX) * 0.5;
        double ctrlX1 = startX + dx;
        double ctrlY1 = startY;
        double ctrlX2 = endX - dx;
        double ctrlY2 = endY;

        // Set curve points
        curve.setStartX(startX);
        curve.setStartY(startY);
        curve.setControlX1(ctrlX1);
        curve.setControlY1(ctrlY1);
        curve.setControlX2(ctrlX2);
        curve.setControlY2(ctrlY2);
        curve.setEndX(endX);
        curve.setEndY(endY);

        // Update arrow position and rotation
        updateArrow(ctrlX2, ctrlY2, endX, endY);
    }

    private void updateArrow(double fromX, double fromY, double toX, double toY) {
        double arrowSize = 8;
        double angle = Math.atan2(toY - fromY, toX - fromX);

        double sin = Math.sin(angle);
        double cos = Math.cos(angle);

        // Arrow points
        double x1 = toX - arrowSize * cos - arrowSize * 0.5 * sin;
        double y1 = toY - arrowSize * sin + arrowSize * 0.5 * cos;
        double x2 = toX - arrowSize * cos + arrowSize * 0.5 * sin;
        double y2 = toY - arrowSize * sin - arrowSize * 0.5 * cos;

        arrow.getPoints().setAll(
                toX, toY,
                x1, y1,
                x2, y2);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        Color color = selected ? CONNECTION_SELECTED_COLOR : CONNECTION_COLOR;
        curve.setStroke(color);
        arrow.setFill(color);
    }

    public boolean isSelected() {
        return selected;
    }

    public Connection getConnection() {
        return connection;
    }

    public boolean involvesNode(String nodeId) {
        return connection.sourceNodeId().equals(nodeId) ||
                connection.targetNodeId().equals(nodeId);
    }
}
