package io.toflowai.app.executor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import io.toflowai.app.service.ExecutionService;
import io.toflowai.app.service.NodeExecutor;
import io.toflowai.app.service.NodeExecutorRegistry;
import io.toflowai.common.domain.Node;

/**
 * Executor for rate limiting operations using token bucket algorithm.
 * Throttles API calls or operations to prevent overwhelming external services.
 * 
 * Configuration:
 * - operations: List of operations to execute with rate limiting
 * - bucketId: Identifier for the rate limit bucket (default: "default")
 * - tokensPerSecond: Rate of token refill (default: 10)
 * - maxTokens: Maximum bucket capacity (default: 100)
 * - tokensPerRequest: Tokens consumed per request (default: 1)
 * - waitForTokens: Wait for tokens if unavailable (default: true)
 * - maxWaitMs: Maximum wait time for tokens (default: 60000)
 * - strategy: "token_bucket" or "sliding_window" (default: "token_bucket")
 * - windowSizeMs: Window size for sliding window (default: 1000)
 * - maxRequestsPerWindow: Max requests per window for sliding window (default:
 * 10)
 * 
 * Output includes rate limit statistics:
 * - tokensRemaining: Tokens left in bucket
 * - waitedMs: Time spent waiting for tokens
 * - throttled: Whether the request was throttled
 */
@Component
public class RateLimitExecutor implements NodeExecutor {

    private final NodeExecutorRegistry nodeExecutorRegistry;

    // Global rate limit buckets (shared across executions)
    private static final ConcurrentHashMap<String, TokenBucket> tokenBuckets = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, SlidingWindow> slidingWindows = new ConcurrentHashMap<>();

    public RateLimitExecutor(@Lazy NodeExecutorRegistry nodeExecutorRegistry) {
        this.nodeExecutorRegistry = nodeExecutorRegistry;
    }

    @Override
    public String getNodeType() {
        return "rate_limit";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> config = node.parameters();

        List<Map<String, Object>> operations = (List<Map<String, Object>>) config.get("operations");
        String bucketId = getStringConfig(config, "bucketId", "default");
        String strategy = getStringConfig(config, "strategy", "token_bucket");
        boolean waitForTokens = getBooleanConfig(config, "waitForTokens", true);
        long maxWaitMs = getLongConfig(config, "maxWaitMs", 60000L);

        long startTime = System.currentTimeMillis();
        boolean acquired = false;
        long waitedMs = 0;

        if (strategy.equals("sliding_window")) {
            acquired = acquireSlidingWindow(config, bucketId, waitForTokens, maxWaitMs);
        } else {
            acquired = acquireTokenBucket(config, bucketId, waitForTokens, maxWaitMs);
        }

        waitedMs = System.currentTimeMillis() - startTime;

        Map<String, Object> output = new HashMap<>();
        output.put("bucketId", bucketId);
        output.put("strategy", strategy);
        output.put("waitedMs", waitedMs);
        output.put("throttled", !acquired);

        if (!acquired) {
            output.put("error", "Rate limit exceeded, could not acquire tokens within timeout");
            output.put("success", false);
            return output;
        }

        // Add bucket stats
        if (strategy.equals("sliding_window")) {
            SlidingWindow window = slidingWindows.get(bucketId);
            if (window != null) {
                output.put("requestsInWindow", window.getRequestCount());
            }
        } else {
            TokenBucket bucket = tokenBuckets.get(bucketId);
            if (bucket != null) {
                output.put("tokensRemaining", bucket.getAvailableTokens());
            }
        }

        // Execute operations if we have any
        if (operations != null && !operations.isEmpty()) {
            try {
                Map<String, Object> result = executeOperations(operations, input, context);
                output.put("success", true);
                output.put("result", result);
                output.putAll(result);
            } catch (Exception e) {
                output.put("success", false);
                output.put("error", e.getMessage());
            }
        } else {
            output.put("success", true);
            output.putAll(input);
        }

        return output;
    }

    private boolean acquireTokenBucket(Map<String, Object> config, String bucketId, boolean wait, long maxWaitMs) {
        double tokensPerSecond = getDoubleConfig(config, "tokensPerSecond", 10.0);
        int maxTokens = getIntConfig(config, "maxTokens", 100);
        int tokensPerRequest = getIntConfig(config, "tokensPerRequest", 1);

        TokenBucket bucket = tokenBuckets.computeIfAbsent(bucketId,
                k -> new TokenBucket(maxTokens, tokensPerSecond));

        if (wait) {
            return bucket.acquireWithWait(tokensPerRequest, maxWaitMs);
        } else {
            return bucket.tryAcquire(tokensPerRequest);
        }
    }

    private boolean acquireSlidingWindow(Map<String, Object> config, String bucketId, boolean wait, long maxWaitMs) {
        long windowSizeMs = getLongConfig(config, "windowSizeMs", 1000L);
        int maxRequestsPerWindow = getIntConfig(config, "maxRequestsPerWindow", 10);

        SlidingWindow window = slidingWindows.computeIfAbsent(bucketId,
                k -> new SlidingWindow(windowSizeMs, maxRequestsPerWindow));

        if (wait) {
            return window.acquireWithWait(maxWaitMs);
        } else {
            return window.tryAcquire();
        }
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
                    "ratelimit_" + UUID.randomUUID().toString().substring(0, 8),
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

    /**
     * Token Bucket implementation for rate limiting.
     */
    private static class TokenBucket {
        private final int maxTokens;
        private final double tokensPerSecond;
        private double availableTokens;
        private long lastRefillTime;
        private final ReentrantLock lock = new ReentrantLock();

        TokenBucket(int maxTokens, double tokensPerSecond) {
            this.maxTokens = maxTokens;
            this.tokensPerSecond = tokensPerSecond;
            this.availableTokens = maxTokens;
            this.lastRefillTime = System.nanoTime();
        }

        private void refill() {
            long now = System.nanoTime();
            double elapsedSeconds = (now - lastRefillTime) / 1_000_000_000.0;
            double tokensToAdd = elapsedSeconds * tokensPerSecond;
            availableTokens = Math.min(maxTokens, availableTokens + tokensToAdd);
            lastRefillTime = now;
        }

        boolean tryAcquire(int tokens) {
            lock.lock();
            try {
                refill();
                if (availableTokens >= tokens) {
                    availableTokens -= tokens;
                    return true;
                }
                return false;
            } finally {
                lock.unlock();
            }
        }

        boolean acquireWithWait(int tokens, long maxWaitMs) {
            long deadline = System.currentTimeMillis() + maxWaitMs;

            while (System.currentTimeMillis() < deadline) {
                if (tryAcquire(tokens)) {
                    return true;
                }

                // Calculate wait time until enough tokens are available
                lock.lock();
                try {
                    refill();
                    double tokensNeeded = tokens - availableTokens;
                    if (tokensNeeded <= 0)
                        continue;

                    long waitMs = (long) ((tokensNeeded / tokensPerSecond) * 1000) + 10;
                    waitMs = Math.min(waitMs, deadline - System.currentTimeMillis());

                    if (waitMs > 0) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(waitMs);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return false;
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }

            return tryAcquire(tokens);
        }

        double getAvailableTokens() {
            lock.lock();
            try {
                refill();
                return availableTokens;
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Sliding Window implementation for rate limiting.
     */
    private static class SlidingWindow {
        private final long windowSizeMs;
        private final int maxRequests;
        private final LinkedList<Long> requestTimestamps = new LinkedList<>();
        private final ReentrantLock lock = new ReentrantLock();

        SlidingWindow(long windowSizeMs, int maxRequests) {
            this.windowSizeMs = windowSizeMs;
            this.maxRequests = maxRequests;
        }

        private void cleanOldRequests() {
            long now = System.currentTimeMillis();
            long cutoff = now - windowSizeMs;
            while (!requestTimestamps.isEmpty() && requestTimestamps.peek() < cutoff) {
                requestTimestamps.poll();
            }
        }

        boolean tryAcquire() {
            lock.lock();
            try {
                cleanOldRequests();
                if (requestTimestamps.size() < maxRequests) {
                    requestTimestamps.add(System.currentTimeMillis());
                    return true;
                }
                return false;
            } finally {
                lock.unlock();
            }
        }

        boolean acquireWithWait(long maxWaitMs) {
            long deadline = System.currentTimeMillis() + maxWaitMs;

            while (System.currentTimeMillis() < deadline) {
                if (tryAcquire()) {
                    return true;
                }

                // Wait until oldest request expires
                lock.lock();
                try {
                    cleanOldRequests();
                    if (requestTimestamps.isEmpty())
                        continue;

                    long oldestExpiry = requestTimestamps.peek() + windowSizeMs;
                    long waitMs = oldestExpiry - System.currentTimeMillis() + 10;
                    waitMs = Math.min(waitMs, deadline - System.currentTimeMillis());

                    if (waitMs > 0) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(waitMs);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return false;
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }

            return tryAcquire();
        }

        int getRequestCount() {
            lock.lock();
            try {
                cleanOldRequests();
                return requestTimestamps.size();
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Clear all rate limit buckets. Useful for testing.
     */
    public static void clearAllBuckets() {
        tokenBuckets.clear();
        slidingWindows.clear();
    }

    /**
     * Clear a specific bucket.
     */
    public static void clearBucket(String bucketId) {
        tokenBuckets.remove(bucketId);
        slidingWindows.remove(bucketId);
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
