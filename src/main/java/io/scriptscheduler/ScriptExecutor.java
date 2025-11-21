package io.scriptscheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Executes shell scripts with configurable timeouts, environment variables,
 * and working directories.
 *
 * <p>This class is responsible for the actual execution of shell scripts,
 * including process management, timeout handling, output streaming, and
 * proper cleanup of system resources.
 *
 * <p>Features:
 * <ul>
 *   <li>Configurable script interpreter</li>
 *   <li>Timeout management with process termination</li>
 *   <li>Environment variable injection</li>
 *   <li>Working directory configuration</li>
 *   <li>Real-time output streaming</li>
 *   <li>Proper process cleanup to prevent zombie processes</li>
 * </ul>
 *
 * @author Sourin Kumar Sen
 * @since 1.0.0
 */
public class ScriptExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ScriptExecutor.class);

    private final ScriptSchedulerProperties properties;

    /**
     * Creates a new ScriptExecutor with the given configuration.
     *
     * @param properties the script scheduler configuration properties
     */
    public ScriptExecutor(ScriptSchedulerProperties properties) {
        this.properties = properties;
    }

    /**
     * Executes a scheduled script with retry logic.
     *
     * @param script the script configuration to execute
     * @return the exit code of the script (0 for success)
     */
    public int execute(ScheduledScript script) {
        int attempts = 0;
        int maxAttempts = script.getRetryAttempts() + 1;
        int exitCode = -1;

        while (attempts < maxAttempts) {
            attempts++;

            if (attempts > 1) {
                logger.info("Retry attempt {} of {} for script: {}",
                           attempts - 1, script.getRetryAttempts(), script.getName());
                try {
                    Thread.sleep(script.getRetryDelaySeconds() * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Interrupted while waiting for retry: {}", script.getName());
                    return exitCode;
                }
            }

            try {
                exitCode = executeScript(script);

                if (exitCode == 0) {
                    if (attempts > 1) {
                        logger.info("Script {} succeeded on attempt {}",
                                   script.getName(), attempts);
                    }
                    return exitCode;
                }

                logger.warn("Script {} failed with exit code: {}",
                           script.getName(), exitCode);

            } catch (Exception e) {
                logger.error("Error executing script {}: {}",
                            script.getName(), e.getMessage(), e);
                exitCode = -1;
            }
        }

        logger.error("Script {} failed after {} attempts",
                    script.getName(), maxAttempts);
        return exitCode;
    }

    /**
     * Executes a single instance of the script.
     *
     * @param script the script configuration
     * @return the exit code of the script
     * @throws IOException if there's an error starting the process
     * @throws InterruptedException if the thread is interrupted
     */
    private int executeScript(ScheduledScript script) throws IOException, InterruptedException {
        // Validate script file exists
        File scriptFile = new File(script.getScript());
        if (!scriptFile.exists()) {
            throw new IOException("Script file not found: " + script.getScript());
        }

        if (!scriptFile.canRead()) {
            throw new IOException("Script file not readable: " + script.getScript());
        }

        // Prepare command
        String interpreter = script.getInterpreter() != null ?
            script.getInterpreter() : properties.getDefaultInterpreter();

        List<String> command = new ArrayList<>();
        command.add(interpreter);
        command.add(scriptFile.getAbsolutePath());

        // Create process builder
        ProcessBuilder processBuilder = new ProcessBuilder(command);

        // Set working directory
        if (script.getWorkingDirectory() != null) {
            File workDir = new File(script.getWorkingDirectory());
            if (!workDir.exists() || !workDir.isDirectory()) {
                logger.warn("Working directory does not exist or is not a directory: {}. Using default.",
                           script.getWorkingDirectory());
            } else {
                processBuilder.directory(workDir);
            }
        }

        // Set environment variables
        if (script.getEnvironment() != null && !script.getEnvironment().isEmpty()) {
            Map<String, String> env = processBuilder.environment();
            env.putAll(script.getEnvironment());
        }

        // Determine timeout
        int timeoutSeconds = script.getTimeoutSeconds() != null ?
            script.getTimeoutSeconds() : properties.getDefaultTimeoutSeconds();

        // Determine if output should be logged
        boolean logOutput = script.getLogOutput() != null ?
            script.getLogOutput() : properties.isDefaultLogOutput();

        logger.info("Executing script: {} with interpreter: {} and timeout: {}s",
                   script.getName(), interpreter, timeoutSeconds);

        // Start the process
        Process process = processBuilder.start();

        // Handle output streaming
        CompletableFuture<Void> outputFuture = CompletableFuture.completedFuture(null);
        CompletableFuture<Void> errorFuture = CompletableFuture.completedFuture(null);

        if (logOutput) {
            outputFuture = streamOutput(process, script.getName(), false);
            errorFuture = streamOutput(process, script.getName(), true);
        }

        try {
            // Wait for the process with timeout
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                logger.error("Script {} timed out after {} seconds. Terminating process.",
                            script.getName(), timeoutSeconds);

                // Try graceful termination first
                process.destroy();
                if (!process.waitFor(10, TimeUnit.SECONDS)) {
                    // Force kill if graceful termination fails
                    logger.warn("Forcibly killing script {} after graceful termination failed",
                               script.getName());
                    process.destroyForcibly();
                    process.waitFor(5, TimeUnit.SECONDS);
                }

                return -1;
            }

            // Wait for output streams to complete
            try {
                outputFuture.get(5, TimeUnit.SECONDS);
                errorFuture.get(5, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                logger.warn("Timed out waiting for output streams to complete for script: {}",
                           script.getName());
            } catch (ExecutionException e) {
                logger.warn("Error reading output streams for script: {}",
                           script.getName(), e);
            }

            int exitCode = process.exitValue();

            if (exitCode == 0) {
                logger.info("Script {} completed successfully", script.getName());
            } else {
                logger.warn("Script {} exited with code: {}", script.getName(), exitCode);
            }

            return exitCode;

        } finally {
            // Ensure process is destroyed to prevent zombie processes
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /**
     * Streams the output from a process to the logger.
     *
     * @param process the process to stream output from
     * @param scriptName the name of the script for logging
     * @param isError whether to stream stderr (true) or stdout (false)
     * @return a CompletableFuture that completes when streaming is done
     */
    private CompletableFuture<Void> streamOutput(Process process, String scriptName, boolean isError) {
        return CompletableFuture.runAsync(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                        isError ? process.getErrorStream() : process.getInputStream(),
                        StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (isError) {
                        logger.error("[{}] STDERR: {}", scriptName, line);
                    } else {
                        logger.info("[{}] STDOUT: {}", scriptName, line);
                    }
                }
            } catch (IOException e) {
                logger.error("Error reading {} stream for script {}: {}",
                            isError ? "error" : "output", scriptName, e.getMessage());
            }
        });
    }
}
