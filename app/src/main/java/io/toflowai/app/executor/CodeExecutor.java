package io.toflowai.app.executor;

import io.toflowai.app.service.ExecutionService;
import io.toflowai.app.service.NodeExecutor;
import io.toflowai.common.domain.Node;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Code node executor - executes JavaScript or Python code.
 * Uses GraalVM Polyglot for multi-language support.
 */
@Component
public class CodeExecutor implements NodeExecutor {

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input, ExecutionService.ExecutionContext context) {
        String code = (String) node.parameters().getOrDefault("code", "");
        String language = (String) node.parameters().getOrDefault("language", "js");

        if (code.isBlank()) {
            return input;
        }

        String graalLanguage = switch (language) {
            case "js", "javascript" -> "js";
            case "py", "python" -> "python";
            default -> "js";
        };

        try (Context polyglotContext = Context.newBuilder(graalLanguage)
                .allowAllAccess(false)
                .build()) {

            // Bind input data
            Value bindings = polyglotContext.getBindings(graalLanguage);
            bindings.putMember("$input", input);
            bindings.putMember("$node", node.parameters());

            // Execute code
            Value result = polyglotContext.eval(graalLanguage, wrapCode(code, graalLanguage));

            // Extract result
            Map<String, Object> output = new HashMap<>(input);
            if (result.hasMembers()) {
                for (String key : result.getMemberKeys()) {
                    output.put(key, convertValue(result.getMember(key)));
                }
            } else if (!result.isNull()) {
                output.put("result", convertValue(result));
            }

            return output;
        } catch (Exception e) {
            throw new RuntimeException("Code execution failed: " + e.getMessage(), e);
        }
    }

    private String wrapCode(String code, String language) {
        // Wrap code to return a value
        return switch (language) {
            case "js" -> """
                    (function() {
                        %s
                    })()
                    """.formatted(code);
            case "python" -> code;
            default -> code;
        };
    }

    private Object convertValue(Value value) {
        if (value.isNull()) return null;
        if (value.isBoolean()) return value.asBoolean();
        if (value.isNumber()) {
            if (value.fitsInLong()) return value.asLong();
            return value.asDouble();
        }
        if (value.isString()) return value.asString();
        if (value.hasArrayElements()) {
            int size = (int) value.getArraySize();
            Object[] array = new Object[size];
            for (int i = 0; i < size; i++) {
                array[i] = convertValue(value.getArrayElement(i));
            }
            return array;
        }
        if (value.hasMembers()) {
            Map<String, Object> map = new HashMap<>();
            for (String key : value.getMemberKeys()) {
                map.put(key, convertValue(value.getMember(key)));
            }
            return map;
        }
        return value.toString();
    }

    @Override
    public String getNodeType() {
        return "code";
    }
}
