package io.scriptscheduler;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a scheduled script job configuration.
 *
 * <p>This class holds all the configuration for a single script that should be
 * executed on a schedule. Each script can have its own cron expression, timeout,
 * working directory, environment variables, and other settings.
 *
 * <p>Example YAML configuration:
 * <pre>
 * - name: daily-backup
 *   script: /opt/scripts/backup.sh
 *   cron: "0 0 2 * * ?"
 *   description: "Daily database backup"
 *   interpreter: /bin/bash
 *   working-directory: /opt/backup
 *   timeout-seconds: 600
 *   log-output: true
 *   environment:
 *     BACKUP_DIR: /mnt/backup
 *     RETENTION_DAYS: 30
 * </pre>
 *
 * @author Sourin Kumar Sen
 * @since 1.0.0
 */
public class ScheduledScript {

    /**
     * Unique name for the scheduled job.
     * Used for logging and identification.
     */
    @NotBlank(message = "Script name cannot be blank")
    private String name;

    /**
     * Path to the script file to execute.
     * Can be absolute or relative.
     */
    @NotBlank(message = "Script path cannot be blank")
    private String script;

    /**
     * Cron expression defining when to run the script.
     * Uses standard Spring cron format (6 fields including seconds).
     */
    @NotBlank(message = "Cron expression cannot be blank")
    @Pattern(
        regexp = "^(\\S+\\s+){5}\\S+$",
        message = "Invalid cron expression. Must have 6 fields (seconds minutes hours day month weekday)"
    )
    private String cron;

    /**
     * Optional description of what the script does.
     */
    private String description;

    /**
     * The interpreter to use for executing the script.
     * Defaults to /bin/sh if not specified.
     */
    private String interpreter;

    /**
     * The working directory for script execution.
     * If not specified, uses the current working directory.
     */
    private String workingDirectory;

    /**
     * Environment variables to set for the script execution.
     * These are added to the existing environment.
     */
    private Map<String, String> environment = new HashMap<>();

    /**
     * Timeout for the script execution in seconds.
     * If not specified, uses the global default timeout.
     */
    @Min(value = 1, message = "Timeout must be at least 1 second")
    private Integer timeoutSeconds;

    /**
     * Whether to log the script output.
     * If not specified, uses the global default setting.
     */
    private Boolean logOutput;

    /**
     * Maximum number of retry attempts if the script fails.
     * Default is 0 (no retries).
     */
    @Min(value = 0, message = "Retry attempts cannot be negative")
    private int retryAttempts = 0;

    /**
     * Delay between retry attempts in seconds.
     * Default is 10 seconds.
     */
    @Min(value = 1, message = "Retry delay must be at least 1 second")
    private int retryDelaySeconds = 10;

    // Getters and setters

    /**
     * @return the unique name of the script job
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the path to the script file
     */
    public String getScript() {
        return script;
    }

    /**
     * @param script the script path to set
     */
    public void setScript(String script) {
        this.script = script;
    }

    /**
     * @return the cron expression for scheduling
     */
    public String getCron() {
        return cron;
    }

    /**
     * @param cron the cron expression to set
     */
    public void setCron(String cron) {
        this.cron = cron;
    }

    /**
     * @return the description of the script job
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the interpreter to use for the script
     */
    public String getInterpreter() {
        return interpreter;
    }

    /**
     * @param interpreter the interpreter to set
     */
    public void setInterpreter(String interpreter) {
        this.interpreter = interpreter;
    }

    /**
     * @return the working directory for script execution
     */
    public String getWorkingDirectory() {
        return workingDirectory;
    }

    /**
     * @param workingDirectory the working directory to set
     */
    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    /**
     * @return the environment variables for the script
     */
    public Map<String, String> getEnvironment() {
        return environment;
    }

    /**
     * @param environment the environment variables to set
     */
    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    /**
     * @return the timeout in seconds for script execution
     */
    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /**
     * @param timeoutSeconds the timeout to set
     */
    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * @return whether to log script output
     */
    public Boolean getLogOutput() {
        return logOutput;
    }

    /**
     * @param logOutput whether to log output
     */
    public void setLogOutput(Boolean logOutput) {
        this.logOutput = logOutput;
    }

    /**
     * @return the number of retry attempts
     */
    public int getRetryAttempts() {
        return retryAttempts;
    }

    /**
     * @param retryAttempts the retry attempts to set
     */
    public void setRetryAttempts(int retryAttempts) {
        this.retryAttempts = retryAttempts;
    }

    /**
     * @return the delay between retry attempts in seconds
     */
    public int getRetryDelaySeconds() {
        return retryDelaySeconds;
    }

    /**
     * @param retryDelaySeconds the retry delay to set
     */
    public void setRetryDelaySeconds(int retryDelaySeconds) {
        this.retryDelaySeconds = retryDelaySeconds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScheduledScript that = (ScheduledScript) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "ScheduledScript{" +
               "name='" + name + '\'' +
               ", script='" + script + '\'' +
               ", cron='" + cron + '\'' +
               ", description='" + description + '\'' +
               '}';
    }
}
