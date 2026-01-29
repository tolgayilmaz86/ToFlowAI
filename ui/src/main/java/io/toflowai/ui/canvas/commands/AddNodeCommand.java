package io.toflowai.ui.canvas.commands;

import io.toflowai.common.domain.Node;
import io.toflowai.ui.canvas.CanvasCommand;
import io.toflowai.ui.canvas.WorkflowCanvas;

/**
 * Command for adding a node to the canvas.
 */
public class AddNodeCommand implements CanvasCommand {

    private final WorkflowCanvas canvas;
    private final String nodeType;
    private final double x;
    private final double y;
    private String createdNodeId;

    public AddNodeCommand(WorkflowCanvas canvas, String nodeType, double x, double y) {
        this.canvas = canvas;
        this.nodeType = nodeType;
        this.x = x;
        this.y = y;
    }

    @Override
    public void execute() {
        Node node = canvas.createNodeInternal(nodeType, x, y);
        if (node != null) {
            createdNodeId = node.id();
        }
    }

    @Override
    public void undo() {
        if (createdNodeId != null) {
            canvas.deleteNodeInternal(createdNodeId);
        }
    }

    @Override
    public String getDescription() {
        return "Add " + nodeType.replaceAll("([A-Z])", " $1").trim();
    }
}
