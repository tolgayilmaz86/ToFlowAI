package io.toflowai.app.executor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.toflowai.app.service.ExecutionService;
import io.toflowai.app.service.NodeExecutor;
import io.toflowai.app.service.SettingsDefaults;
import io.toflowai.common.domain.Node;
import io.toflowai.common.service.SettingsServiceInterface;

/**
 * LLM Chat node - sends messages to LLM providers and returns the response.
 * 
 * Parameters:
 * - provider: "openai", "anthropic", "ollama", "azure", "custom" (default:
 * "openai")
 * - model: Model name (e.g., "gpt-4", "claude-3-opus", "llama2")
 * - apiKey: API key (can use ${credential.name} to reference stored
 * credentials)
 * - baseUrl: Base URL for custom/Ollama providers (default depends on provider)
 * - messages: Array of message objects with "role" and "content"
 * - systemPrompt: System prompt to prepend to messages
 * - prompt: Single user message (alternative to messages array)
 * - temperature: Sampling temperature (default: 0.7)
 * - maxTokens: Maximum tokens in response (default: 1024)
 * - timeout: Request timeout in seconds (default: 120)
 * - responseFormat: "text" or "json" (default: "text")
 * 
 * Output:
 * - response: The LLM's text response
 * - usage: Token usage information
 * - model: Model used
 * - finishReason: Why the response ended
 */
@Component
public class LlmChatExecutor implements NodeExecutor {
    private static final Logger log = LoggerFactory.getLogger(LlmChatExecutor.class);

    private static final Pattern INTERPOLATION_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    private static final Map<String, String> DEFAULT_BASE_URLS = Map.of(
            "openai", "https://api.openai.com/v1",
            "anthropic", "https://api.anthropic.com/v1",
            "ollama", "http://localhost:11434/api",
            "azure", "" // Requires custom configuration
    );

    private final HttpClient httpClient;
    private final SettingsServiceInterface settingsService;

    public LlmChatExecutor(SettingsServiceInterface settingsService) {
        this.settingsService = settingsService;
        int connectTimeout = settingsService.getInt(SettingsDefaults.HTTP_CONNECT_TIMEOUT, 30);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeout))
                .build();
    }

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> params = node.parameters();

        String provider = interpolate((String) params.getOrDefault("provider", "openai"), input);
        String model = interpolate((String) params.getOrDefault("model", getDefaultModel(provider)), input);
        String apiKey = interpolate((String) params.get("apiKey"), input);
        String baseUrl = interpolate(
                (String) params.getOrDefault("baseUrl", getDefaultBaseUrl(provider)), input);

        // Fall back to settings for API key if not specified in node params
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = getApiKeyFromSettings(provider);
        }
        String systemPrompt = interpolate((String) params.get("systemPrompt"), input);
        String prompt = interpolate((String) params.get("prompt"), input);
        double temperature = ((Number) params.getOrDefault("temperature", 0.7)).doubleValue();
        int maxTokens = ((Number) params.getOrDefault("maxTokens", 1024)).intValue();
        int timeout = ((Number) params.getOrDefault("timeout", 120)).intValue();
        String responseFormat = (String) params.getOrDefault("responseFormat", "text");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) params.get("messages");

        try {
            // Build messages list
            List<Map<String, String>> chatMessages = buildMessages(systemPrompt, prompt, messages, input);

            // Call the appropriate provider
            Map<String, Object> result = switch (provider.toLowerCase()) {
                case "anthropic" ->
                    callAnthropic(baseUrl, apiKey, model, chatMessages, temperature, maxTokens, timeout);
                case "ollama" -> callOllama(baseUrl, model, chatMessages, temperature, maxTokens, timeout);
                case "azure" ->
                    callAzure(baseUrl, apiKey, model, chatMessages, temperature, maxTokens, timeout, responseFormat);
                default ->
                    callOpenAI(baseUrl, apiKey, model, chatMessages, temperature, maxTokens, timeout, responseFormat);
            };

            Map<String, Object> output = new HashMap<>(input);
            output.putAll(result);
            output.put("provider", provider);
            output.put("model", model);
            output.put("success", true);

            return output;

        } catch (Exception e) {
            log.error("LLM chat failed: {}", e.getMessage(), e);
            Map<String, Object> output = new HashMap<>(input);
            output.put("success", false);
            output.put("error", e.getMessage());
            output.put("provider", provider);
            output.put("model", model);
            return output;
        }
    }

    private String getDefaultModel(String provider) {
        // Try to get model from settings first
        String settingsModel = switch (provider.toLowerCase()) {
            case "anthropic" -> settingsService.getValue(SettingsDefaults.AI_ANTHROPIC_DEFAULT_MODEL, null);
            case "ollama" -> settingsService.getValue(SettingsDefaults.AI_OLLAMA_DEFAULT_MODEL, null);
            case "azure" -> settingsService.getValue(SettingsDefaults.AI_AZURE_DEPLOYMENT, null);
            default -> settingsService.getValue(SettingsDefaults.AI_OPENAI_DEFAULT_MODEL, null);
        };

        if (settingsModel != null && !settingsModel.isBlank()) {
            return settingsModel;
        }

        // Fall back to hardcoded defaults
        return switch (provider.toLowerCase()) {
            case "anthropic" -> "claude-3-5-sonnet-20241022";
            case "ollama" -> "llama3.2";
            case "azure" -> "gpt-4";
            default -> "gpt-4o";
        };
    }

    /**
     * Gets the API key from settings based on provider.
     */
    private String getApiKeyFromSettings(String provider) {
        return switch (provider.toLowerCase()) {
            case "anthropic" -> settingsService.getValue(SettingsDefaults.AI_ANTHROPIC_API_KEY, null);
            case "azure" -> settingsService.getValue(SettingsDefaults.AI_AZURE_API_KEY, null);
            default -> settingsService.getValue(SettingsDefaults.AI_OPENAI_API_KEY, null);
        };
    }

    /**
     * Gets the default base URL for a provider, checking settings first.
     */
    private String getDefaultBaseUrl(String provider) {
        String settingsUrl = switch (provider.toLowerCase()) {
            case "anthropic" -> settingsService.getValue(SettingsDefaults.AI_ANTHROPIC_BASE_URL, null);
            case "ollama" -> settingsService.getValue(SettingsDefaults.AI_OLLAMA_BASE_URL, null);
            case "azure" -> settingsService.getValue(SettingsDefaults.AI_AZURE_ENDPOINT, null);
            default -> settingsService.getValue(SettingsDefaults.AI_OPENAI_BASE_URL, null);
        };

        if (settingsUrl != null && !settingsUrl.isBlank()) {
            return settingsUrl;
        }

        return DEFAULT_BASE_URLS.getOrDefault(provider, "");
    }

    private List<Map<String, String>> buildMessages(String systemPrompt, String prompt,
            List<Map<String, Object>> messages,
            Map<String, Object> input) {
        List<Map<String, String>> chatMessages = new ArrayList<>();

        // Add system prompt if provided
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            chatMessages.add(Map.of("role", "system", "content", systemPrompt));
        }

        // Add messages array if provided
        if (messages != null && !messages.isEmpty()) {
            for (Map<String, Object> msg : messages) {
                String role = (String) msg.getOrDefault("role", "user");
                String content = interpolate((String) msg.get("content"), input);
                if (content != null) {
                    chatMessages.add(Map.of("role", role, "content", content));
                }
            }
        }

        // Add single prompt if provided (and no messages)
        if (prompt != null && !prompt.isBlank() && (messages == null || messages.isEmpty())) {
            chatMessages.add(Map.of("role", "user", "content", prompt));
        }

        return chatMessages;
    }

    private Map<String, Object> callOpenAI(String baseUrl, String apiKey, String model,
            List<Map<String, String>> messages, double temperature,
            int maxTokens, int timeout, String responseFormat) throws Exception {
        String url = baseUrl + "/chat/completions";

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);

        if ("json".equals(responseFormat)) {
            requestBody.put("response_format", Map.of("type", "json_object"));
        }

        String jsonBody = toJson(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(timeout))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("OpenAI API error: " + response.statusCode() + " - " + response.body());
        }

        return parseOpenAIResponse(response.body());
    }

    private Map<String, Object> callAnthropic(String baseUrl, String apiKey, String model,
            List<Map<String, String>> messages, double temperature,
            int maxTokens, int timeout) throws Exception {
        String url = baseUrl + "/messages";

        // Anthropic requires system message separately
        String systemMessage = null;
        List<Map<String, String>> userMessages = new ArrayList<>();

        for (Map<String, String> msg : messages) {
            if ("system".equals(msg.get("role"))) {
                systemMessage = msg.get("content");
            } else {
                userMessages.add(msg);
            }
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", userMessages);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("temperature", temperature);

        if (systemMessage != null) {
            requestBody.put("system", systemMessage);
        }

        String jsonBody = toJson(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .timeout(Duration.ofSeconds(timeout))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Anthropic API error: " + response.statusCode() + " - " + response.body());
        }

        return parseAnthropicResponse(response.body());
    }

    private Map<String, Object> callOllama(String baseUrl, String model,
            List<Map<String, String>> messages, double temperature,
            int maxTokens, int timeout) throws Exception {
        String url = baseUrl + "/chat";

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("stream", false);
        requestBody.put("options", Map.of(
                "temperature", temperature,
                "num_predict", maxTokens));

        String jsonBody = toJson(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(timeout))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama API error: " + response.statusCode() + " - " + response.body());
        }

        return parseOllamaResponse(response.body());
    }

    private Map<String, Object> callAzure(String baseUrl, String apiKey, String model,
            List<Map<String, String>> messages, double temperature,
            int maxTokens, int timeout, String responseFormat) throws Exception {
        // Azure OpenAI uses a different URL pattern
        String url = baseUrl + "/openai/deployments/" + model + "/chat/completions?api-version=2024-02-15-preview";

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("messages", messages);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);

        if ("json".equals(responseFormat)) {
            requestBody.put("response_format", Map.of("type", "json_object"));
        }

        String jsonBody = toJson(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("api-key", apiKey)
                .timeout(Duration.ofSeconds(timeout))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Azure OpenAI API error: " + response.statusCode() + " - " + response.body());
        }

        return parseOpenAIResponse(response.body());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseOpenAIResponse(String json) {
        Map<String, Object> parsed = fromJson(json);
        Map<String, Object> result = new HashMap<>();

        List<Map<String, Object>> choices = (List<Map<String, Object>>) parsed.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> choice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) choice.get("message");

            result.put("response", message.get("content"));
            result.put("finishReason", choice.get("finish_reason"));
        }

        Map<String, Object> usage = (Map<String, Object>) parsed.get("usage");
        if (usage != null) {
            result.put("usage", Map.of(
                    "promptTokens", usage.get("prompt_tokens"),
                    "completionTokens", usage.get("completion_tokens"),
                    "totalTokens", usage.get("total_tokens")));
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAnthropicResponse(String json) {
        Map<String, Object> parsed = fromJson(json);
        Map<String, Object> result = new HashMap<>();

        List<Map<String, Object>> content = (List<Map<String, Object>>) parsed.get("content");
        if (content != null && !content.isEmpty()) {
            StringBuilder responseText = new StringBuilder();
            for (Map<String, Object> block : content) {
                if ("text".equals(block.get("type"))) {
                    responseText.append(block.get("text"));
                }
            }
            result.put("response", responseText.toString());
        }

        result.put("finishReason", parsed.get("stop_reason"));

        Map<String, Object> usage = (Map<String, Object>) parsed.get("usage");
        if (usage != null) {
            result.put("usage", Map.of(
                    "promptTokens", usage.get("input_tokens"),
                    "completionTokens", usage.get("output_tokens"),
                    "totalTokens", ((Number) usage.get("input_tokens")).intValue() +
                            ((Number) usage.get("output_tokens")).intValue()));
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseOllamaResponse(String json) {
        Map<String, Object> parsed = fromJson(json);
        Map<String, Object> result = new HashMap<>();

        Map<String, Object> message = (Map<String, Object>) parsed.get("message");
        if (message != null) {
            result.put("response", message.get("content"));
        }

        result.put("finishReason", parsed.get("done") != null && (Boolean) parsed.get("done") ? "stop" : "unknown");

        // Ollama provides different usage metrics
        if (parsed.containsKey("eval_count")) {
            result.put("usage", Map.of(
                    "promptTokens", parsed.getOrDefault("prompt_eval_count", 0),
                    "completionTokens", parsed.getOrDefault("eval_count", 0),
                    "totalTokens", ((Number) parsed.getOrDefault("prompt_eval_count", 0)).intValue() +
                            ((Number) parsed.getOrDefault("eval_count", 0)).intValue()));
        }

        return result;
    }

    private String interpolate(String text, Map<String, Object> data) {
        if (text == null) {
            return null;
        }

        Matcher matcher = INTERPOLATION_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String path = matcher.group(1);
            Object value = getNestedValue(data, path);
            String replacement = value != null ? value.toString() : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    @SuppressWarnings("unchecked")
    private Object getNestedValue(Map<String, Object> data, String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        String[] parts = path.split("\\.");
        Object current = data;

        for (String part : parts) {
            if (current instanceof Map<?, ?> map) {
                current = ((Map<String, Object>) map).get(part);
            } else {
                return null;
            }

            if (current == null) {
                return null;
            }
        }

        return current;
    }

    // Simple JSON serialization (production should use Jackson)
    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first)
                sb.append(",");
            first = false;

            sb.append("\"").append(entry.getKey()).append("\":");
            sb.append(valueToJson(entry.getValue()));
        }

        sb.append("}");
        return sb.toString();
    }

    private String valueToJson(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String s) {
            return "\"" + escapeJson(s) + "\"";
        } else if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : list) {
                if (!first)
                    sb.append(",");
                first = false;
                sb.append(valueToJson(item));
            }
            sb.append("]");
            return sb.toString();
        } else if (value instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> typedMap = (Map<String, Object>) map;
            return toJson(typedMap);
        }
        return "\"" + escapeJson(value.toString()) + "\"";
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // Simple JSON parsing (production should use Jackson)
    @SuppressWarnings("unchecked")
    private Map<String, Object> fromJson(String json) {
        // Use a simple approach - delegate to the Jackson ObjectMapper if available
        try {
            var objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON: " + e.getMessage(), e);
        }
    }

    @Override
    public String getNodeType() {
        return "llmChat";
    }
}
