package io.toflowai.common.dto;

import io.toflowai.common.domain.Connection;
import io.toflowai.common.domain.Node;
import io.toflowai.common.enums.TriggerType;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for Workflow.
 * Used for API communication and serialization.
 *
 * @param id             Workflow unique identifier
 * @param name           Workflow display name
 * @param description    Optional description
 * @param nodes          List of nodes in the workflow
 * @param connections    List of connections between nodes
 * @param settings       Workflow-level settings
 * @param isActive       Whether workflow is active (for scheduled/webhook
 *                       triggers)
 * @param triggerType    Type of trigger (MANUAL, SCHEDULE, WEBHOOK)
 * @param cronExpression Cron expression for scheduled workflows
 * @param createdAt      Creation timestamp
 * @param updatedAt      Last update timestamp
 * @param lastExecuted   Last execution timestamp
 * @param version        Workflow version number
 */
public record WorkflowDTO(
        Long id,
        String name,
        String description,
        List<Node> nodes,
        List<Connection> connections,
        Map<String, Object> settings,
        boolean isActive,
        TriggerType triggerType,
        String cronExpression,
        Instant createdAt,
        Instant updatedAt,
        Instant lastExecuted,
        int version) {
    /**
     * Compact constructor with validation and defaults.
     */
    public WorkflowDTO {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Workflow name cannot be null or blank");
        }
        if (nodes == null) {
            nodes = List.of();
        }
        if (connections == null) {
            connections = List.of();
        }
        if (settings == null) {
            settings = Map.of();
        }
        if (triggerType == null) {
            triggerType = TriggerType.MANUAL;
        }
        if (version < 1) {
            version = 1;
        }
    }

    /**
     * Factory method for creating a new workflow with minimal required fields.
     */
    public static WorkflowDTO create(String name) {
        return new WorkflowDTO(
                null, name, null, List.of(), List.of(), Map.of(),
                false, TriggerType.MANUAL, null,
                Instant.now(), Instant.now(), null, 1);
    }

    /**
     * Factory method for creating a new workflow with description.
     */
    public static WorkflowDTO create(String name, String description) {
        return new WorkflowDTO(
                null, name, description, List.of(), List.of(), Map.of(),
                false, TriggerType.MANUAL, null,
                Instant.now(), Instant.now(), null, 1);
    }

    /**
     * Find a node by its ID.
     */
    public Node findNode(String nodeId) {
        return nodes.stream()
                .filter(n -> n.id().equals(nodeId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get all trigger nodes (nodes with no incoming connections).
     */
    public List<Node> getTriggerNodes() {
        var targetNodeIds = connections.stream()
                .map(Connection::targetNodeId)
                .toList();

        return nodes.stream()
                .filter(n -> !targetNodeIds.contains(n.id()))
                .toList();
    }

    /**
     * Get connections from a specific node.
     */
    public List<Connection> getOutgoingConnections(String nodeId) {
        return connections.stream()
                .filter(c -> c.sourceNodeId().equals(nodeId))
                .toList();
    }

    /**
     * Get connections to a specific node.
     */
    public List<Connection> getIncomingConnections(String nodeId) {
        return connections.stream()
                .filter(c -> c.targetNodeId().equals(nodeId))
                .toList();
    }

    /**
     * Create a copy with updated nodes.
     */
    public WorkflowDTO withNodes(List<Node> newNodes) {
        return new WorkflowDTO(
                id, name, description, newNodes, connections, settings,
                isActive, triggerType, cronExpression,
                createdAt, Instant.now(), lastExecuted, version);
    }

    /**
     * Create a copy with updated name.
     */
    public WorkflowDTO withName(String newName) {
        return new WorkflowDTO(
                id, newName, description, nodes, connections, settings,
                isActive, triggerType, cronExpression,
                createdAt, Instant.now(), lastExecuted, version);
    }

    /**
     * Create a copy with updated connections.
     */
    public WorkflowDTO withConnections(List<Connection> newConnections) {
        return new WorkflowDTO(
                id, name, description, nodes, newConnections, settings,
                isActive, triggerType, cronExpression,
                createdAt, Instant.now(), lastExecuted, version);
    }

    /**
     * Create a copy with an added node.
     */
    public WorkflowDTO withAddedNode(Node node) {
        var newNodes = new java.util.ArrayList<>(nodes);
        newNodes.add(node);
        return withNodes(newNodes);
    }

    /**
     * Create a copy with an added connection.
     */
    public WorkflowDTO withAddedConnection(Connection connection) {
        var newConnections = new java.util.ArrayList<>(connections);
        newConnections.add(connection);
        return withConnections(newConnections);
    }
}
