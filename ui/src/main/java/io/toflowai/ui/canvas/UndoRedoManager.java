package io.toflowai.ui.canvas;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Manages undo/redo operations for canvas commands.
 * Uses two stacks: one for undo, one for redo.
 */
public class UndoRedoManager {

    private static final int MAX_HISTORY_SIZE = 50;

    private final Deque<CanvasCommand> undoStack = new ArrayDeque<>();
    private final Deque<CanvasCommand> redoStack = new ArrayDeque<>();

    private Runnable onStateChanged;

    /**
     * Execute a command and add it to the undo stack.
     */
    public void executeCommand(CanvasCommand command) {
        command.execute();
        undoStack.push(command);

        // Clear redo stack after new action
        redoStack.clear();

        // Trim history if too large
        while (undoStack.size() > MAX_HISTORY_SIZE) {
            undoStack.removeLast();
        }

        notifyStateChanged();
    }

    /**
     * Undo the last command.
     */
    public void undo() {
        if (!canUndo())
            return;

        CanvasCommand command = undoStack.pop();
        command.undo();
        redoStack.push(command);

        notifyStateChanged();
    }

    /**
     * Redo the last undone command.
     */
    public void redo() {
        if (!canRedo())
            return;

        CanvasCommand command = redoStack.pop();
        command.execute();
        undoStack.push(command);

        notifyStateChanged();
    }

    /**
     * Check if undo is available.
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /**
     * Check if redo is available.
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * Get the description of the next undo command.
     */
    public String getUndoDescription() {
        if (undoStack.isEmpty())
            return null;
        return undoStack.peek().getDescription();
    }

    /**
     * Get the description of the next redo command.
     */
    public String getRedoDescription() {
        if (redoStack.isEmpty())
            return null;
        return redoStack.peek().getDescription();
    }

    /**
     * Clear all history.
     */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
        notifyStateChanged();
    }

    /**
     * Set a callback for when undo/redo state changes.
     */
    public void setOnStateChanged(Runnable callback) {
        this.onStateChanged = callback;
    }

    private void notifyStateChanged() {
        if (onStateChanged != null) {
            onStateChanged.run();
        }
    }

    /**
     * Get the number of commands in undo stack.
     */
    public int getUndoCount() {
        return undoStack.size();
    }

    /**
     * Get the number of commands in redo stack.
     */
    public int getRedoCount() {
        return redoStack.size();
    }
}
