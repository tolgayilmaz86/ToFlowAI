package io.toflowai.app.executor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import io.toflowai.app.service.ExecutionLogger;
import io.toflowai.app.service.ExecutionService;
import io.toflowai.app.service.NodeExecutor;
import io.toflowai.app.service.SettingsDefaults;
import io.toflowai.common.domain.Node;
import io.toflowai.common.service.SettingsServiceInterface;

/**
 * HTTP Request node executor.
 * Makes HTTP calls to external APIs.
 * Uses virtual threads for non-blocking I/O operations.
 */
@Component
public class HttpRequestExecutor implements NodeExecutor {

    private final HttpClient httpClient;
    private final int defaultTimeout;

    public HttpRequestExecutor(SettingsServiceInterface settingsService) {
        int connectTimeout = settingsService.getInt(SettingsDefaults.HTTP_CONNECT_TIMEOUT, 30);
        this.defaultTimeout = settingsService.getInt(SettingsDefaults.HTTP_READ_TIMEOUT, 30);

        // Configure HttpClient to use virtual threads for async operations
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeout))
                .executor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor())
                .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> params = node.parameters();

        // Combine input data with workflow settings for template interpolation
        Map<String, Object> templateData = new HashMap<>(input);
        if (context.getWorkflow().settings() != null) {
            templateData.putAll(context.getWorkflow().settings());
        }

        String url = interpolate((String) params.getOrDefault("url", ""), templateData, context);
        context.getExecutionLogger().custom(context.getExecutionId().toString(),
                ExecutionLogger.LogLevel.DEBUG, "HTTP Request URL: " + url, Map.of());
        String method = (String) params.getOrDefault("method", "GET");
        Map<String, String> headers = (Map<String, String>) params.getOrDefault("headers", Map.of());
        String body = interpolate((String) params.getOrDefault("body", ""), templateData, context);
        int timeout = (int) params.getOrDefault("timeout", defaultTimeout);

        try {
            // Build request
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeout));

            // Add headers
            for (Map.Entry<String, String> header : headers.entrySet()) {
                requestBuilder.header(header.getKey(), interpolate(header.getValue(), templateData, context));
            }

            // Add authentication if credential is specified
            if (node.credentialId() != null) {
                String credential = context.getDecryptedCredential(node.credentialId());
                requestBuilder.header("Authorization", "Bearer " + credential);
            }

            // Set method and body
            HttpRequest.BodyPublisher bodyPublisher = body.isBlank()
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body);

            requestBuilder.method(method.toUpperCase(), bodyPublisher);

            // Execute request
            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            context.getExecutionLogger().custom(context.getExecutionId().toString(),
                    ExecutionLogger.LogLevel.DEBUG, "HTTP Response: status=" + response.statusCode() +
                            ", body_length=" + (response.body() != null ? response.body().length() : 0),
                    Map.of());

            if (response.body() != null && response.body().length() > 0) {
                String preview = response.body().length() > 200 ? response.body().substring(0, 200) + "..."
                        : response.body();
                context.getExecutionLogger().custom(context.getExecutionId().toString(),
                        ExecutionLogger.LogLevel.TRACE, "HTTP Response Body: " + preview, Map.of());
            }

            // Build output
            Map<String, Object> output = new HashMap<>();
            output.put("statusCode", response.statusCode());
            output.put("body", response.body());
            output.put("headers", response.headers().map());
            output.put("success", response.statusCode() >= 200 && response.statusCode() < 300);

            // Add JSON response if detected
            addJsonResponseIfValid(output, response.body());

            return output;

        } catch (Exception e) {
            throw new RuntimeException("HTTP request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Simple template interpolation for {{ variable }} syntax.
     * Supports workflow settings and credentials by name.
     */
    private String interpolate(String template, Map<String, Object> data, ExecutionService.ExecutionContext context) {
        if (template == null)
            return "";

        String result = template;

        // First pass: replace variables from data map (input + settings)
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }

        // Second pass: replace credential references like {{credentialName}}
        // Look for patterns like {{someName}} that weren't replaced in the first pass
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{\\{([^}]+)\\}\\}");
        java.util.regex.Matcher matcher = pattern.matcher(result);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String varName = matcher.group(1);
            // Check if this is a credential name
            String credentialValue = context.getDecryptedCredentialByName(varName);
            if (credentialValue != null) {
                context.getExecutionLogger().custom(context.getExecutionId().toString(),
                        ExecutionLogger.LogLevel.DEBUG,
                        "Found credential '" + varName + "', value length: " + credentialValue.length(), Map.of());
                matcher.appendReplacement(sb, credentialValue);
            } else {
                context.getExecutionLogger().custom(context.getExecutionId().toString(),
                        ExecutionLogger.LogLevel.DEBUG, "Credential '" + varName + "' not found", Map.of());
                // Leave as-is if not found
                matcher.appendReplacement(sb, matcher.group(0));
            }
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Adds JSON response to output if the response body appears to be valid JSON.
     * Simple detection - in production, use proper JSON parsing library.
     */
    private void addJsonResponseIfValid(Map<String, Object> output, String responseBody) {
        if (responseBody != null && !responseBody.trim().isEmpty()) {
            String trimmed = responseBody.trim();
            // Basic JSON detection: starts with { or [ and ends with } or ]
            if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                    (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
                output.put("json", responseBody);
            }
        }
    }

    @Override
    public String getNodeType() {
        return "httpRequest";
    }
}
