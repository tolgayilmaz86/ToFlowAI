package io.toflowai.app.executor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import io.toflowai.app.service.ExecutionService;
import io.toflowai.app.service.NodeExecutor;
import io.toflowai.common.domain.Node;

/**
 * Filter node - filters items in an array based on conditions.
 * 
 * Parameters:
 * - inputField: Field containing the array to filter (default: "items")
 * - outputField: Field to store filtered results (default: "filtered")
 * - conditions: List of filter conditions
 * - field: Field path to check within each item
 * - operator: Comparison operator
 * - value: Expected value
 * - combineWith: "and" or "or" (default: "and")
 * - keepMatching: true to keep matching items, false to keep non-matching
 * (default: true)
 */
@Component
public class FilterExecutor implements NodeExecutor {

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> params = node.parameters();

        String inputField = (String) params.getOrDefault("inputField", "items");
        String outputField = (String) params.getOrDefault("outputField", "filtered");
        boolean keepMatching = (Boolean) params.getOrDefault("keepMatching", true);
        String combineWith = (String) params.getOrDefault("combineWith", "and");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) params.getOrDefault("conditions", List.of());

        // Get the input array
        Object inputData = getNestedValue(input, inputField);

        Map<String, Object> output = new HashMap<>(input);

        if (!(inputData instanceof List<?>)) {
            // If input is not a list, try to filter a single item
            if (inputData instanceof Map<?, ?> singleItem) {
                @SuppressWarnings("unchecked")
                Map<String, Object> item = (Map<String, Object>) singleItem;
                boolean matches = evaluateConditions(conditions, item, combineWith);

                if ((keepMatching && matches) || (!keepMatching && !matches)) {
                    output.put(outputField, List.of(item));
                    output.put("_filteredCount", 1);
                    output.put("_originalCount", 1);
                } else {
                    output.put(outputField, List.of());
                    output.put("_filteredCount", 0);
                    output.put("_originalCount", 1);
                }
            } else {
                output.put(outputField, List.of());
                output.put("_filteredCount", 0);
                output.put("_originalCount", 0);
                output.put("_error", "Input field '" + inputField + "' is not an array");
            }
            return output;
        }

        List<?> items = (List<?>) inputData;
        List<Object> filtered = new ArrayList<>();

        for (Object item : items) {
            if (item instanceof Map<?, ?> mapItem) {
                @SuppressWarnings("unchecked")
                Map<String, Object> itemMap = (Map<String, Object>) mapItem;
                boolean matches = evaluateConditions(conditions, itemMap, combineWith);

                if ((keepMatching && matches) || (!keepMatching && !matches)) {
                    filtered.add(item);
                }
            } else {
                // For non-map items, include them if keepMatching is true and no conditions
                if (conditions.isEmpty() && keepMatching) {
                    filtered.add(item);
                }
            }
        }

        output.put(outputField, filtered);
        output.put("_filteredCount", filtered.size());
        output.put("_originalCount", items.size());
        output.put("_removedCount", items.size() - filtered.size());

        return output;
    }

    private boolean evaluateConditions(List<Map<String, Object>> conditions, Map<String, Object> item,
            String combineWith) {
        if (conditions.isEmpty()) {
            return true;
        }

        boolean isAnd = "and".equalsIgnoreCase(combineWith);

        for (Map<String, Object> condition : conditions) {
            boolean result = evaluateCondition(condition, item);

            if (isAnd && !result) {
                return false;
            }
            if (!isAnd && result) {
                return true;
            }
        }

        return isAnd;
    }

    private boolean evaluateCondition(Map<String, Object> condition, Map<String, Object> item) {
        String field = (String) condition.get("field");
        String operator = (String) condition.getOrDefault("operator", "equals");
        Object expectedValue = condition.get("value");

        Object actualValue = getNestedValue(item, field);

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
            default -> true;
        };
    }

    @SuppressWarnings("unchecked")
    private Object getNestedValue(Object data, String path) {
        if (path == null || path.isEmpty() || data == null) {
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
        return "filter";
    }
}
