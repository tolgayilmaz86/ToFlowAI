package io.toflowai.common.domain;

import java.util.Map;

/**
 * Represents a node within a workflow.
 * Uses Java 25 record for immutable data carrier.
 *
 * @param id          Unique identifier within the workflow (UUID string)
 * @param type        Node type identifier (e.g., "httpRequest", "code", "if")
 * @param name        User-defined display name
 * @param position    Position on the canvas (x, y coordinates)
 * @param parameters  Node-specific configuration parameters
 * @param credentials Reference to credential ID if needed
 * @param disabled    Whether the node is disabled
 * @param notes       User notes/documentation
 */
public record Node(
        String id,
        String type,
        String name,
        Position position,
        Map<String, Object> parameters,
        Long credentialId,
        boolean disabled,
        String notes) {
    /**
     * Canvas position record.
     */
    public record Position(double x, double y) {
        public Position {
            // Validate coordinates are reasonable
            if (x < -10000 || x > 10000 || y < -10000 || y > 10000) {
                throw new IllegalArgumentException("Position coordinates out of bounds");
            }
        }
    }

    /**
     * Compact constructor with validation.
     */
    public Node {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Node id cannot be null or blank");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Node type cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            name = type; // Default to type if no name provided
        }
        if (position == null) {
            position = new Position(0, 0);
        }
        if (parameters == null) {
            parameters = Map.of();
        }
    }

    /**
     * Create a new node with updated position.
     */
    public Node withPosition(Position newPosition) {
        return new Node(id, type, name, newPosition, parameters, credentialId, disabled, notes);
    }

    /**
     * Create a new node with updated parameters.
     */
    public Node withParameters(Map<String, Object> newParameters) {
        return new Node(id, type, name, position, newParameters, credentialId, disabled, notes);
    }

    /**
     * Create a new node with updated disabled state.
     */
    public Node withDisabled(boolean newDisabled) {
        return new Node(id, type, name, position, parameters, credentialId, newDisabled, notes);
    }
}
