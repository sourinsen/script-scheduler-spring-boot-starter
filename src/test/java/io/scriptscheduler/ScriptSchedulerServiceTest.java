package io.scriptscheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link ScriptSchedulerService}.
 *
 * @author Sourin Kumar Sen
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class ScriptSchedulerServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private ScriptExecutor scriptExecutor;

    @Mock
    private ScheduledFuture<?> scheduledFuture;

    private ScriptSchedulerProperties properties;
    private ScriptSchedulerService service;

    @BeforeEach
    void setUp() {
        properties = new ScriptSchedulerProperties();
        lenient().when(taskScheduler.schedule(any(Runnable.class), any(CronTrigger.class)))
            .thenReturn((ScheduledFuture) scheduledFuture);
    }

    @Test
    void shouldInitializeWithNoJobs() {
        // Empty job list
        service = new ScriptSchedulerService(taskScheduler, scriptExecutor, properties);
        service.init();

        assertThat(service.getScheduledScriptCount()).isZero();
        assertThat(service.getScheduledScriptNames()).isEmpty();

        // No scheduling should occur
        verify(taskScheduler, never()).schedule(any(Runnable.class), any(CronTrigger.class));
    }

    @Test
    void shouldScheduleValidJobs() throws IOException {
        // Create a test script
        Path scriptPath = createTestScript("test.sh");

        ScheduledScript job = createValidJob("test-job", scriptPath.toString());

        List<ScheduledScript> jobs = new ArrayList<>();
        jobs.add(job);
        properties.setJobs(jobs);

        service = new ScriptSchedulerService(taskScheduler, scriptExecutor, properties);
        service.init();

        // Verify job was scheduled
        verify(taskScheduler, times(1)).schedule(any(Runnable.class), any(CronTrigger.class));

        assertThat(service.getScheduledScriptCount()).isEqualTo(1);
        assertThat(service.getScheduledScriptNames()).containsExactly("test-job");
    }

    @Test
    void shouldSkipInvalidJobs() {
        // Job with invalid cron expression
        ScheduledScript invalidJob = new ScheduledScript();
        invalidJob.setName("invalid-job");
        invalidJob.setScript("/path/to/script.sh");
        invalidJob.setCron("invalid cron");

        List<ScheduledScript> jobs = new ArrayList<>();
        jobs.add(invalidJob);
        properties.setJobs(jobs);

        service = new ScriptSchedulerService(taskScheduler, scriptExecutor, properties);
        service.init();

        // Should not schedule invalid job
        verify(taskScheduler, never()).schedule(any(Runnable.class), any(CronTrigger.class));

        assertThat(service.getScheduledScriptCount()).isZero();
        assertThat(service.getScheduledScriptNames()).isEmpty();
    }

    @Test
    void shouldSkipDuplicateJobNames() throws IOException {
        Path script1 = createTestScript("script1.sh");
        Path script2 = createTestScript("script2.sh");

        ScheduledScript job1 = createValidJob("duplicate-name", script1.toString());
        ScheduledScript job2 = createValidJob("duplicate-name", script2.toString());

        List<ScheduledScript> jobs = new ArrayList<>();
        jobs.add(job1);
        jobs.add(job2);
        properties.setJobs(jobs);

        service = new ScriptSchedulerService(taskScheduler, scriptExecutor, properties);
        service.init();

        // Should only schedule first job with the name
        verify(taskScheduler, times(1)).schedule(any(Runnable.class), any(CronTrigger.class));

        assertThat(service.getScheduledScriptCount()).isEqualTo(1);
        assertThat(service.getScheduledScriptNames()).containsExactly("duplicate-name");
    }

    @Test
    void shouldScheduleMultipleJobs() throws IOException {
        Path script1 = createTestScript("script1.sh");
        Path script2 = createTestScript("script2.sh");
        Path script3 = createTestScript("script3.sh");

        ScheduledScript job1 = createValidJob("job1", script1.toString());
        ScheduledScript job2 = createValidJob("job2", script2.toString());
        ScheduledScript job3 = createValidJob("job3", script3.toString());

        List<ScheduledScript> jobs = new ArrayList<>();
        jobs.add(job1);
        jobs.add(job2);
        jobs.add(job3);
        properties.setJobs(jobs);

        service = new ScriptSchedulerService(taskScheduler, scriptExecutor, properties);
        service.init();

        verify(taskScheduler, times(3)).schedule(any(Runnable.class), any(CronTrigger.class));

        assertThat(service.getScheduledScriptCount()).isEqualTo(3);
        assertThat(service.getScheduledScriptNames()).containsExactlyInAnyOrder("job1", "job2", "job3");
    }

    @Test
    void shouldApplyDefaultValues() throws IOException {
        Path scriptPath = createTestScript("test.sh");

        // Set defaults in properties
        properties.setDefaultInterpreter("/bin/bash");
        properties.setDefaultTimeoutSeconds(600);
        properties.setDefaultLogOutput(false);

        ScheduledScript job = new ScheduledScript();
        job.setName("test-job");
        job.setScript(scriptPath.toString());
        job.setCron("0 0 * * * ?");
        // Don't set interpreter, timeout, or logOutput - should use defaults

        List<ScheduledScript> jobs = new ArrayList<>();
        jobs.add(job);
        properties.setJobs(jobs);

        service = new ScriptSchedulerService(taskScheduler, scriptExecutor, properties);
        service.init();

        // Verify defaults were applied
        ScheduledScript processedJob = properties.getJobs().get(0);
        assertThat(processedJob.getInterpreter()).isEqualTo("/bin/bash");
        assertThat(processedJob.getTimeoutSeconds()).isEqualTo(600);
        assertThat(processedJob.getLogOutput()).isFalse();
    }

    @Test
    void shouldCheckIfScriptIsRunning() throws IOException {
        Path scriptPath = createTestScript("test.sh");
        ScheduledScript job = createValidJob("test-job", scriptPath.toString());

        List<ScheduledScript> jobs = new ArrayList<>();
        jobs.add(job);
        properties.setJobs(jobs);

        service = new ScriptSchedulerService(taskScheduler, scriptExecutor, properties);
        service.init();

        // Initially not running
        assertThat(service.isScriptRunning("test-job")).isFalse();

        // Test non-existent script
        assertThat(service.isScriptRunning("non-existent")).isFalse();
    }

    @Test
    void shouldShutdownGracefully() throws IOException {
        Path scriptPath = createTestScript("test.sh");
        ScheduledScript job = createValidJob("test-job", scriptPath.toString());

        List<ScheduledScript> jobs = new ArrayList<>();
        jobs.add(job);
        properties.setJobs(jobs);

        when(scheduledFuture.isCancelled()).thenReturn(false);
        when(scheduledFuture.cancel(false)).thenReturn(true);

        service = new ScriptSchedulerService(taskScheduler, scriptExecutor, properties);
        service.init();
        service.shutdown();

        // Verify scheduled tasks were cancelled
        verify(scheduledFuture).cancel(false);

        assertThat(service.getScheduledScriptCount()).isZero();
        assertThat(service.getScheduledScriptNames()).isEmpty();
    }

    @Test
    void shouldHandleJobWithDescription() throws IOException {
        Path scriptPath = createTestScript("test.sh");

        ScheduledScript job = createValidJob("described-job", scriptPath.toString());
        job.setDescription("This is a test job description");

        List<ScheduledScript> jobs = new ArrayList<>();
        jobs.add(job);
        properties.setJobs(jobs);

        service = new ScriptSchedulerService(taskScheduler, scriptExecutor, properties);
        service.init();

        verify(taskScheduler).schedule(any(Runnable.class), any(CronTrigger.class));
        assertThat(service.getScheduledScriptCount()).isEqualTo(1);
    }

    @Test
    void shouldHandleJobWithEnvironmentVariables() throws IOException {
        Path scriptPath = createTestScript("test.sh");

        ScheduledScript job = createValidJob("env-job", scriptPath.toString());
        job.getEnvironment().put("TEST_VAR", "test_value");
        job.getEnvironment().put("ANOTHER_VAR", "another_value");

        List<ScheduledScript> jobs = new ArrayList<>();
        jobs.add(job);
        properties.setJobs(jobs);

        service = new ScriptSchedulerService(taskScheduler, scriptExecutor, properties);
        service.init();

        verify(taskScheduler).schedule(any(Runnable.class), any(CronTrigger.class));
        assertThat(service.getScheduledScriptCount()).isEqualTo(1);
    }

    @Test
    void shouldHandleJobWithRetryConfiguration() throws IOException {
        Path scriptPath = createTestScript("test.sh");

        ScheduledScript job = createValidJob("retry-job", scriptPath.toString());
        job.setRetryAttempts(3);
        job.setRetryDelaySeconds(10);

        List<ScheduledScript> jobs = new ArrayList<>();
        jobs.add(job);
        properties.setJobs(jobs);

        service = new ScriptSchedulerService(taskScheduler, scriptExecutor, properties);
        service.init();

        verify(taskScheduler).schedule(any(Runnable.class), any(CronTrigger.class));
        assertThat(service.getScheduledScriptCount()).isEqualTo(1);
    }

    @Test
    void shouldSkipJobWithEmptyScriptPath() {
        ScheduledScript job = new ScheduledScript();
        job.setName("empty-script-job");
        job.setScript("");  // Empty script path
        job.setCron("0 0 * * * ?");

        List<ScheduledScript> jobs = new ArrayList<>();
        jobs.add(job);
        properties.setJobs(jobs);

        service = new ScriptSchedulerService(taskScheduler, scriptExecutor, properties);
        service.init();

        // Should not schedule job with empty script path
        verify(taskScheduler, never()).schedule(any(Runnable.class), any(CronTrigger.class));
        assertThat(service.getScheduledScriptCount()).isZero();
    }

    @Test
    void shouldHandleJobWithWorkingDirectory() throws IOException {
        Path scriptPath = createTestScript("test.sh");
        Path workDir = tempDir.resolve("workdir");
        Files.createDirectory(workDir);

        ScheduledScript job = createValidJob("workdir-job", scriptPath.toString());
        job.setWorkingDirectory(workDir.toString());

        List<ScheduledScript> jobs = new ArrayList<>();
        jobs.add(job);
        properties.setJobs(jobs);

        service = new ScriptSchedulerService(taskScheduler, scriptExecutor, properties);
        service.init();

        verify(taskScheduler).schedule(any(Runnable.class), any(CronTrigger.class));
        assertThat(service.getScheduledScriptCount()).isEqualTo(1);
    }

    private ScheduledScript createValidJob(String name, String scriptPath) {
        ScheduledScript job = new ScheduledScript();
        job.setName(name);
        job.setScript(scriptPath);
        job.setCron("0 0 * * * ?");
        return job;
    }

    private Path createTestScript(String name) throws IOException {
        Path scriptPath = tempDir.resolve(name);
        Files.writeString(scriptPath, "#!/bin/sh\nexit 0");

        // Make executable on Unix-like systems
        try {
            Set<PosixFilePermission> perms = Files.getPosixFilePermissions(scriptPath);
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            Files.setPosixFilePermissions(scriptPath, perms);
        } catch (UnsupportedOperationException e) {
            // Windows doesn't support POSIX permissions
        }

        return scriptPath;
    }
}
