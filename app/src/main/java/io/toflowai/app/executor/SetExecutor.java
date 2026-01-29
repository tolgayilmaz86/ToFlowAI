package io.toflowai.app.executor;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import io.toflowai.app.service.ExecutionService;
import io.toflowai.app.service.NodeExecutor;
import io.toflowai.common.domain.Node;

/**
 * Set node executor - sets or transforms data values.
 */
@Component
public class SetExecutor implements NodeExecutor {

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> output = new HashMap<>(input);

        Map<String, Object> values = (Map<String, Object>) node.parameters().getOrDefault("values", Map.of());
        boolean keepOnlySet = (boolean) node.parameters().getOrDefault("keepOnlySet", false);

        if (keepOnlySet) {
            output = new HashMap<>();
        }

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Support simple expression syntax: $input.fieldName
            if (value instanceof String strValue && strValue.startsWith("$input.")) {
                String fieldPath = strValue.substring(7);
                value = getNestedValue(input, fieldPath);
            }

            output.put(key, value);
        }

        return output;
    }

    private Object getNestedValue(Map<String, Object> data, String path) {
        String[] parts = path.split("\\.");
        Object current = data;

        for (String part : parts) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
            } else {
                return null;
            }
        }

        return current;
    }

    @Override
    public String getNodeType() {
        return "set";
    }
}
