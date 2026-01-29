package io.toflowai.app.service;

import io.toflowai.common.domain.Node;

import java.util.Map;

/**
 * Interface for node executors.
 * Each node type has its own executor implementation.
 */
public interface NodeExecutor {

    /**
     * Execute the node with given input.
     * @param node The node configuration
     * @param input Input data from previous nodes
     * @param context Execution context
     * @return Output data to pass to next nodes
     */
    Map<String, Object> execute(Node node, Map<String, Object> input, ExecutionService.ExecutionContext context);

    /**
     * Get the node type this executor handles.
     */
    String getNodeType();
}
