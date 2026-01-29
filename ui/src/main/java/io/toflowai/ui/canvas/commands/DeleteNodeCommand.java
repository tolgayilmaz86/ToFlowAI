package io.toflowai.ui.canvas.commands;

import io.toflowai.common.domain.Node;
import io.toflowai.ui.canvas.CanvasCommand;
import io.toflowai.ui.canvas.WorkflowCanvas;

/**
 * Command for deleting a node from the canvas.
 */
public class DeleteNodeCommand implements CanvasCommand {

    private final WorkflowCanvas canvas;
    private final String nodeId;
    private Node deletedNode;

    public DeleteNodeCommand(WorkflowCanvas canvas, String nodeId) {
        this.canvas = canvas;
        this.nodeId = nodeId;
    }

    @Override
    public void execute() {
        deletedNode = canvas.getNodeById(nodeId);
        if (deletedNode != null) {
            canvas.deleteNodeInternal(nodeId);
        }
    }

    @Override
    public void undo() {
        if (deletedNode != null) {
            canvas.restoreNode(deletedNode);
        }
    }

    @Override
    public String getDescription() {
        return "Delete " + (deletedNode != null ? deletedNode.name() : "node");
    }
}
