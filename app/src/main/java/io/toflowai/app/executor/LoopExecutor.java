package io.toflowai.app.executor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.StructuredTaskScope;

import org.springframework.stereotype.Component;

import io.toflowai.app.service.ExecutionService;
import io.toflowai.app.service.NodeExecutor;
import io.toflowai.common.domain.Node;

/**
 * Loop node executor - iterates over arrays/collections.
 * Uses virtual threads for parallel execution when configured.
 */
@Component
public class LoopExecutor implements NodeExecutor {

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> params = node.parameters();

        String itemsField = (String) params.getOrDefault("items", "items");
        boolean parallel = (boolean) params.getOrDefault("parallel", false);
        int batchSize = (int) params.getOrDefault("batchSize", 10);

        Object itemsObj = input.get(itemsField);
        if (itemsObj == null) {
            return Map.of("results", List.of(), "count", 0);
        }

        List<?> items;
        if (itemsObj instanceof List<?> list) {
            items = list;
        } else if (itemsObj.getClass().isArray()) {
            items = java.util.Arrays.asList((Object[]) itemsObj);
        } else {
            items = List.of(itemsObj);
        }

        List<Map<String, Object>> results;

        if (parallel && items.size() > 1) {
            // Use virtual threads for parallel processing
            results = executeParallel(items, input, batchSize);
        } else {
            // Sequential execution
            results = executeSequential(items, input);
        }

        Map<String, Object> output = new HashMap<>(input);
        output.put("results", results);
        output.put("count", results.size());
        return output;
    }

    private List<Map<String, Object>> executeSequential(List<?> items, Map<String, Object> input) {
        List<Map<String, Object>> results = new ArrayList<>();
        int index = 0;
        for (Object item : items) {
            Map<String, Object> itemResult = new HashMap<>();
            itemResult.put("item", item);
            itemResult.put("index", index++);
            // In a real implementation, this would execute child nodes
            results.add(itemResult);
        }
        return results;
    }

    private List<Map<String, Object>> executeParallel(List<?> items, Map<String, Object> input, int batchSize) {
        List<Map<String, Object>> results = new ArrayList<>();

        // Process in batches using virtual threads with structured concurrency
        for (int i = 0; i < items.size(); i += batchSize) {
            int end = Math.min(i + batchSize, items.size());
            List<?> batch = items.subList(i, end);

            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                int startIndex = i;
                List<StructuredTaskScope.Subtask<Map<String, Object>>> subtasks = new ArrayList<>();

                for (int j = 0; j < batch.size(); j++) {
                    Object item = batch.get(j);
                    int index = startIndex + j;

                    // Each item processed in its own virtual thread
                    subtasks.add(scope.fork(() -> {
                        Map<String, Object> itemResult = new HashMap<>();
                        itemResult.put("item", item);
                        itemResult.put("index", index);
                        // Simulate processing - in real impl, would execute child nodes
                        return itemResult;
                    }));
                }

                scope.join();
                scope.throwIfFailed();

                // Collect results
                for (var subtask : subtasks) {
                    results.add(subtask.get());
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Parallel loop execution interrupted", e);
            } catch (java.util.concurrent.ExecutionException e) {
                throw new RuntimeException("Parallel loop execution failed", e.getCause());
            }
        }

        return results;
    }

    @Override
    public String getNodeType() {
        return "loop";
    }
}
