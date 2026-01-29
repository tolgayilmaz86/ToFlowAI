package io.toflowai.app.executor;

import io.toflowai.app.service.ExecutionService;
import io.toflowai.app.service.NodeExecutor;
import io.toflowai.common.domain.Node;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Schedule trigger node - starts workflow execution on a schedule.
 * The actual scheduling is handled by SchedulerService.
 * This executor is called when the scheduled time arrives.
 */
@Component
public class ScheduleTriggerExecutor implements NodeExecutor {

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> params = node.parameters();

        Map<String, Object> output = new HashMap<>(input);
        output.put("triggeredAt", LocalDateTime.now().toString());
        output.put("triggerType", "schedule");

        // Include schedule info in output
        String cronExpression = (String) params.getOrDefault("cronExpression", "");
        String timezone = (String) params.getOrDefault("timezone", "UTC");

        output.put("cronExpression", cronExpression);
        output.put("timezone", timezone);
        output.put("scheduledTime", context.getInput().getOrDefault("scheduledTime", LocalDateTime.now().toString()));

        return output;
    }

    @Override
    public String getNodeType() {
        return "scheduleTrigger";
    }
}
