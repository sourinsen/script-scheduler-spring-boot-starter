package io.scriptscheduler;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service that manages the scheduling and execution of scripts.
 *
 * <p>This service is responsible for:
 * <ul>
 *   <li>Validating script configurations at startup</li>
 *   <li>Scheduling scripts based on their cron expressions</li>
 *   <li>Managing the lifecycle of scheduled tasks</li>
 *   <li>Handling graceful shutdown of running scripts</li>
 *   <li>Providing runtime information about scheduled jobs</li>
 * </ul>
 *
 * <p>The service automatically starts scheduling jobs when the application
 * context is initialized and gracefully stops all jobs on shutdown.
 *
 * @author Sourin Kumar Sen
 * @since 1.0.0
 */
public class ScriptSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(ScriptSchedulerService.class);

    private final TaskScheduler taskScheduler;
    private final ScriptExecutor scriptExecutor;
    private final ScriptSchedulerProperties properties;
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final Map<String, AtomicBoolean> runningScripts = new ConcurrentHashMap<>();
    private final List<ScheduledScript> validatedScripts = new ArrayList<>();

    /**
     * Creates a new ScriptSchedulerService.
     *
     * @param taskScheduler the task scheduler for scheduling scripts
     * @param scriptExecutor the executor for running scripts
     * @param properties the configuration properties
     */
    public ScriptSchedulerService(TaskScheduler taskScheduler,
                                   ScriptExecutor scriptExecutor,
                                   ScriptSchedulerProperties properties) {
        this.taskScheduler = taskScheduler;
        this.scriptExecutor = scriptExecutor;
        this.properties = properties;
    }

    /**
     * Initializes the service and schedules all configured scripts.
     *
     * <p>This method is called automatically after the bean is constructed
     * and all dependencies are injected.
     */
    @PostConstruct
    public void init() {
        logger.info("Initializing Script Scheduler Service");

        if (properties.getJobs() == null || properties.getJobs().isEmpty()) {
            logger.warn("No script jobs configured. Script scheduler is running but idle.");
            return;
        }

        // Validate all scripts first
        validateScripts();

        // Schedule valid scripts
        scheduleScripts();

        logger.info("Script Scheduler Service initialized with {} active jobs",
                   scheduledTasks.size());
    }

    /**
     * Validates all configured scripts.
     *
     * <p>This method checks that:
     * <ul>
     *   <li>Script files exist and are readable</li>
     *   <li>Cron expressions are valid</li>
     *   <li>Script names are unique</li>
     *   <li>Required fields are present</li>
     * </ul>
     */
    private void validateScripts() {
        logger.info("Validating {} configured scripts", properties.getJobs().size());

        List<String> scriptNames = new ArrayList<>();

        for (ScheduledScript script : properties.getJobs()) {
            try {
                // Check for duplicate names
                if (scriptNames.contains(script.getName())) {
                    logger.error("Duplicate script name found: {}. Script will be skipped.",
                                script.getName());
                    continue;
                }
                scriptNames.add(script.getName());

                // Validate script file
                if (script.getScript() == null || script.getScript().isEmpty()) {
                    logger.error("Script path is empty for job: {}. Script will be skipped.",
                                script.getName());
                    continue;
                }

                // Validate cron expression
                try {
                    CronExpression.parse(script.getCron());

                    // Log next execution time
                    CronExpression cronExpression = CronExpression.parse(script.getCron());
                    LocalDateTime next = cronExpression.next(LocalDateTime.now());
                    if (next != null) {
                        logger.info("Script '{}' will first run at: {}",
                                   script.getName(), next);
                    } else {
                        logger.error("Cron expression '{}' for script '{}' will never execute. Script will be skipped.",
                                script.getCron(), script.getName());
                        continue;
                    }
                } catch (IllegalArgumentException e) {
                    logger.error("Invalid cron expression '{}' for script: {}. Script will be skipped.",
                                script.getCron(), script.getName());
                    continue;
                }

                // Apply defaults if not set
                if (script.getInterpreter() == null) {
                    script.setInterpreter(properties.getDefaultInterpreter());
                }
                if (script.getTimeoutSeconds() == null) {
                    script.setTimeoutSeconds(properties.getDefaultTimeoutSeconds());
                }
                if (script.getLogOutput() == null) {
                    script.setLogOutput(properties.isDefaultLogOutput());
                }

                validatedScripts.add(script);
                logger.debug("Script '{}' validated successfully", script.getName());

            } catch (Exception e) {
                logger.error("Error validating script '{}': {}",
                            script.getName(), e.getMessage(), e);
            }
        }

        logger.info("Validation complete. {} of {} scripts are valid.",
                   validatedScripts.size(), properties.getJobs().size());
    }

    /**
     * Schedules all validated scripts for execution.
     */
    private void scheduleScripts() {
        for (ScheduledScript script : validatedScripts) {
            try {
                scheduleScript(script);
            } catch (Exception e) {
                logger.error("Failed to schedule script '{}': {}",
                            script.getName(), e.getMessage(), e);
            }
        }
    }

    /**
     * Schedules a single script for execution.
     *
     * @param script the script to schedule
     */
    private void scheduleScript(ScheduledScript script) {
        logger.info("Scheduling script '{}' with cron expression: {}",
                   script.getName(), script.getCron());

        // Create a running flag for this script
        runningScripts.put(script.getName(), new AtomicBoolean(false));

        // Create the task
        Runnable task = () -> executeScript(script);

        // Schedule the task
        CronTrigger trigger = new CronTrigger(script.getCron());
        ScheduledFuture<?> future = taskScheduler.schedule(task, trigger);

        // Store the future for management
        scheduledTasks.put(script.getName(), future);

        logger.info("Script '{}' scheduled successfully", script.getName());

        if (script.getDescription() != null && !script.getDescription().isEmpty()) {
            logger.info("Script '{}' description: {}", script.getName(), script.getDescription());
        }
    }

    /**
     * Executes a script, ensuring only one instance runs at a time.
     *
     * @param script the script to execute
     */
    private void executeScript(ScheduledScript script) {
        AtomicBoolean isRunning = runningScripts.get(script.getName());

        if (isRunning == null) {
            logger.error("Script '{}' not found in running scripts map", script.getName());
            return;
        }

        // Check if script is already running
        if (!isRunning.compareAndSet(false, true)) {
            logger.warn("Script '{}' is already running. Skipping this execution.",
                       script.getName());
            return;
        }

        try {
            logger.info("Starting execution of script: {}", script.getName());
            long startTime = System.currentTimeMillis();

            int exitCode = scriptExecutor.execute(script);

            long duration = System.currentTimeMillis() - startTime;

            if (exitCode == 0) {
                logger.info("Script '{}' completed successfully in {} ms",
                           script.getName(), duration);
            } else {
                logger.error("Script '{}' failed with exit code {} after {} ms",
                            script.getName(), exitCode, duration);
            }

        } catch (Exception e) {
            logger.error("Unexpected error executing script '{}': {}",
                        script.getName(), e.getMessage(), e);
        } finally {
            isRunning.set(false);
        }
    }

    /**
     * Gracefully shuts down the scheduler and all running scripts.
     *
     * <p>This method is called automatically when the application context
     * is being destroyed.
     */
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down Script Scheduler Service");

        // Cancel all scheduled tasks
        for (Map.Entry<String, ScheduledFuture<?>> entry : scheduledTasks.entrySet()) {
            String scriptName = entry.getKey();
            ScheduledFuture<?> future = entry.getValue();

            if (future != null && !future.isCancelled()) {
                logger.info("Cancelling scheduled task for script: {}", scriptName);
                future.cancel(false);
            }
        }

        // Wait for running scripts to complete
        int waitTime = 0;
        int maxWaitTime = properties.getShutdownTimeoutSeconds() * 1000;
        int checkInterval = 1000; // Check every second

        while (waitTime < maxWaitTime && hasRunningScripts()) {
            logger.info("Waiting for running scripts to complete...");
            try {
                Thread.sleep(checkInterval);
                waitTime += checkInterval;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while waiting for scripts to complete");
                break;
            }
        }

        if (hasRunningScripts()) {
            logger.warn("Some scripts are still running after {} seconds. Proceeding with shutdown.",
                       properties.getShutdownTimeoutSeconds());
        }

        scheduledTasks.clear();
        runningScripts.clear();
        validatedScripts.clear();

        logger.info("Script Scheduler Service shutdown complete");
    }

    /**
     * Checks if any scripts are currently running.
     *
     * @return true if at least one script is running
     */
    private boolean hasRunningScripts() {
        return runningScripts.values().stream()
            .anyMatch(AtomicBoolean::get);
    }

    /**
     * Gets the names of all scheduled scripts.
     *
     * @return list of script names
     */
    public List<String> getScheduledScriptNames() {
        return new ArrayList<>(scheduledTasks.keySet());
    }

    /**
     * Checks if a specific script is currently running.
     *
     * @param scriptName the name of the script
     * @return true if the script is running
     */
    public boolean isScriptRunning(String scriptName) {
        AtomicBoolean isRunning = runningScripts.get(scriptName);
        return isRunning != null && isRunning.get();
    }

    /**
     * Gets the number of scheduled scripts.
     *
     * @return the count of scheduled scripts
     */
    public int getScheduledScriptCount() {
        return scheduledTasks.size();
    }
}
