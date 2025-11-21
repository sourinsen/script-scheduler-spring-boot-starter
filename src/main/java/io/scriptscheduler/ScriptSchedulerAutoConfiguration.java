package io.scriptscheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Auto-configuration for Script Scheduler that automatically configures the necessary beans
 * when the starter is on the classpath and the scheduler is enabled.
 *
 * <p>This configuration is activated when:
 * <ul>
 *   <li>The starter is on the classpath</li>
 *   <li>The property {@code script-scheduler.enabled} is set to true (default)</li>
 * </ul>
 *
 * @author Sourin Kumar Sen
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnProperty(
    prefix = "script-scheduler",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
@EnableConfigurationProperties(ScriptSchedulerProperties.class)
@EnableScheduling
public class ScriptSchedulerAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ScriptSchedulerAutoConfiguration.class);

    /**
     * Creates a {@link TaskScheduler} bean configured for script execution.
     *
     * <p>This scheduler is used to execute scripts based on their cron expressions.
     * The thread pool size is configurable via the {@code script-scheduler.thread-pool-size} property.
     *
     * @param properties the script scheduler configuration properties
     * @return a configured {@link TaskScheduler}
     */
    @Bean
    @ConditionalOnMissingBean
    public TaskScheduler scriptTaskScheduler(ScriptSchedulerProperties properties) {
        logger.info("Configuring Script Scheduler with thread pool size: {}",
                   properties.getThreadPoolSize());

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(properties.getThreadPoolSize());
        scheduler.setThreadNamePrefix("script-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(properties.getShutdownTimeoutSeconds());
        scheduler.setRejectedExecutionHandler((r, executor) ->
            logger.warn("Script execution rejected due to thread pool saturation"));
        scheduler.initialize();

        return scheduler;
    }

    /**
     * Creates a {@link ScriptExecutor} bean for executing shell scripts.
     *
     * @param properties the script scheduler configuration properties
     * @return a configured {@link ScriptExecutor}
     */
    @Bean
    @ConditionalOnMissingBean
    public ScriptExecutor scriptExecutor(ScriptSchedulerProperties properties) {
        logger.debug("Creating ScriptExecutor bean");
        return new ScriptExecutor(properties);
    }

    /**
     * Creates a {@link ScriptSchedulerService} bean that manages the scheduling and execution of scripts.
     *
     * @param taskScheduler the task scheduler for scheduling script executions
     * @param scriptExecutor the script executor for running scripts
     * @param properties the script scheduler configuration properties
     * @return a configured {@link ScriptSchedulerService}
     */
    @Bean
    @ConditionalOnMissingBean
    public ScriptSchedulerService scriptSchedulerService(
            TaskScheduler taskScheduler,
            ScriptExecutor scriptExecutor,
            ScriptSchedulerProperties properties) {

        logger.info("Initializing Script Scheduler Service");
        return new ScriptSchedulerService(taskScheduler, scriptExecutor, properties);
    }
}
