package io.toflowai.app.executor;

import io.toflowai.app.service.ExecutionService;
import io.toflowai.app.service.NodeExecutor;
import io.toflowai.common.domain.Node;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Manual trigger node - starts workflow execution manually.
 */
@Component
public class ManualTriggerExecutor implements NodeExecutor {

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input, ExecutionService.ExecutionContext context) {
        Map<String, Object> output = new HashMap<>(input);
        output.put("triggeredAt", LocalDateTime.now().toString());
        output.put("triggerType", "manual");
        return output;
    }

    @Override
    public String getNodeType() {
        return "manualTrigger";
    }
}
