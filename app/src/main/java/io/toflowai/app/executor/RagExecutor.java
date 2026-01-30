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
 * RAG (Retrieval-Augmented Generation) node - retrieves relevant context and
 * generates a response.
 * 
 * This node combines:
 * 1. Embedding generation for the query
 * 2. Vector similarity search against provided documents
 * 3. LLM response generation with retrieved context
 * 
 * Parameters:
 * - provider: "openai", "anthropic", "ollama" (default: "openai")
 * - chatModel: Model for generation (e.g., "gpt-4o")
 * - embeddingModel: Model for embeddings (e.g., "text-embedding-3-small")
 * - apiKey: API key
 * - baseUrl: Base URL for custom providers
 * - query: The user's question/query
 * - documents: Array of documents with "content" and optional "embedding" and
 * "metadata"
 * - topK: Number of documents to retrieve (default: 3)
 * - systemPrompt: Custom system prompt (default: standard RAG prompt)
 * - temperature: LLM temperature (default: 0.7)
 * - maxTokens: Max response tokens (default: 1024)
 * - timeout: Request timeout in seconds (default: 120)
 * - includeContext: Whether to include retrieved context in output (default:
 * true)
 * 
 * Output:
 * - response: The LLM's generated response
 * - context: Retrieved documents used for context
 * - sources: Source metadata from retrieved documents
 */
@Component
public class RagExecutor implements NodeExecutor {
    private static final Logger log = LoggerFactory.getLogger(RagExecutor.class);

    private static final Pattern INTERPOLATION_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    private static final String DEFAULT_RAG_PROMPT = """
            You are a helpful assistant that answers questions based on the provided context.

            Use ONLY the information from the context below to answer the question.
            If the answer cannot be found in the context, say "I don't have enough information to answer that question."

            Context:
            {context}

            Please provide a clear and concise answer based on the context above.
            """;

    private final HttpClient httpClient;

    public RagExecutor() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> params = node.parameters();

        String provider = interpolate((String) params.getOrDefault("provider", "openai"), input);
        String chatModel = interpolate((String) params.getOrDefault("chatModel", getChatModel(provider)), input);
        String embeddingModel = interpolate((String) params.getOrDefault("embeddingModel", getEmbeddingModel(provider)),
                input);
        String apiKey = interpolate((String) params.get("apiKey"), input);
        String baseUrl = interpolate((String) params.getOrDefault("baseUrl", getDefaultBaseUrl(provider)), input);
        String query = interpolate((String) params.get("query"), input);
        String systemPrompt = interpolate((String) params.getOrDefault("systemPrompt", DEFAULT_RAG_PROMPT), input);
        int topK = ((Number) params.getOrDefault("topK", 3)).intValue();
        double temperature = ((Number) params.getOrDefault("temperature", 0.7)).doubleValue();
        int maxTokens = ((Number) params.getOrDefault("maxTokens", 1024)).intValue();
        int timeout = ((Number) params.getOrDefault("timeout", 120)).intValue();
        boolean includeContext = (Boolean) params.getOrDefault("includeContext", true);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents = (List<Map<String, Object>>) params.getOrDefault("documents", List.of());

        // Also check input for documents
        if (documents.isEmpty() && input.containsKey("documents")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> inputDocs = (List<Map<String, Object>>) input.get("documents");
            documents = inputDocs != null ? inputDocs : List.of();
        }

        if (query == null || query.isBlank()) {
            // Try to get from input
            query = (String) input.get("query");
        }

        if (query == null || query.isBlank()) {
            Map<String, Object> output = new HashMap<>(input);
            output.put("success", false);
            output.put("error", "Query is required");
            return output;
        }

        if (documents.isEmpty()) {
            Map<String, Object> output = new HashMap<>(input);
            output.put("success", false);
            output.put("error", "Documents are required for RAG");
            return output;
        }

        try {
            // Step 1: Generate embedding for the query
            List<Double> queryEmbedding = generateEmbedding(provider, baseUrl, apiKey,
                    embeddingModel, query, timeout);

            // Step 2: Generate embeddings for documents that don't have them
            List<Map<String, Object>> documentsWithEmbeddings = ensureEmbeddings(
                    documents, provider, baseUrl, apiKey, embeddingModel, timeout);

            // Step 3: Find top-K most similar documents
            List<Map<String, Object>> retrievedDocs = retrieveTopK(
                    queryEmbedding, documentsWithEmbeddings, topK);

            // Step 4: Build context from retrieved documents
            String contextText = buildContextText(retrievedDocs);
            String enhancedSystemPrompt = systemPrompt.replace("{context}", contextText);

            // Step 5: Generate response with LLM
            Map<String, Object> llmResult = callLlm(provider, baseUrl, apiKey, chatModel,
                    enhancedSystemPrompt, query, temperature, maxTokens, timeout);

            Map<String, Object> output = new HashMap<>(input);
            output.put("response", llmResult.get("response"));
            output.put("provider", provider);
            output.put("chatModel", chatModel);
            output.put("embeddingModel", embeddingModel);
            output.put("success", true);
            output.put("documentsRetrieved", retrievedDocs.size());

            if (includeContext) {
                output.put("context", retrievedDocs);

                // Extract sources
                List<Map<String, Object>> sources = new ArrayList<>();
                for (Map<String, Object> doc : retrievedDocs) {
                    if (doc.containsKey("metadata")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> metadata = (Map<String, Object>) doc.get("metadata");
                        sources.add(metadata);
                    }
                }
                if (!sources.isEmpty()) {
                    output.put("sources", sources);
                }
            }

            if (llmResult.containsKey("usage")) {
                output.put("usage", llmResult.get("usage"));
            }

            return output;

        } catch (Exception e) {
            log.error("RAG execution failed: {}", e.getMessage(), e);
            Map<String, Object> output = new HashMap<>(input);
            output.put("success", false);
            output.put("error", e.getMessage());
            return output;
        }
    }

    private String getChatModel(String provider) {
        return switch (provider.toLowerCase()) {
            case "anthropic" -> "claude-3-5-sonnet-20241022";
            case "ollama" -> "llama3.2";
            default -> "gpt-4o";
        };
    }

    private String getEmbeddingModel(String provider) {
        return switch (provider.toLowerCase()) {
            case "ollama" -> "nomic-embed-text";
            default -> "text-embedding-3-small";
        };
    }

    private String getDefaultBaseUrl(String provider) {
        return switch (provider.toLowerCase()) {
            case "anthropic" -> "https://api.anthropic.com/v1";
            case "ollama" -> "http://localhost:11434/api";
            default -> "https://api.openai.com/v1";
        };
    }

    private List<Double> generateEmbedding(String provider, String baseUrl, String apiKey,
            String model, String text, int timeout) throws Exception {
        if ("ollama".equalsIgnoreCase(provider)) {
            return generateOllamaEmbedding(baseUrl, model, text, timeout);
        } else {
            return generateOpenAIEmbedding(baseUrl, apiKey, model, text, timeout);
        }
    }

    private List<Double> generateOpenAIEmbedding(String baseUrl, String apiKey, String model,
            String text, int timeout) throws Exception {
        String url = baseUrl + "/embeddings";

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("input", text);

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
            throw new RuntimeException("OpenAI embedding error: " + response.statusCode());
        }

        Map<String, Object> parsed = fromJson(response.body());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) parsed.get("data");

        if (data != null && !data.isEmpty()) {
            @SuppressWarnings("unchecked")
            List<Double> embedding = (List<Double>) data.get(0).get("embedding");
            return embedding;
        }

        throw new RuntimeException("No embedding returned");
    }

    private List<Double> generateOllamaEmbedding(String baseUrl, String model,
            String text, int timeout) throws Exception {
        String url = baseUrl + "/embeddings";

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("prompt", text);

        String jsonBody = toJson(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(timeout))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama embedding error: " + response.statusCode());
        }

        Map<String, Object> parsed = fromJson(response.body());
        @SuppressWarnings("unchecked")
        List<Double> embedding = (List<Double>) parsed.get("embedding");

        return embedding;
    }

    private List<Map<String, Object>> ensureEmbeddings(List<Map<String, Object>> documents,
            String provider, String baseUrl,
            String apiKey, String model,
            int timeout) throws Exception {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> doc : documents) {
            Map<String, Object> docCopy = new HashMap<>(doc);

            if (!doc.containsKey("embedding") || doc.get("embedding") == null) {
                String content = (String) doc.get("content");
                if (content != null && !content.isBlank()) {
                    List<Double> embedding = generateEmbedding(provider, baseUrl, apiKey,
                            model, content, timeout);
                    docCopy.put("embedding", embedding);
                }
            }

            result.add(docCopy);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> retrieveTopK(List<Double> queryEmbedding,
            List<Map<String, Object>> documents,
            int topK) {
        // Calculate similarity scores
        List<Map.Entry<Map<String, Object>, Double>> scored = new ArrayList<>();

        for (Map<String, Object> doc : documents) {
            List<Double> docEmbedding = (List<Double>) doc.get("embedding");
            if (docEmbedding != null) {
                double similarity = cosineSimilarity(queryEmbedding, docEmbedding);
                scored.add(Map.entry(doc, similarity));
            }
        }

        // Sort by similarity (descending)
        scored.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // Return top K
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, scored.size()); i++) {
            Map<String, Object> doc = new HashMap<>(scored.get(i).getKey());
            doc.put("similarity", scored.get(i).getValue());
            // Remove embedding from output to save space
            doc.remove("embedding");
            result.add(doc);
        }

        return result;
    }

    private double cosineSimilarity(List<Double> a, List<Double> b) {
        if (a.size() != b.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.size(); i++) {
            dotProduct += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private String buildContextText(List<Map<String, Object>> documents) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < documents.size(); i++) {
            Map<String, Object> doc = documents.get(i);
            sb.append("--- Document ").append(i + 1).append(" ---\n");
            sb.append(doc.get("content")).append("\n\n");
        }

        return sb.toString();
    }

    private Map<String, Object> callLlm(String provider, String baseUrl, String apiKey,
            String model, String systemPrompt, String userMessage,
            double temperature, int maxTokens, int timeout) throws Exception {
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage));

        return switch (provider.toLowerCase()) {
            case "anthropic" -> callAnthropic(baseUrl, apiKey, model, messages, temperature, maxTokens, timeout);
            case "ollama" -> callOllama(baseUrl, model, messages, temperature, maxTokens, timeout);
            default -> callOpenAI(baseUrl, apiKey, model, messages, temperature, maxTokens, timeout);
        };
    }

    private Map<String, Object> callOpenAI(String baseUrl, String apiKey, String model,
            List<Map<String, String>> messages, double temperature,
            int maxTokens, int timeout) throws Exception {
        String url = baseUrl + "/chat/completions";

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);

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
            throw new RuntimeException("OpenAI API error: " + response.statusCode());
        }

        return parseOpenAIResponse(response.body());
    }

    private Map<String, Object> callAnthropic(String baseUrl, String apiKey, String model,
            List<Map<String, String>> messages, double temperature,
            int maxTokens, int timeout) throws Exception {
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
            throw new RuntimeException("Anthropic API error: " + response.statusCode());
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
            throw new RuntimeException("Ollama API error: " + response.statusCode());
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
        return "rag";
    }
}
