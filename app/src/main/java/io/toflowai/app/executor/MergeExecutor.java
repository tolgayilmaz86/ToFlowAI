package io.toflowai.app.executor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import io.toflowai.app.service.ExecutionService;
import io.toflowai.app.service.NodeExecutor;
import io.toflowai.common.domain.Node;

/**
 * Merge node - combines data from multiple input branches.
 * 
 * Parameters:
 * - mode: How to merge inputs
 * - "waitAll": Wait for all inputs before proceeding (default)
 * - "waitAny": Proceed when any input arrives
 * - "append": Append all inputs to a list
 * - "merge": Merge all input objects (later inputs override)
 * - inputCount: Expected number of inputs (for waitAll mode)
 * - timeout: Timeout in seconds for waiting (default: 300)
 * - outputKey: Key under which to store merged data (default: "merged")
 */
@Component
public class MergeExecutor implements NodeExecutor {

    // Store pending inputs per execution context
    private final ConcurrentHashMap<String, MergeState> pendingMerges = new ConcurrentHashMap<>();

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> params = node.parameters();

        String mode = (String) params.getOrDefault("mode", "waitAll");
        int inputCount = ((Number) params.getOrDefault("inputCount", 2)).intValue();
        int timeout = ((Number) params.getOrDefault("timeout", 300)).intValue();
        String outputKey = (String) params.getOrDefault("outputKey", "merged");

        // Create unique key for this merge point in this execution
        String mergeKey = context.getExecutionId() + ":" + node.id();

        return switch (mode.toLowerCase()) {
            case "waitany" -> handleWaitAny(input, outputKey);
            case "append" -> handleAppend(mergeKey, input, inputCount, timeout, outputKey);
            case "merge" -> handleMerge(mergeKey, input, inputCount, timeout, outputKey);
            default -> handleWaitAll(mergeKey, input, inputCount, timeout, outputKey);
        };
    }

    private Map<String, Object> handleWaitAny(Map<String, Object> input, String outputKey) {
        // Just pass through the first input that arrives
        Map<String, Object> output = new HashMap<>();
        output.put(outputKey, input);
        output.put("_mergeMode", "waitAny");
        output.put("_inputsReceived", 1);
        return output;
    }

    private Map<String, Object> handleWaitAll(String mergeKey, Map<String, Object> input,
            int inputCount, int timeout, String outputKey) {
        MergeState state = pendingMerges.computeIfAbsent(mergeKey,
                k -> new MergeState(inputCount, timeout));

        state.addInput(input);

        if (state.isComplete()) {
            pendingMerges.remove(mergeKey);

            Map<String, Object> output = new HashMap<>();
            output.put(outputKey, state.getInputs());
            output.put("_mergeMode", "waitAll");
            output.put("_inputsReceived", state.getInputs().size());
            return output;
        }

        // Wait for more inputs
        try {
            if (state.waitForCompletion()) {
                pendingMerges.remove(mergeKey);

                Map<String, Object> output = new HashMap<>();
                output.put(outputKey, state.getInputs());
                output.put("_mergeMode", "waitAll");
                output.put("_inputsReceived", state.getInputs().size());
                return output;
            } else {
                // Timeout - return what we have
                pendingMerges.remove(mergeKey);

                Map<String, Object> output = new HashMap<>();
                output.put(outputKey, state.getInputs());
                output.put("_mergeMode", "waitAll");
                output.put("_timedOut", true);
                output.put("_inputsReceived", state.getInputs().size());
                output.put("_inputsExpected", inputCount);
                return output;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pendingMerges.remove(mergeKey);

            Map<String, Object> output = new HashMap<>();
            output.put(outputKey, state.getInputs());
            output.put("_mergeMode", "waitAll");
            output.put("_interrupted", true);
            return output;
        }
    }

    private Map<String, Object> handleAppend(String mergeKey, Map<String, Object> input,
            int inputCount, int timeout, String outputKey) {
        MergeState state = pendingMerges.computeIfAbsent(mergeKey,
                k -> new MergeState(inputCount, timeout));

        state.addInput(input);

        if (state.isComplete()) {
            pendingMerges.remove(mergeKey);

            Map<String, Object> output = new HashMap<>();
            output.put(outputKey, state.getInputs()); // List of all inputs
            output.put("_mergeMode", "append");
            output.put("_inputsReceived", state.getInputs().size());
            return output;
        }

        try {
            state.waitForCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        pendingMerges.remove(mergeKey);

        Map<String, Object> output = new HashMap<>();
        output.put(outputKey, state.getInputs());
        output.put("_mergeMode", "append");
        output.put("_inputsReceived", state.getInputs().size());
        return output;
    }

    private Map<String, Object> handleMerge(String mergeKey, Map<String, Object> input,
            int inputCount, int timeout, String outputKey) {
        MergeState state = pendingMerges.computeIfAbsent(mergeKey,
                k -> new MergeState(inputCount, timeout));

        state.addInput(input);

        if (state.isComplete()) {
            pendingMerges.remove(mergeKey);

            // Merge all inputs into one map
            Map<String, Object> merged = new HashMap<>();
            for (Map<String, Object> inputMap : state.getInputs()) {
                merged.putAll(inputMap);
            }

            Map<String, Object> output = new HashMap<>();
            output.put(outputKey, merged);
            output.put("_mergeMode", "merge");
            output.put("_inputsReceived", state.getInputs().size());
            return output;
        }

        try {
            state.waitForCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        pendingMerges.remove(mergeKey);

        // Merge all inputs into one map
        Map<String, Object> merged = new HashMap<>();
        for (Map<String, Object> inputMap : state.getInputs()) {
            merged.putAll(inputMap);
        }

        Map<String, Object> output = new HashMap<>();
        output.put(outputKey, merged);
        output.put("_mergeMode", "merge");
        output.put("_inputsReceived", state.getInputs().size());
        return output;
    }

    @Override
    public String getNodeType() {
        return "merge";
    }

    private static class MergeState {
        private final List<Map<String, Object>> inputs = Collections.synchronizedList(new ArrayList<>());
        private final int expectedCount;
        private final int timeoutSeconds;
        private final CountDownLatch latch;

        MergeState(int expectedCount, int timeoutSeconds) {
            this.expectedCount = expectedCount;
            this.timeoutSeconds = timeoutSeconds;
            this.latch = new CountDownLatch(expectedCount);
        }

        void addInput(Map<String, Object> input) {
            inputs.add(new HashMap<>(input));
            latch.countDown();
        }

        boolean isComplete() {
            return inputs.size() >= expectedCount;
        }

        boolean waitForCompletion() throws InterruptedException {
            return latch.await(timeoutSeconds, TimeUnit.SECONDS);
        }

        List<Map<String, Object>> getInputs() {
            return new ArrayList<>(inputs);
        }
    }
}
