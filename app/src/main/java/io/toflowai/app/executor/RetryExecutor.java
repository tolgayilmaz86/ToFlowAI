package io.toflowai.app.executor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import io.toflowai.app.service.ExecutionService;
import io.toflowai.app.service.NodeExecutor;
import io.toflowai.app.service.NodeExecutorRegistry;
import io.toflowai.common.domain.Node;

/**
 * Executor for retry logic with configurable backoff strategies.
 * Retries failed operations automatically with various retry strategies.
 * 
 * Configuration:
 * - operations: List of operations to execute with retry
 * - maxRetries: Maximum number of retry attempts (default: 3)
 * - backoffStrategy: "fixed", "linear", "exponential", "fibonacci" (default:
 * "exponential")
 * - initialDelayMs: Initial delay between retries in milliseconds (default:
 * 1000)
 * - maxDelayMs: Maximum delay between retries (default: 30000)
 * - multiplier: Backoff multiplier for exponential/linear (default: 2.0)
 * - jitter: Add random jitter to delays (default: true)
 * - jitterFactor: Maximum jitter as fraction of delay (default: 0.1)
 * - retryableErrors: List of error types to retry (empty = retry all)
 * - nonRetryableErrors: List of error types to NOT retry
 * 
 * Output includes retry statistics:
 * - attemptCount: Total attempts made
 * - totalDelayMs: Total time spent waiting
 * - success: Whether operation succeeded
 * - errors: List of errors from failed attempts
 */
@Component
public class RetryExecutor implements NodeExecutor {

    private final NodeExecutorRegistry nodeExecutorRegistry;
    private final Random random = new Random();

    public RetryExecutor(@Lazy NodeExecutorRegistry nodeExecutorRegistry) {
        this.nodeExecutorRegistry = nodeExecutorRegistry;
    }

    @Override
    public String getNodeType() {
        return "retry";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> config = node.parameters();

        List<Map<String, Object>> operations = (List<Map<String, Object>>) config.get("operations");
        if (operations == null || operations.isEmpty()) {
            return Map.of("error", "No operations configured", "input", input);
        }

        int maxRetries = getIntConfig(config, "maxRetries", 3);
        String backoffStrategy = getStringConfig(config, "backoffStrategy", "exponential");
        long initialDelayMs = getLongConfig(config, "initialDelayMs", 1000L);
        long maxDelayMs = getLongConfig(config, "maxDelayMs", 30000L);
        double multiplier = getDoubleConfig(config, "multiplier", 2.0);
        boolean jitter = getBooleanConfig(config, "jitter", true);
        double jitterFactor = getDoubleConfig(config, "jitterFactor", 0.1);
        List<String> retryableErrors = (List<String>) config.getOrDefault("retryableErrors", List.of());
        List<String> nonRetryableErrors = (List<String>) config.getOrDefault("nonRetryableErrors", List.of());

        List<Map<String, Object>> attemptErrors = new ArrayList<>();
        Map<String, Object> result = null;
        long totalDelayMs = 0;
        int attemptCount = 0;
        boolean success = false;
        long startTime = System.currentTimeMillis();

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            attemptCount = attempt + 1;

            try {
                result = executeOperations(operations, input, context);
                success = true;
                break; // Success!

            } catch (Exception e) {
                Map<String, Object> errorInfo = new HashMap<>();
                errorInfo.put("attempt", attemptCount);
                errorInfo.put("message", e.getMessage());
                errorInfo.put("type", e.getClass().getSimpleName());
                errorInfo.put("timestamp", java.time.Instant.now().toString());
                attemptErrors.add(errorInfo);

                // Check if this error should be retried
                if (!shouldRetry(e, retryableErrors, nonRetryableErrors)) {
                    break;
                }

                // Check if we have retries left
                if (attempt >= maxRetries) {
                    break;
                }

                // Calculate delay
                long delay = calculateDelay(attempt, backoffStrategy, initialDelayMs, maxDelayMs, multiplier);

                // Add jitter
                if (jitter) {
                    long jitterAmount = (long) (delay * jitterFactor * random.nextDouble());
                    delay = delay + (random.nextBoolean() ? jitterAmount : -jitterAmount);
                    delay = Math.max(0, delay);
                }

                totalDelayMs += delay;

                // Wait before retry
                try {
                    TimeUnit.MILLISECONDS.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // Build result
        Map<String, Object> output = new HashMap<>();
        output.put("success", success);
        output.put("attemptCount", attemptCount);
        output.put("maxRetries", maxRetries);
        output.put("totalDelayMs", totalDelayMs);
        output.put("totalTimeMs", System.currentTimeMillis() - startTime);
        output.put("backoffStrategy", backoffStrategy);

        if (success && result != null) {
            output.put("result", result);
            // Also merge result into output for convenience
            output.putAll(result);
        } else {
            output.put("errors", attemptErrors);
            if (!attemptErrors.isEmpty()) {
                output.put("lastError", attemptErrors.getLast());
            }
        }

        return output;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> executeOperations(
            List<Map<String, Object>> operations,
            Map<String, Object> input,
            ExecutionService.ExecutionContext context) {

        Map<String, Object> currentData = new HashMap<>(input);

        for (Map<String, Object> operation : operations) {
            String type = (String) operation.get("type");
            if (type == null)
                continue;

            Map<String, Object> opConfig = (Map<String, Object>) operation.getOrDefault("config", Map.of());
            String name = (String) operation.getOrDefault("name", type);

            Node tempNode = new Node(
                    "retry_" + UUID.randomUUID().toString().substring(0, 8),
                    type,
                    name,
                    new Node.Position(0, 0),
                    opConfig,
                    null,
                    false,
                    null);

            NodeExecutor executor = nodeExecutorRegistry.getExecutor(type);
            currentData = executor.execute(tempNode, currentData, context);
        }

        return currentData;
    }

    private long calculateDelay(int attempt, String strategy, long initialDelay, long maxDelay, double multiplier) {
        long delay = switch (strategy.toLowerCase()) {
            case "fixed" -> initialDelay;
            case "linear" -> (long) (initialDelay * (1 + attempt * multiplier));
            case "exponential" -> (long) (initialDelay * Math.pow(multiplier, attempt));
            case "fibonacci" -> initialDelay * fibonacci(attempt + 1);
            default -> (long) (initialDelay * Math.pow(multiplier, attempt));
        };

        return Math.min(delay, maxDelay);
    }

    private long fibonacci(int n) {
        if (n <= 1)
            return n;
        long a = 0, b = 1;
        for (int i = 2; i <= n; i++) {
            long temp = a + b;
            a = b;
            b = temp;
        }
        return b;
    }

    private boolean shouldRetry(Exception e, List<String> retryable, List<String> nonRetryable) {
        String errorType = e.getClass().getSimpleName();
        String errorFullType = e.getClass().getName();

        // Check non-retryable first
        if (!nonRetryable.isEmpty()) {
            for (String type : nonRetryable) {
                if (errorType.equalsIgnoreCase(type) || errorFullType.equalsIgnoreCase(type)) {
                    return false;
                }
            }
        }

        // If retryable list is empty, retry all errors
        if (retryable.isEmpty()) {
            return true;
        }

        // Check if error is in retryable list
        for (String type : retryable) {
            if (errorType.equalsIgnoreCase(type) || errorFullType.equalsIgnoreCase(type)) {
                return true;
            }
        }

        return false;
    }

    private int getIntConfig(Map<String, Object> config, String key, int defaultValue) {
        Object value = config.get(key);
        if (value == null)
            return defaultValue;
        if (value instanceof Number n)
            return n.intValue();
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private long getLongConfig(Map<String, Object> config, String key, long defaultValue) {
        Object value = config.get(key);
        if (value == null)
            return defaultValue;
        if (value instanceof Number n)
            return n.longValue();
        if (value instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private double getDoubleConfig(Map<String, Object> config, String key, double defaultValue) {
        Object value = config.get(key);
        if (value == null)
            return defaultValue;
        if (value instanceof Number n)
            return n.doubleValue();
        if (value instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private boolean getBooleanConfig(Map<String, Object> config, String key, boolean defaultValue) {
        Object value = config.get(key);
        if (value == null)
            return defaultValue;
        if (value instanceof Boolean b)
            return b;
        if (value instanceof String s)
            return Boolean.parseBoolean(s);
        return defaultValue;
    }

    private String getStringConfig(Map<String, Object> config, String key, String defaultValue) {
        Object value = config.get(key);
        if (value == null)
            return defaultValue;
        return value.toString();
    }
}
