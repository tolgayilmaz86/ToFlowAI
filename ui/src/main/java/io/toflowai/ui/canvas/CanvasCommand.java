package io.toflowai.ui.canvas;

/**
 * Command interface for undo/redo operations.
 * Implements the Command pattern for canvas actions.
 */
public interface CanvasCommand {

    /**
     * Execute the command.
     */
    void execute();

    /**
     * Undo the command - restore previous state.
     */
    void undo();

    /**
     * Get a description of the command for display.
     */
    String getDescription();
}
