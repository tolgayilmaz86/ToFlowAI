package io.toflowai.ui.canvas.commands;

import io.toflowai.common.domain.Connection;
import io.toflowai.ui.canvas.CanvasCommand;
import io.toflowai.ui.canvas.WorkflowCanvas;

/**
 * Command for deleting a connection between nodes.
 */
public class DeleteConnectionCommand implements CanvasCommand {

    private final WorkflowCanvas canvas;
    private final String connectionId;
    private Connection deletedConnection;

    public DeleteConnectionCommand(WorkflowCanvas canvas, String connectionId) {
        this.canvas = canvas;
        this.connectionId = connectionId;
    }

    @Override
    public void execute() {
        deletedConnection = canvas.getConnectionById(connectionId);
        if (deletedConnection != null) {
            canvas.deleteConnectionInternal(connectionId);
        }
    }

    @Override
    public void undo() {
        if (deletedConnection != null) {
            canvas.restoreConnection(deletedConnection);
        }
    }

    @Override
    public String getDescription() {
        return "Delete connection";
    }
}
