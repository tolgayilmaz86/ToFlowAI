package io.toflowai.common.expression;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expression evaluator for dynamic workflow values.
 * Supports variable substitution and built-in functions.
 */
public class ExpressionEvaluator {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("(\\w+)\\(([^)]*)\\)");

    private final Map<String, Object> variables = new HashMap<>();

    public ExpressionEvaluator() {
    }

    public ExpressionEvaluator(Map<String, Object> variables) {
        this.variables.putAll(variables);
    }

    /**
     * Set a variable value.
     */
    public void setVariable(String name, Object value) {
        variables.put(name, value);
    }

    /**
     * Set multiple variables.
     */
    public void setVariables(Map<String, Object> vars) {
        variables.putAll(vars);
    }

    /**
     * Evaluate an expression and return the result as a string.
     */
    public String evaluate(String expression) {
        if (expression == null || expression.isBlank()) {
            return expression;
        }

        // First, substitute variables
        String result = substituteVariables(expression);

        // Then, evaluate functions
        result = evaluateFunctions(result);

        return result;
    }

    /**
     * Evaluate an expression and return the result as an object.
     */
    public Object evaluateToObject(String expression) {
        String result = evaluate(expression);

        // Try to parse as number
        try {
            if (result.contains(".")) {
                return Double.parseDouble(result);
            }
            return Long.parseLong(result);
        } catch (NumberFormatException ignored) {
        }

        // Try to parse as boolean
        if ("true".equalsIgnoreCase(result))
            return true;
        if ("false".equalsIgnoreCase(result))
            return false;

        return result;
    }

    private String substituteVariables(String expression) {
        Matcher matcher = VARIABLE_PATTERN.matcher(expression);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String varName = matcher.group(1).trim();
            Object value = variables.get(varName);
            String replacement = value != null ? value.toString() : matcher.group(0);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private String evaluateFunctions(String expression) {
        String result = expression;
        Matcher matcher = FUNCTION_PATTERN.matcher(result);

        while (matcher.find()) {
            String funcName = matcher.group(1);
            String argsStr = matcher.group(2);
            List<String> args = parseArguments(argsStr);

            String funcResult = executeFunction(funcName, args);
            result = result.replace(matcher.group(0), funcResult);
            matcher = FUNCTION_PATTERN.matcher(result);
        }

        return result;
    }

    private List<String> parseArguments(String argsStr) {
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenDepth = 0;
        boolean inString = false;
        char stringChar = 0;

        for (int i = 0; i < argsStr.length(); i++) {
            char c = argsStr.charAt(i);

            if ((c == '"' || c == '\'') && (i == 0 || argsStr.charAt(i - 1) != '\\')) {
                if (!inString) {
                    inString = true;
                    stringChar = c;
                } else if (c == stringChar) {
                    inString = false;
                }
                current.append(c);
            } else if (c == '(' && !inString) {
                parenDepth++;
                current.append(c);
            } else if (c == ')' && !inString) {
                parenDepth--;
                current.append(c);
            } else if (c == ',' && parenDepth == 0 && !inString) {
                args.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        String last = current.toString().trim();
        if (!last.isEmpty()) {
            args.add(last);
        }

        return args;
    }

    private String executeFunction(String name, List<String> args) {
        return switch (name.toLowerCase()) {
            // Conditionals
            case "if" -> evaluateIf(args);
            case "and" -> evaluateAnd(args);
            case "or" -> evaluateOr(args);
            case "not" -> evaluateNot(args);

            // Comparisons
            case "eq" -> evaluateEquals(args);
            case "ne" -> evaluateNotEquals(args);
            case "gt" -> evaluateGreaterThan(args);
            case "lt" -> evaluateLessThan(args);
            case "gte" -> evaluateGreaterThanOrEqual(args);
            case "lte" -> evaluateLessThanOrEqual(args);

            // String functions
            case "contains" -> evaluateContains(args);
            case "startswith" -> evaluateStartsWith(args);
            case "endswith" -> evaluateEndsWith(args);
            case "length" -> evaluateLength(args);
            case "trim" -> evaluateTrim(args);
            case "upper" -> evaluateUpper(args);
            case "lower" -> evaluateLower(args);
            case "concat" -> evaluateConcat(args);
            case "substring" -> evaluateSubstring(args);
            case "replace" -> evaluateReplace(args);
            case "split" -> evaluateSplit(args);
            case "join" -> evaluateJoin(args);

            // Date functions
            case "now" -> evaluateNow();
            case "format" -> evaluateFormat(args);

            // Type conversion
            case "tonumber" -> evaluateToNumber(args);
            case "tostring" -> args.isEmpty() ? "" : stripQuotes(args.getFirst());
            case "toboolean" -> evaluateToBoolean(args);

            default -> name + "(" + String.join(", ", args) + ")";
        };
    }

    // Conditional functions

    private String evaluateIf(List<String> args) {
        if (args.size() < 3)
            return "";
        boolean condition = toBoolean(args.get(0));
        return condition ? stripQuotes(args.get(1)) : stripQuotes(args.get(2));
    }

    private String evaluateAnd(List<String> args) {
        return String.valueOf(args.stream().allMatch(this::toBoolean));
    }

    private String evaluateOr(List<String> args) {
        return String.valueOf(args.stream().anyMatch(this::toBoolean));
    }

    private String evaluateNot(List<String> args) {
        return String.valueOf(!toBoolean(args.isEmpty() ? "" : args.getFirst()));
    }

    // Comparison functions

    private String evaluateEquals(List<String> args) {
        if (args.size() < 2)
            return "false";
        return String.valueOf(stripQuotes(args.get(0)).equals(stripQuotes(args.get(1))));
    }

    private String evaluateNotEquals(List<String> args) {
        if (args.size() < 2)
            return "true";
        return String.valueOf(!stripQuotes(args.get(0)).equals(stripQuotes(args.get(1))));
    }

    private String evaluateGreaterThan(List<String> args) {
        if (args.size() < 2)
            return "false";
        try {
            double a = Double.parseDouble(args.get(0));
            double b = Double.parseDouble(args.get(1));
            return String.valueOf(a > b);
        } catch (NumberFormatException e) {
            return "false";
        }
    }

    private String evaluateLessThan(List<String> args) {
        if (args.size() < 2)
            return "false";
        try {
            double a = Double.parseDouble(args.get(0));
            double b = Double.parseDouble(args.get(1));
            return String.valueOf(a < b);
        } catch (NumberFormatException e) {
            return "false";
        }
    }

    private String evaluateGreaterThanOrEqual(List<String> args) {
        if (args.size() < 2)
            return "false";
        try {
            double a = Double.parseDouble(args.get(0));
            double b = Double.parseDouble(args.get(1));
            return String.valueOf(a >= b);
        } catch (NumberFormatException e) {
            return "false";
        }
    }

    private String evaluateLessThanOrEqual(List<String> args) {
        if (args.size() < 2)
            return "false";
        try {
            double a = Double.parseDouble(args.get(0));
            double b = Double.parseDouble(args.get(1));
            return String.valueOf(a <= b);
        } catch (NumberFormatException e) {
            return "false";
        }
    }

    // String functions

    private String evaluateContains(List<String> args) {
        if (args.size() < 2)
            return "false";
        return String.valueOf(stripQuotes(args.get(0)).contains(stripQuotes(args.get(1))));
    }

    private String evaluateStartsWith(List<String> args) {
        if (args.size() < 2)
            return "false";
        return String.valueOf(stripQuotes(args.get(0)).startsWith(stripQuotes(args.get(1))));
    }

    private String evaluateEndsWith(List<String> args) {
        if (args.size() < 2)
            return "false";
        return String.valueOf(stripQuotes(args.get(0)).endsWith(stripQuotes(args.get(1))));
    }

    private String evaluateLength(List<String> args) {
        return String.valueOf(args.isEmpty() ? 0 : stripQuotes(args.getFirst()).length());
    }

    private String evaluateTrim(List<String> args) {
        return args.isEmpty() ? "" : stripQuotes(args.getFirst()).trim();
    }

    private String evaluateUpper(List<String> args) {
        return args.isEmpty() ? "" : stripQuotes(args.getFirst()).toUpperCase();
    }

    private String evaluateLower(List<String> args) {
        return args.isEmpty() ? "" : stripQuotes(args.getFirst()).toLowerCase();
    }

    private String evaluateConcat(List<String> args) {
        return args.stream()
                .map(this::stripQuotes)
                .reduce("", (a, b) -> a + b);
    }

    private String evaluateSubstring(List<String> args) {
        if (args.isEmpty())
            return "";
        String str = stripQuotes(args.getFirst());
        int start = args.size() > 1 ? Integer.parseInt(args.get(1).trim()) : 0;
        int end = args.size() > 2 ? Integer.parseInt(args.get(2).trim()) : str.length();
        return str.substring(Math.max(0, start), Math.min(str.length(), end));
    }

    private String evaluateReplace(List<String> args) {
        if (args.size() < 3)
            return args.isEmpty() ? "" : stripQuotes(args.getFirst());
        return stripQuotes(args.get(0))
                .replace(stripQuotes(args.get(1)), stripQuotes(args.get(2)));
    }

    private String evaluateSplit(List<String> args) {
        if (args.size() < 2)
            return "[]";
        String[] parts = stripQuotes(args.get(0)).split(Pattern.quote(stripQuotes(args.get(1))));
        return "[" + String.join(", ", parts) + "]";
    }

    private String evaluateJoin(List<String> args) {
        if (args.size() < 2)
            return "";
        String input = stripQuotes(args.get(0));
        String delimiter = stripQuotes(args.get(1));
        // Remove array brackets if present
        if (input.startsWith("[") && input.endsWith("]")) {
            input = input.substring(1, input.length() - 1);
        }
        String[] parts = input.split(",\\s*");
        return String.join(delimiter, parts);
    }

    // Date functions

    private String evaluateNow() {
        return Instant.now().toString();
    }

    private String evaluateFormat(List<String> args) {
        if (args.size() < 2)
            return "";
        try {
            Instant instant = Instant.parse(stripQuotes(args.get(0)));
            String pattern = stripQuotes(args.get(1));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern)
                    .withZone(ZoneId.systemDefault());
            return formatter.format(instant);
        } catch (Exception e) {
            return args.get(0);
        }
    }

    // Type conversion

    private String evaluateToNumber(List<String> args) {
        if (args.isEmpty())
            return "0";
        try {
            String value = stripQuotes(args.getFirst());
            if (value.contains(".")) {
                return String.valueOf(Double.parseDouble(value));
            }
            return String.valueOf(Long.parseLong(value));
        } catch (NumberFormatException e) {
            return "0";
        }
    }

    private String evaluateToBoolean(List<String> args) {
        return String.valueOf(toBoolean(args.isEmpty() ? "" : args.getFirst()));
    }

    // Helpers

    private String stripQuotes(String value) {
        if (value == null)
            return "";
        value = value.trim();
        if ((value.startsWith("\"") && value.endsWith("\"")) ||
                (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private boolean toBoolean(String value) {
        String v = stripQuotes(value).toLowerCase();
        return "true".equals(v) || "1".equals(v) || "yes".equals(v);
    }
}
