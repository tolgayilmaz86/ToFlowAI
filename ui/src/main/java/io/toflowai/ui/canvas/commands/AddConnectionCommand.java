package io.toflowai.ui.canvas.commands;

import io.toflowai.ui.canvas.CanvasCommand;
import io.toflowai.ui.canvas.WorkflowCanvas;

/**
 * Command for creating a connection between nodes.
 */
public class AddConnectionCommand implements CanvasCommand {

    private final WorkflowCanvas canvas;
    private final String sourceNodeId;
    private final String targetNodeId;
    private String createdConnectionId;

    public AddConnectionCommand(WorkflowCanvas canvas, String sourceNodeId, String targetNodeId) {
        this.canvas = canvas;
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
    }

    @Override
    public void execute() {
        createdConnectionId = canvas.createConnectionInternal(sourceNodeId, targetNodeId);
    }

    @Override
    public void undo() {
        if (createdConnectionId != null) {
            canvas.deleteConnectionInternal(createdConnectionId);
        }
    }

    @Override
    public String getDescription() {
        return "Connect nodes";
    }
}
