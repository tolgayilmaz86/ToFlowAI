package io.toflowai.common.enums;

/**
 * Categories for organizing node types in the palette.
 */
public enum NodeCategory {
    TRIGGER("Triggers", "Nodes that start workflow execution"),
    ACTION("Actions", "Nodes that perform operations"),
    FLOW("Flow Control", "Nodes that control execution flow"),
    DATA("Data", "Nodes that transform and manipulate data"),
    AI("AI & ML", "Artificial intelligence and machine learning nodes"),
    UTILITY("Utility", "Helper and utility nodes");

    private final String displayName;
    private final String description;

    NodeCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
