package io.toflowai.ui.canvas.commands;

import io.toflowai.ui.canvas.CanvasCommand;
import io.toflowai.ui.canvas.WorkflowCanvas;

/**
 * Command for moving a node on the canvas.
 */
public class MoveNodeCommand implements CanvasCommand {

    private final WorkflowCanvas canvas;
    private final String nodeId;
    private final double oldX, oldY;
    private final double newX, newY;

    public MoveNodeCommand(WorkflowCanvas canvas, String nodeId,
            double oldX, double oldY, double newX, double newY) {
        this.canvas = canvas;
        this.nodeId = nodeId;
        this.oldX = oldX;
        this.oldY = oldY;
        this.newX = newX;
        this.newY = newY;
    }

    @Override
    public void execute() {
        canvas.setNodePosition(nodeId, newX, newY);
    }

    @Override
    public void undo() {
        canvas.setNodePosition(nodeId, oldX, oldY);
    }

    @Override
    public String getDescription() {
        return "Move node";
    }
}
