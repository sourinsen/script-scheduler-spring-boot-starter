package io.scriptscheduler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ScriptSchedulerProperties}.
 *
 * @author Sourin Kumar Sen
 * @since 1.0.0
 */
class ScriptSchedulerPropertiesTest {

    private Validator validator;
    private ScriptSchedulerProperties properties;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        properties = new ScriptSchedulerProperties();
    }

    @Test
    void defaultValuesShouldBeCorrect() {
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getThreadPoolSize()).isEqualTo(10);
        assertThat(properties.getShutdownTimeoutSeconds()).isEqualTo(60);
        assertThat(properties.getDefaultTimeoutSeconds()).isEqualTo(300);
        assertThat(properties.getDefaultInterpreter()).isEqualTo("/bin/sh");
        assertThat(properties.isDefaultLogOutput()).isTrue();
        assertThat(properties.getJobs()).isNotNull().isEmpty();
    }

    @Test
    void shouldAcceptValidConfiguration() {
        properties.setEnabled(true);
        properties.setThreadPoolSize(20);
        properties.setShutdownTimeoutSeconds(120);
        properties.setDefaultTimeoutSeconds(600);
        properties.setDefaultInterpreter("/bin/bash");
        properties.setDefaultLogOutput(false);

        ScheduledScript job = new ScheduledScript();
        job.setName("test-job");
        job.setScript("/path/to/script.sh");
        job.setCron("0 0 * * * ?");

        List<ScheduledScript> jobs = new ArrayList<>();
        jobs.add(job);
        properties.setJobs(jobs);

        Set<ConstraintViolation<ScriptSchedulerProperties>> violations = validator.validate(properties);
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldRejectInvalidThreadPoolSize() {
        properties.setThreadPoolSize(0);

        Set<ConstraintViolation<ScriptSchedulerProperties>> violations = validator.validate(properties);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("must be greater than or equal to 1");
    }

    @Test
    void shouldRejectInvalidShutdownTimeout() {
        properties.setShutdownTimeoutSeconds(-1);

        Set<ConstraintViolation<ScriptSchedulerProperties>> violations = validator.validate(properties);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("must be greater than or equal to 1");
    }

    @Test
    void shouldRejectInvalidDefaultTimeout() {
        properties.setDefaultTimeoutSeconds(0);

        Set<ConstraintViolation<ScriptSchedulerProperties>> violations = validator.validate(properties);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("must be greater than or equal to 1");
    }

    @Test
    void shouldValidateNestedScheduledScripts() {
        ScheduledScript invalidJob = new ScheduledScript();
        // Missing required fields

        List<ScheduledScript> jobs = new ArrayList<>();
        jobs.add(invalidJob);
        properties.setJobs(jobs);

        Set<ConstraintViolation<ScriptSchedulerProperties>> violations = validator.validate(properties);
        assertThat(violations).isNotEmpty();

        // Check that nested validation errors are present
        boolean hasNameError = violations.stream()
            .anyMatch(v -> v.getMessage().contains("Script name cannot be blank"));
        boolean hasScriptError = violations.stream()
            .anyMatch(v -> v.getMessage().contains("Script path cannot be blank"));
        boolean hasCronError = violations.stream()
            .anyMatch(v -> v.getMessage().contains("Cron expression cannot be blank"));

        assertThat(hasNameError).isTrue();
        assertThat(hasScriptError).isTrue();
        assertThat(hasCronError).isTrue();
    }

    @Test
    void jobsShouldBeModifiable() {
        List<ScheduledScript> jobs = new ArrayList<>();

        ScheduledScript job1 = new ScheduledScript();
        job1.setName("job1");
        job1.setScript("/script1.sh");
        job1.setCron("0 0 * * * ?");

        ScheduledScript job2 = new ScheduledScript();
        job2.setName("job2");
        job2.setScript("/script2.sh");
        job2.setCron("0 30 * * * ?");

        jobs.add(job1);
        jobs.add(job2);

        properties.setJobs(jobs);

        assertThat(properties.getJobs()).hasSize(2);
        assertThat(properties.getJobs()).containsExactly(job1, job2);

        // Modify the list
        properties.getJobs().remove(job1);
        assertThat(properties.getJobs()).hasSize(1);
        assertThat(properties.getJobs()).containsExactly(job2);
    }

    @Test
    void shouldHandleNullJobs() {
        properties.setJobs(null);

        Set<ConstraintViolation<ScriptSchedulerProperties>> violations = validator.validate(properties);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("must not be null");
    }

    @Test
    void shouldAcceptEmptyJobsList() {
        properties.setJobs(new ArrayList<>());

        Set<ConstraintViolation<ScriptSchedulerProperties>> violations = validator.validate(properties);
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldAllowDisablingScheduler() {
        properties.setEnabled(false);

        assertThat(properties.isEnabled()).isFalse();

        Set<ConstraintViolation<ScriptSchedulerProperties>> violations = validator.validate(properties);
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldValidateComplexJobConfiguration() {
        ScheduledScript job = new ScheduledScript();
        job.setName("complex-job");
        job.setScript("/opt/scripts/complex.sh");
        job.setCron("0 0 2 * * ?");
        job.setDescription("Complex job with full configuration");
        job.setInterpreter("/bin/bash");
        job.setWorkingDirectory("/opt/work");
        job.setTimeoutSeconds(600);
        job.setLogOutput(true);
        job.setRetryAttempts(3);
        job.setRetryDelaySeconds(30);

        Map<String, String> env = new HashMap<>();
        env.put("KEY1", "value1");
        env.put("KEY2", "value2");
        job.setEnvironment(env);

        List<ScheduledScript> jobs = new ArrayList<>();
        jobs.add(job);
        properties.setJobs(jobs);

        Set<ConstraintViolation<ScriptSchedulerProperties>> violations = validator.validate(properties);
        assertThat(violations).isEmpty();

        assertThat(properties.getJobs()).hasSize(1);
        ScheduledScript savedJob = properties.getJobs().get(0);
        assertThat(savedJob.getName()).isEqualTo("complex-job");
        assertThat(savedJob.getEnvironment()).hasSize(2);
        assertThat(savedJob.getEnvironment()).containsEntry("KEY1", "value1");
    }
}
