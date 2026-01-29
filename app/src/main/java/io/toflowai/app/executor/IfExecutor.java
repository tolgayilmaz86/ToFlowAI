package io.toflowai.app.executor;

import io.toflowai.app.service.ExecutionService;
import io.toflowai.app.service.NodeExecutor;
import io.toflowai.common.domain.Node;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * If node executor - conditional branching.
 */
@Component
public class IfExecutor implements NodeExecutor {

    private final ExpressionParser parser = new SpelExpressionParser();

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input, ExecutionService.ExecutionContext context) {
        Map<String, Object> params = node.parameters();
        
        String condition = (String) params.getOrDefault("condition", "true");
        
        // Evaluate condition using Spring Expression Language
        StandardEvaluationContext evalContext = new StandardEvaluationContext();
        evalContext.setVariables(input);
        
        boolean result;
        try {
            result = Boolean.TRUE.equals(parser.parseExpression(condition).getValue(evalContext, Boolean.class));
        } catch (Exception e) {
            // Default to false on evaluation error
            result = false;
        }
        
        Map<String, Object> output = new HashMap<>(input);
        output.put("conditionResult", result);
        output.put("branch", result ? "true" : "false");
        
        return output;
    }

    @Override
    public String getNodeType() {
        return "if";
    }
}
