package io.toflowai.app.executor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import io.toflowai.app.service.ExecutionService;
import io.toflowai.app.service.NodeExecutor;
import io.toflowai.common.domain.Node;

/**
 * Webhook trigger node - starts workflow execution when an HTTP request is
 * received.
 * The actual HTTP endpoint is registered by WebhookService.
 * This executor is called when a webhook request arrives.
 */
@Component
public class WebhookTriggerExecutor implements NodeExecutor {

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> params = node.parameters();

        Map<String, Object> output = new HashMap<>();
        output.put("triggeredAt", LocalDateTime.now().toString());
        output.put("triggerType", "webhook");

        // Include webhook configuration
        String path = (String) params.getOrDefault("path", "/webhook");
        String method = (String) params.getOrDefault("method", "POST");
        String authentication = (String) params.getOrDefault("authentication", "none");

        output.put("webhookPath", path);
        output.put("webhookMethod", method);
        output.put("authentication", authentication);

        // Include incoming request data from context
        Map<String, Object> contextInput = context.getInput();

        // HTTP request data passed from WebhookController
        output.put("body", contextInput.getOrDefault("body", Map.of()));
        output.put("headers", contextInput.getOrDefault("headers", Map.of()));
        output.put("queryParams", contextInput.getOrDefault("queryParams", Map.of()));
        output.put("requestMethod", contextInput.getOrDefault("requestMethod", method));
        output.put("requestPath", contextInput.getOrDefault("requestPath", path));
        output.put("remoteAddress", contextInput.getOrDefault("remoteAddress", "unknown"));

        return output;
    }

    @Override
    public String getNodeType() {
        return "webhookTrigger";
    }
}
