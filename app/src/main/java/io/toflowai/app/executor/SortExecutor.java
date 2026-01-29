package io.toflowai.app.executor;

import io.toflowai.app.service.ExecutionService;
import io.toflowai.app.service.NodeExecutor;
import io.toflowai.common.domain.Node;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Sort node - sorts items in an array.
 * 
 * Parameters:
 * - inputField: Field containing the array to sort (default: "items")
 * - outputField: Field to store sorted results (default: "sorted")
 * - sortBy: Field path to sort by within each item (default: sorts by item
 * value)
 * - direction: "asc" or "desc" (default: "asc")
 * - sortType: "string", "number", "date" (default: auto-detect)
 * - nullsFirst: true to put nulls first, false for last (default: false)
 */
@Component
public class SortExecutor implements NodeExecutor {

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> params = node.parameters();

        String inputField = (String) params.getOrDefault("inputField", "items");
        String outputField = (String) params.getOrDefault("outputField", "sorted");
        String sortBy = (String) params.get("sortBy");
        String direction = (String) params.getOrDefault("direction", "asc");
        String sortType = (String) params.get("sortType"); // null means auto-detect
        boolean nullsFirst = (Boolean) params.getOrDefault("nullsFirst", false);

        // Get the input array
        Object inputData = getNestedValue(input, inputField);

        Map<String, Object> output = new HashMap<>(input);

        if (!(inputData instanceof List<?>)) {
            output.put(outputField, List.of());
            output.put("_sortedCount", 0);
            output.put("_error", "Input field '" + inputField + "' is not an array");
            return output;
        }

        List<?> items = (List<?>) inputData;
        List<Object> sorted = new ArrayList<>(items);

        boolean ascending = "asc".equalsIgnoreCase(direction);

        Comparator<Object> comparator = createComparator(sortBy, sortType, ascending, nullsFirst);
        sorted.sort(comparator);

        output.put(outputField, sorted);
        output.put("_sortedCount", sorted.size());
        output.put("_sortedBy", sortBy != null ? sortBy : "value");
        output.put("_sortDirection", direction);

        return output;
    }

    private Comparator<Object> createComparator(String sortBy, String sortType, boolean ascending, boolean nullsFirst) {
        Comparator<Object> comparator = (a, b) -> {
            Object valueA = sortBy != null ? getNestedValue(a, sortBy) : a;
            Object valueB = sortBy != null ? getNestedValue(b, sortBy) : b;

            // Handle nulls
            if (valueA == null && valueB == null)
                return 0;
            if (valueA == null)
                return nullsFirst ? -1 : 1;
            if (valueB == null)
                return nullsFirst ? 1 : -1;

            // Determine sort type
            String effectiveType = sortType;
            if (effectiveType == null) {
                effectiveType = detectType(valueA, valueB);
            }

            return switch (effectiveType.toLowerCase()) {
                case "number" -> compareAsNumbers(valueA, valueB);
                case "date" -> compareAsDates(valueA, valueB);
                default -> compareAsStrings(valueA, valueB);
            };
        };

        return ascending ? comparator : comparator.reversed();
    }

    private String detectType(Object a, Object b) {
        if (a instanceof Number || b instanceof Number) {
            return "number";
        }

        String strA = a.toString();
        String strB = b.toString();

        // Check if both look like numbers
        if (looksLikeNumber(strA) && looksLikeNumber(strB)) {
            return "number";
        }

        // Check if both look like dates (ISO format)
        if (looksLikeDate(strA) && looksLikeDate(strB)) {
            return "date";
        }

        return "string";
    }

    private boolean looksLikeNumber(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean looksLikeDate(String s) {
        // Check for ISO date format: YYYY-MM-DD or YYYY-MM-DDTHH:MM:SS
        return s.matches("\\d{4}-\\d{2}-\\d{2}.*");
    }

    private int compareAsNumbers(Object a, Object b) {
        try {
            double numA = a instanceof Number n ? n.doubleValue() : Double.parseDouble(a.toString());
            double numB = b instanceof Number n ? n.doubleValue() : Double.parseDouble(b.toString());
            return Double.compare(numA, numB);
        } catch (NumberFormatException e) {
            return compareAsStrings(a, b);
        }
    }

    private int compareAsDates(Object a, Object b) {
        // For dates in ISO format, string comparison works
        return a.toString().compareTo(b.toString());
    }

    private int compareAsStrings(Object a, Object b) {
        return a.toString().compareToIgnoreCase(b.toString());
    }

    @SuppressWarnings("unchecked")
    private Object getNestedValue(Object data, String path) {
        if (path == null || path.isEmpty() || data == null) {
            return data;
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

    @Override
    public String getNodeType() {
        return "sort";
    }
}
