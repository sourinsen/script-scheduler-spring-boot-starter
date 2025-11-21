package io.scriptscheduler;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for the Script Scheduler.
 *
 * <p>These properties are bound from the application configuration under the
 * {@code script-scheduler} prefix.
 *
 * <p>Example configuration:
 * <pre>
 * script-scheduler:
 *   enabled: true
 *   thread-pool-size: 10
 *   shutdown-timeout-seconds: 60
 *   jobs:
 *     - name: backup-job
 *       script: /path/to/backup.sh
 *       cron: "0 0 * * * ?"
 * </pre>
 *
 * @author Sourin Kumar Sen
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "script-scheduler")
@Validated
public class ScriptSchedulerProperties {

    /**
     * Whether the script scheduler is enabled.
     * Default is true.
     */
    private boolean enabled = true;

    /**
     * The size of the thread pool used for executing scripts.
     * Default is 10.
     */
    @Min(1)
    private int threadPoolSize = 10;

    /**
     * The timeout in seconds to wait for running scripts to complete on shutdown.
     * Default is 60 seconds.
     */
    @Min(1)
    private int shutdownTimeoutSeconds = 60;

    /**
     * List of scheduled script jobs.
     */
    @NotNull
    @Valid
    private List<ScheduledScript> jobs = new ArrayList<>();

    /**
     * Global default timeout for all scripts in seconds.
     * Can be overridden per script.
     * Default is 300 seconds (5 minutes).
     */
    @Min(1)
    private int defaultTimeoutSeconds = 300;

    /**
     * Global default interpreter for all scripts.
     * Can be overridden per script.
     * Default is /bin/sh.
     */
    private String defaultInterpreter = "/bin/sh";

    /**
     * Whether to log script output by default.
     * Can be overridden per script.
     * Default is true.
     */
    private boolean defaultLogOutput = true;

    // Getters and setters

    /**
     * @return whether the script scheduler is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param enabled whether to enable the script scheduler
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return the thread pool size for script execution
     */
    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    /**
     * @param threadPoolSize the thread pool size to set
     */
    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    /**
     * @return the shutdown timeout in seconds
     */
    public int getShutdownTimeoutSeconds() {
        return shutdownTimeoutSeconds;
    }

    /**
     * @param shutdownTimeoutSeconds the shutdown timeout to set
     */
    public void setShutdownTimeoutSeconds(int shutdownTimeoutSeconds) {
        this.shutdownTimeoutSeconds = shutdownTimeoutSeconds;
    }

    /**
     * @return the list of scheduled script jobs
     */
    public List<ScheduledScript> getJobs() {
        return jobs;
    }

    /**
     * @param jobs the list of jobs to set
     */
    public void setJobs(List<ScheduledScript> jobs) {
        this.jobs = jobs;
    }

    /**
     * @return the default timeout for scripts in seconds
     */
    public int getDefaultTimeoutSeconds() {
        return defaultTimeoutSeconds;
    }

    /**
     * @param defaultTimeoutSeconds the default timeout to set
     */
    public void setDefaultTimeoutSeconds(int defaultTimeoutSeconds) {
        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
    }

    /**
     * @return the default interpreter for scripts
     */
    public String getDefaultInterpreter() {
        return defaultInterpreter;
    }

    /**
     * @param defaultInterpreter the default interpreter to set
     */
    public void setDefaultInterpreter(String defaultInterpreter) {
        this.defaultInterpreter = defaultInterpreter;
    }

    /**
     * @return whether to log script output by default
     */
    public boolean isDefaultLogOutput() {
        return defaultLogOutput;
    }

    /**
     * @param defaultLogOutput whether to log script output by default
     */
    public void setDefaultLogOutput(boolean defaultLogOutput) {
        this.defaultLogOutput = defaultLogOutput;
    }
}
