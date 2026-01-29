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
import io.toflowai.common.domain.Node;

/**
 * Text Classifier node - classifies text into categories using LLM.
 * 
 * Parameters:
 * - provider: "openai", "anthropic", "ollama" (default: "openai")
 * - model: Model name
 * - apiKey: API key
 * - baseUrl: Base URL for custom providers
 * - text: Text to classify (supports ${interpolation})
 * - categories: List of category names to classify into
 * - multiLabel: Whether to allow multiple labels (default: false)
 * - includeConfidence: Whether to include confidence scores (default: true)
 * - timeout: Request timeout in seconds (default: 60)
 * 
 * Output:
 * - category: The predicted category (or list if multiLabel)
 * - confidence: Confidence score(s)
 * - allScores: Map of all categories with scores
 */
@Component
public class TextClassifierExecutor implements NodeExecutor {
    private static final Logger log = LoggerFactory.getLogger(TextClassifierExecutor.class);

    private static final Pattern INTERPOLATION_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    private final HttpClient httpClient;

    public TextClassifierExecutor() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> params = node.parameters();

        String provider = interpolate((String) params.getOrDefault("provider", "openai"), input);
        String model = interpolate((String) params.getOrDefault("model", getDefaultModel(provider)), input);
        String apiKey = interpolate((String) params.get("apiKey"), input);
        String baseUrl = interpolate((String) params.getOrDefault("baseUrl", getDefaultBaseUrl(provider)), input);
        String text = interpolate((String) params.get("text"), input);
        boolean multiLabel = (Boolean) params.getOrDefault("multiLabel", false);
        boolean includeConfidence = (Boolean) params.getOrDefault("includeConfidence", true);
        int timeout = ((Number) params.getOrDefault("timeout", 60)).intValue();

        @SuppressWarnings("unchecked")
        List<String> categories = (List<String>) params.getOrDefault("categories", List.of());

        if (text == null || text.isBlank()) {
            Map<String, Object> output = new HashMap<>(input);
            output.put("success", false);
            output.put("error", "Text is required for classification");
            return output;
        }

        if (categories.isEmpty()) {
            Map<String, Object> output = new HashMap<>(input);
            output.put("success", false);
            output.put("error", "Categories list is required");
            return output;
        }

        try {
            // Build the classification prompt
            String systemPrompt = buildClassificationPrompt(categories, multiLabel, includeConfidence);
            String userPrompt = "Classify the following text:\n\n" + text;

            // Call LLM
            Map<String, Object> llmResult = callLlm(provider, baseUrl, apiKey, model,
                    systemPrompt, userPrompt, timeout);

            // Parse the classification result
            Map<String, Object> classification = parseClassificationResult(
                    (String) llmResult.get("response"), categories, multiLabel);

            Map<String, Object> output = new HashMap<>(input);
            output.putAll(classification);
            output.put("inputText", text);
            output.put("provider", provider);
            output.put("model", model);
            output.put("success", true);

            if (llmResult.containsKey("usage")) {
                output.put("usage", llmResult.get("usage"));
            }

            return output;

        } catch (Exception e) {
            log.error("Text classification failed: {}", e.getMessage(), e);
            Map<String, Object> output = new HashMap<>(input);
            output.put("success", false);
            output.put("error", e.getMessage());
            return output;
        }
    }

    private String getDefaultModel(String provider) {
        return switch (provider.toLowerCase()) {
            case "anthropic" -> "claude-3-5-sonnet-20241022";
            case "ollama" -> "llama3.2";
            default -> "gpt-4o-mini";
        };
    }

    private String getDefaultBaseUrl(String provider) {
        return switch (provider.toLowerCase()) {
            case "anthropic" -> "https://api.anthropic.com/v1";
            case "ollama" -> "http://localhost:11434/api";
            default -> "https://api.openai.com/v1";
        };
    }

    private String buildClassificationPrompt(List<String> categories, boolean multiLabel, boolean includeConfidence) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a text classification assistant. ");

        if (multiLabel) {
            sb.append("Classify the given text into one or more of the following categories. ");
        } else {
            sb.append("Classify the given text into exactly one of the following categories. ");
        }

        sb.append("Categories: ").append(String.join(", ", categories)).append("\n\n");

        sb.append("Respond in JSON format:\n");
        if (multiLabel) {
            if (includeConfidence) {
                sb.append("{\"categories\": [{\"name\": \"category_name\", \"confidence\": 0.95}]}");
            } else {
                sb.append("{\"categories\": [\"category1\", \"category2\"]}");
            }
        } else {
            if (includeConfidence) {
                sb.append("{\"category\": \"category_name\", \"confidence\": 0.95, " +
                        "\"allScores\": {\"cat1\": 0.95, \"cat2\": 0.05}}");
            } else {
                sb.append("{\"category\": \"category_name\"}");
            }
        }

        sb.append("\n\nOnly use the provided categories. Confidence should be a number between 0 and 1.");

        return sb.toString();
    }

    private Map<String, Object> callLlm(String provider, String baseUrl, String apiKey,
            String model, String systemPrompt, String userPrompt,
            int timeout) throws Exception {
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt));

        return switch (provider.toLowerCase()) {
            case "anthropic" -> callAnthropic(baseUrl, apiKey, model, messages, timeout);
            case "ollama" -> callOllama(baseUrl, model, messages, timeout);
            default -> callOpenAI(baseUrl, apiKey, model, messages, timeout);
        };
    }

    private Map<String, Object> callOpenAI(String baseUrl, String apiKey, String model,
            List<Map<String, String>> messages, int timeout) throws Exception {
        String url = baseUrl + "/chat/completions";

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.1); // Low temperature for classification
        requestBody.put("response_format", Map.of("type", "json_object"));

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
            List<Map<String, String>> messages, int timeout) throws Exception {
        String url = baseUrl + "/messages";

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
        requestBody.put("max_tokens", 1024);
        requestBody.put("temperature", 0.1);

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
            List<Map<String, String>> messages, int timeout) throws Exception {
        String url = baseUrl + "/chat";

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("stream", false);
        requestBody.put("format", "json");
        requestBody.put("options", Map.of("temperature", 0.1));

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

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseOpenAIResponse(String json) {
        Map<String, Object> parsed = fromJson(json);
        Map<String, Object> result = new HashMap<>();

        List<Map<String, Object>> choices = (List<Map<String, Object>>) parsed.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> choice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) choice.get("message");
            result.put("response", message.get("content"));
        }

        if (parsed.containsKey("usage")) {
            result.put("usage", parsed.get("usage"));
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAnthropicResponse(String json) {
        Map<String, Object> parsed = fromJson(json);
        Map<String, Object> result = new HashMap<>();

        List<Map<String, Object>> content = (List<Map<String, Object>>) parsed.get("content");
        if (content != null && !content.isEmpty()) {
            for (Map<String, Object> block : content) {
                if ("text".equals(block.get("type"))) {
                    result.put("response", block.get("text"));
                    break;
                }
            }
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

        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseClassificationResult(String llmResponse,
            List<String> validCategories,
            boolean multiLabel) {
        Map<String, Object> result = new HashMap<>();

        try {
            Map<String, Object> parsed = fromJson(llmResponse);

            if (multiLabel) {
                List<Map<String, Object>> categories = (List<Map<String, Object>>) parsed.get("categories");
                if (categories != null) {
                    List<String> categoryNames = new ArrayList<>();
                    List<Double> confidences = new ArrayList<>();

                    for (Object cat : categories) {
                        if (cat instanceof String s) {
                            categoryNames.add(s);
                        } else if (cat instanceof Map<?, ?> catMap) {
                            categoryNames.add((String) ((Map<String, Object>) catMap).get("name"));
                            Object conf = ((Map<String, Object>) catMap).get("confidence");
                            if (conf instanceof Number n) {
                                confidences.add(n.doubleValue());
                            }
                        }
                    }

                    result.put("categories", categoryNames);
                    if (!confidences.isEmpty()) {
                        result.put("confidences", confidences);
                    }
                }
            } else {
                String category = (String) parsed.get("category");
                result.put("category", category);

                Object confidence = parsed.get("confidence");
                if (confidence instanceof Number n) {
                    result.put("confidence", n.doubleValue());
                }

                Object allScores = parsed.get("allScores");
                if (allScores != null) {
                    result.put("allScores", allScores);
                }
            }

        } catch (Exception e) {
            log.warn("Failed to parse classification response: {}", e.getMessage());
            result.put("rawResponse", llmResponse);
        }

        return result;
    }

    private String interpolate(String text, Map<String, Object> data) {
        if (text == null)
            return null;

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
        if (path == null || path.isEmpty())
            return null;

        String[] parts = path.split("\\.");
        Object current = data;

        for (String part : parts) {
            if (current instanceof Map<?, ?> map) {
                current = ((Map<String, Object>) map).get(part);
            } else {
                return null;
            }
            if (current == null)
                return null;
        }

        return current;
    }

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
        if (value == null)
            return "null";
        if (value instanceof String s)
            return "\"" + escapeJson(s) + "\"";
        if (value instanceof Number || value instanceof Boolean)
            return value.toString();
        if (value instanceof List<?> list) {
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
        }
        if (value instanceof Map<?, ?> map) {
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> fromJson(String json) {
        try {
            var objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON: " + e.getMessage(), e);
        }
    }

    @Override
    public String getNodeType() {
        return "textClassifier";
    }
}
