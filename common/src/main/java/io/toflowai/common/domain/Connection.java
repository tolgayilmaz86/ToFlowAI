package io.toflowai.common.domain;

/**
 * Represents a connection between two nodes in a workflow.
 * Uses Java 25 record for immutable data carrier.
 *
 * @param id             Unique identifier for the connection
 * @param sourceNodeId   ID of the source node
 * @param sourceOutput   Output handle name on source node (e.g., "main", "true", "false")
 * @param targetNodeId   ID of the target node
 * @param targetInput    Input handle name on target node (usually "main")
 */
public record Connection(
        String id,
        String sourceNodeId,
        String sourceOutput,
        String targetNodeId,
        String targetInput
) {
    /**
     * Compact constructor with validation.
     */
    public Connection {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Connection id cannot be null or blank");
        }
        if (sourceNodeId == null || sourceNodeId.isBlank()) {
            throw new IllegalArgumentException("Source node id cannot be null or blank");
        }
        if (targetNodeId == null || targetNodeId.isBlank()) {
            throw new IllegalArgumentException("Target node id cannot be null or blank");
        }
        if (sourceOutput == null || sourceOutput.isBlank()) {
            sourceOutput = "main";
        }
        if (targetInput == null || targetInput.isBlank()) {
            targetInput = "main";
        }
        if (sourceNodeId.equals(targetNodeId)) {
            throw new IllegalArgumentException("Cannot connect a node to itself");
        }
    }

    /**
     * Factory method for creating a simple main-to-main connection.
     */
    public static Connection simple(String id, String sourceNodeId, String targetNodeId) {
        return new Connection(id, sourceNodeId, "main", targetNodeId, "main");
    }

    /**
     * Check if this connection involves the given node.
     */
    public boolean involvesNode(String nodeId) {
        return sourceNodeId.equals(nodeId) || targetNodeId.equals(nodeId);
    }
}
