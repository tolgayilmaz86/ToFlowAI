package io.toflowai.app.executor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.toflowai.app.service.ExecutionService;
import io.toflowai.app.service.NodeExecutor;
import io.toflowai.common.domain.Node;

/**
 * Execute Command node - runs shell commands.
 * 
 * Parameters:
 * - command: The command to execute (required)
 * - arguments: List of arguments to pass to the command
 * - workingDirectory: Working directory for the command (default: system temp)
 * - timeout: Timeout in seconds (default: 300)
 * - shell: Shell to use - "cmd", "powershell", "bash", "sh" (default:
 * auto-detect OS)
 * - environment: Map of environment variables to set
 * - captureOutput: Whether to capture stdout/stderr (default: true)
 * - failOnError: Whether to fail if exit code is non-zero (default: true)
 */
@Component
public class ExecuteCommandExecutor implements NodeExecutor {
    private static final Logger log = LoggerFactory.getLogger(ExecuteCommandExecutor.class);

    private static final Pattern INTERPOLATION_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> params = node.parameters();

        String command = interpolate((String) params.get("command"), input);
        if (command == null || command.isBlank()) {
            return Map.of("success", false, "error", "Command is required");
        }

        @SuppressWarnings("unchecked")
        List<String> arguments = (List<String>) params.getOrDefault("arguments", List.of());
        String workingDirectory = (String) params.get("workingDirectory");
        int timeout = ((Number) params.getOrDefault("timeout", 300)).intValue();
        String shell = (String) params.get("shell");
        @SuppressWarnings("unchecked")
        Map<String, String> environment = (Map<String, String>) params.getOrDefault("environment", Map.of());
        boolean captureOutput = (Boolean) params.getOrDefault("captureOutput", true);
        boolean failOnError = (Boolean) params.getOrDefault("failOnError", true);

        // Interpolate arguments
        List<String> interpolatedArgs = arguments.stream()
                .map(arg -> interpolate(arg, input))
                .toList();

        try {
            ProcessResult result = executeCommand(command, interpolatedArgs, workingDirectory,
                    timeout, shell, environment, captureOutput);

            Map<String, Object> output = new HashMap<>(input);
            output.put("exitCode", result.exitCode);
            output.put("stdout", result.stdout);
            output.put("stderr", result.stderr);
            output.put("success", result.exitCode == 0);
            output.put("timedOut", result.timedOut);
            output.put("executedCommand", command);
            output.put("executedArguments", interpolatedArgs);
            output.put("durationMs", result.durationMs);

            if (failOnError && result.exitCode != 0 && !result.timedOut) {
                output.put("error", "Command exited with code " + result.exitCode + ": " + result.stderr);
            }

            if (result.timedOut) {
                output.put("error", "Command timed out after " + timeout + " seconds");
            }

            return output;

        } catch (Exception e) {
            log.error("Failed to execute command: {}", command, e);
            Map<String, Object> output = new HashMap<>(input);
            output.put("success", false);
            output.put("error", e.getMessage());
            output.put("exitCode", -1);
            output.put("stdout", "");
            output.put("stderr", "");
            return output;
        }
    }

    private ProcessResult executeCommand(String command, List<String> arguments,
            String workingDirectory, int timeout, String shell,
            Map<String, String> environment, boolean captureOutput)
            throws IOException, InterruptedException {

        long startTime = System.currentTimeMillis();

        // Build the command list
        List<String> commandList = buildCommandList(command, arguments, shell);

        ProcessBuilder pb = new ProcessBuilder(commandList);

        // Set working directory
        if (workingDirectory != null && !workingDirectory.isBlank()) {
            File workDir = new File(workingDirectory);
            if (workDir.exists() && workDir.isDirectory()) {
                pb.directory(workDir);
            }
        }

        // Set environment variables
        pb.environment().putAll(environment);

        // Redirect error stream to capture both stdout and stderr
        if (captureOutput) {
            pb.redirectErrorStream(false);
        }

        Process process = pb.start();

        // Read output streams in separate threads to prevent blocking
        StringBuilder stdoutBuilder = new StringBuilder();
        StringBuilder stderrBuilder = new StringBuilder();

        Future<?> stdoutFuture = null;
        Future<?> stderrFuture = null;

        if (captureOutput) {
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

            stdoutFuture = executor.submit(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stdoutBuilder.append(line).append(System.lineSeparator());
                    }
                } catch (IOException e) {
                    log.debug("Error reading stdout", e);
                }
            });

            stderrFuture = executor.submit(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderrBuilder.append(line).append(System.lineSeparator());
                    }
                } catch (IOException e) {
                    log.debug("Error reading stderr", e);
                }
            });

            executor.shutdown();
        }

        // Wait for process with timeout
        boolean completed = process.waitFor(timeout, TimeUnit.SECONDS);

        if (!completed) {
            process.destroyForcibly();
            return new ProcessResult(-1, "", "Process timed out", true,
                    System.currentTimeMillis() - startTime);
        }

        // Wait for output readers to complete
        if (captureOutput && stdoutFuture != null && stderrFuture != null) {
            try {
                stdoutFuture.get(5, TimeUnit.SECONDS);
                stderrFuture.get(5, TimeUnit.SECONDS);
            } catch (ExecutionException | TimeoutException e) {
                log.debug("Error waiting for output readers", e);
            }
        }

        long durationMs = System.currentTimeMillis() - startTime;

        return new ProcessResult(
                process.exitValue(),
                stdoutBuilder.toString().trim(),
                stderrBuilder.toString().trim(),
                false,
                durationMs);
    }

    private List<String> buildCommandList(String command, List<String> arguments, String shell) {
        List<String> commandList = new ArrayList<>();

        // Determine shell to use
        String effectiveShell = shell;
        if (effectiveShell == null || effectiveShell.isBlank()) {
            effectiveShell = detectShell();
        }

        // Build full command string
        StringBuilder fullCommand = new StringBuilder(command);
        for (String arg : arguments) {
            fullCommand.append(" ").append(escapeArgument(arg, effectiveShell));
        }

        switch (effectiveShell.toLowerCase()) {
            case "cmd" -> {
                commandList.add("cmd.exe");
                commandList.add("/c");
                commandList.add(fullCommand.toString());
            }
            case "powershell" -> {
                commandList.add("powershell.exe");
                commandList.add("-NoProfile");
                commandList.add("-NonInteractive");
                commandList.add("-Command");
                commandList.add(fullCommand.toString());
            }
            case "bash" -> {
                commandList.add("bash");
                commandList.add("-c");
                commandList.add(fullCommand.toString());
            }
            case "sh" -> {
                commandList.add("sh");
                commandList.add("-c");
                commandList.add(fullCommand.toString());
            }
            default -> {
                // Direct command execution
                commandList.add(command);
                commandList.addAll(arguments);
            }
        }

        return commandList;
    }

    private String detectShell() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("windows")) {
            return "cmd";
        } else {
            return "sh";
        }
    }

    private String escapeArgument(String arg, String shell) {
        if (arg == null || arg.isEmpty()) {
            return "\"\"";
        }

        // Check if escaping is needed
        if (!arg.contains(" ") && !arg.contains("\"") && !arg.contains("'")
                && !arg.contains("&") && !arg.contains("|") && !arg.contains("<")
                && !arg.contains(">")) {
            return arg;
        }

        // Escape based on shell
        if ("powershell".equalsIgnoreCase(shell)) {
            return "'" + arg.replace("'", "''") + "'";
        } else if ("cmd".equalsIgnoreCase(shell)) {
            return "\"" + arg.replace("\"", "\\\"") + "\"";
        } else {
            // Bash/sh
            return "'" + arg.replace("'", "'\\''") + "'";
        }
    }

    private String interpolate(String text, Map<String, Object> data) {
        if (text == null) {
            return null;
        }

        Matcher matcher = INTERPOLATION_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String path = matcher.group(1);
            Object value = getNestedValue(data, path);
            String replacement = value != null ? value.toString() : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
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

    @Override
    public String getNodeType() {
        return "executeCommand";
    }

    private record ProcessResult(int exitCode, String stdout, String stderr, boolean timedOut, long durationMs) {
    }
}
