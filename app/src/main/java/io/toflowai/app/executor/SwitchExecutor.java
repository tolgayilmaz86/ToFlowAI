package io.toflowai.app.executor;

import io.toflowai.app.service.ExecutionService;
import io.toflowai.app.service.NodeExecutor;
import io.toflowai.common.domain.Node;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Switch node - routes execution to different branches based on conditions.
 * Similar to If node but supports multiple output branches.
 * 
 * Parameters:
 * - rules: List of condition rules, each with:
 * - name: Output branch name
 * - conditions: List of conditions to match
 * - combineWith: "and" or "or" (default: "and")
 * - fallbackOutput: Name of the output branch if no rules match (default:
 * "fallback")
 */
@Component
public class SwitchExecutor implements NodeExecutor {

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> params = node.parameters();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rules = (List<Map<String, Object>>) params.getOrDefault("rules", List.of());
        String fallbackOutput = (String) params.getOrDefault("fallbackOutput", "fallback");

        Map<String, Object> output = new HashMap<>(input);

        // Evaluate each rule in order
        String matchedBranch = null;
        int matchedRuleIndex = -1;

        for (int i = 0; i < rules.size(); i++) {
            Map<String, Object> rule = rules.get(i);
            String branchName = (String) rule.getOrDefault("name", "output" + i);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> conditions = (List<Map<String, Object>>) rule.getOrDefault("conditions",
                    List.of());
            String combineWith = (String) rule.getOrDefault("combineWith", "and");

            boolean ruleMatches = evaluateConditions(conditions, input, combineWith);

            if (ruleMatches) {
                matchedBranch = branchName;
                matchedRuleIndex = i;
                break;
            }
        }

        // Set output routing information
        if (matchedBranch != null) {
            output.put("_branch", matchedBranch);
            output.put("_matchedRuleIndex", matchedRuleIndex);
            output.put("_matched", true);
        } else {
            output.put("_branch", fallbackOutput);
            output.put("_matchedRuleIndex", -1);
            output.put("_matched", false);
        }

        return output;
    }

    private boolean evaluateConditions(List<Map<String, Object>> conditions, Map<String, Object> input,
            String combineWith) {
        if (conditions.isEmpty()) {
            return true;
        }

        boolean isAnd = "and".equalsIgnoreCase(combineWith);

        for (Map<String, Object> condition : conditions) {
            boolean conditionResult = evaluateCondition(condition, input);

            if (isAnd && !conditionResult) {
                return false;
            }
            if (!isAnd && conditionResult) {
                return true;
            }
        }

        return isAnd;
    }

    private boolean evaluateCondition(Map<String, Object> condition, Map<String, Object> input) {
        String field = (String) condition.get("field");
        String operator = (String) condition.getOrDefault("operator", "equals");
        Object expectedValue = condition.get("value");

        Object actualValue = getNestedValue(input, field);

        return switch (operator.toLowerCase()) {
            case "equals", "eq", "==" -> Objects.equals(actualValue, expectedValue);
            case "notequals", "neq", "!=" -> !Objects.equals(actualValue, expectedValue);
            case "contains" -> actualValue != null && actualValue.toString().contains(String.valueOf(expectedValue));
            case "notcontains" ->
                actualValue == null || !actualValue.toString().contains(String.valueOf(expectedValue));
            case "startswith" ->
                actualValue != null && actualValue.toString().startsWith(String.valueOf(expectedValue));
            case "endswith" -> actualValue != null && actualValue.toString().endsWith(String.valueOf(expectedValue));
            case "matches", "regex" ->
                actualValue != null && Pattern.matches(String.valueOf(expectedValue), actualValue.toString());
            case "gt", ">" -> compareNumbers(actualValue, expectedValue) > 0;
            case "gte", ">=" -> compareNumbers(actualValue, expectedValue) >= 0;
            case "lt", "<" -> compareNumbers(actualValue, expectedValue) < 0;
            case "lte", "<=" -> compareNumbers(actualValue, expectedValue) <= 0;
            case "isempty" -> actualValue == null || actualValue.toString().isEmpty();
            case "isnotempty" -> actualValue != null && !actualValue.toString().isEmpty();
            case "isnull" -> actualValue == null;
            case "isnotnull" -> actualValue != null;
            case "in" -> {
                if (expectedValue instanceof List<?> list) {
                    yield list.contains(actualValue);
                }
                yield false;
            }
            case "notin" -> {
                if (expectedValue instanceof List<?> list) {
                    yield !list.contains(actualValue);
                }
                yield true;
            }
            default -> false;
        };
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

    private int compareNumbers(Object actual, Object expected) {
        try {
            double actualNum = actual instanceof Number n ? n.doubleValue() : Double.parseDouble(actual.toString());
            double expectedNum = expected instanceof Number n ? n.doubleValue()
                    : Double.parseDouble(expected.toString());
            return Double.compare(actualNum, expectedNum);
        } catch (NumberFormatException | NullPointerException e) {
            return 0;
        }
    }

    @Override
    public String getNodeType() {
        return "switch";
    }
}
