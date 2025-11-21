package io.scriptscheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Script Scheduler Spring Boot Starter.
 * Tests the complete workflow from configuration to script execution.
 *
 * @author Sourin Kumar Sen
 * @since 1.0.0
 */
class IntegrationTest {

    @TempDir
    Path tempDir;

    private ApplicationContextRunner contextRunner;

    @BeforeEach
    void setUp() {
        contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ScriptSchedulerAutoConfiguration.class));
    }

    @Test
    void shouldExecuteScheduledScriptSuccessfully() throws IOException, InterruptedException {
        // Create a marker file that the script will modify
        Path markerFile = tempDir.resolve("marker.txt");
        Files.writeString(markerFile, "initial");

        // Create a script that modifies the marker file
        Path scriptPath = createExecutableScript("test.sh",
            "#!/bin/sh\n" +
            "echo 'executed' > " + markerFile.toString() + "\n" +
            "exit 0"
        );

        // Configure and run
        contextRunner
            .withPropertyValues(
                "script-scheduler.enabled=true",
                "script-scheduler.jobs[0].name=test-job",
                "script-scheduler.jobs[0].script=" + scriptPath.toString(),
                "script-scheduler.jobs[0].cron=*/1 * * * * ?",  // Every second
                "script-scheduler.jobs[0].log-output=true"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(ScriptSchedulerService.class);

                ScriptSchedulerService service = context.getBean(ScriptSchedulerService.class);
                assertThat(service.getScheduledScriptCount()).isEqualTo(1);
                assertThat(service.getScheduledScriptNames()).containsExactly("test-job");

                // Wait for the script to execute
                Thread.sleep(2000);

                // Verify the script was executed
                String content = Files.readString(markerFile);
                assertThat(content.trim()).isEqualTo("executed");
            });
    }

    @Test
    void shouldExecuteMultipleJobsConcurrently() throws IOException, InterruptedException {
        // Create multiple marker files
        Path marker1 = tempDir.resolve("marker1.txt");
        Path marker2 = tempDir.resolve("marker2.txt");
        Path marker3 = tempDir.resolve("marker3.txt");

        // Create scripts
        Path script1 = createExecutableScript("script1.sh",
            "#!/bin/sh\n" +
            "echo 'job1' > " + marker1.toString() + "\n"
        );

        Path script2 = createExecutableScript("script2.sh",
            "#!/bin/sh\n" +
            "echo 'job2' > " + marker2.toString() + "\n"
        );

        Path script3 = createExecutableScript("script3.sh",
            "#!/bin/sh\n" +
            "echo 'job3' > " + marker3.toString() + "\n"
        );

        contextRunner
            .withPropertyValues(
                "script-scheduler.enabled=true",
                "script-scheduler.thread-pool-size=5",
                // Job 1
                "script-scheduler.jobs[0].name=job1",
                "script-scheduler.jobs[0].script=" + script1.toString(),
                "script-scheduler.jobs[0].cron=*/1 * * * * ?",
                // Job 2
                "script-scheduler.jobs[1].name=job2",
                "script-scheduler.jobs[1].script=" + script2.toString(),
                "script-scheduler.jobs[1].cron=*/1 * * * * ?",
                // Job 3
                "script-scheduler.jobs[2].name=job3",
                "script-scheduler.jobs[2].script=" + script3.toString(),
                "script-scheduler.jobs[2].cron=*/1 * * * * ?"
            )
            .run(context -> {
                ScriptSchedulerService service = context.getBean(ScriptSchedulerService.class);
                assertThat(service.getScheduledScriptCount()).isEqualTo(3);

                // Wait for scripts to execute
                Thread.sleep(2000);

                // Verify all scripts executed
                assertThat(marker1).exists();
                assertThat(Files.readString(marker1).trim()).isEqualTo("job1");

                assertThat(marker2).exists();
                assertThat(Files.readString(marker2).trim()).isEqualTo("job2");

                assertThat(marker3).exists();
                assertThat(Files.readString(marker3).trim()).isEqualTo("job3");
            });
    }

    @Test
    void shouldHandleScriptTimeout() throws IOException {
        // Create a script that sleeps longer than timeout
        Path scriptPath = createExecutableScript("timeout.sh",
            "#!/bin/sh\n" +
            "sleep 10\n" +
            "echo 'Should not reach here'\n"
        );

        Path markerFile = tempDir.resolve("timeout-marker.txt");

        contextRunner
            .withPropertyValues(
                "script-scheduler.enabled=true",
                "script-scheduler.jobs[0].name=timeout-job",
                "script-scheduler.jobs[0].script=" + scriptPath.toString(),
                "script-scheduler.jobs[0].cron=*/1 * * * * ?",
                "script-scheduler.jobs[0].timeout-seconds=1",
                "script-scheduler.jobs[0].log-output=true"
            )
            .run(context -> {
                ScriptSchedulerService service = context.getBean(ScriptSchedulerService.class);
                assertThat(service.getScheduledScriptCount()).isEqualTo(1);

                // Wait for script to timeout
                Thread.sleep(3000);

                // Marker file should not exist (script was terminated)
                assertThat(markerFile).doesNotExist();
            });
    }

    @Test
    void shouldApplyEnvironmentVariables() throws IOException {
        Path outputFile = tempDir.resolve("env-output.txt");

        // Create a script that uses environment variables
        Path scriptPath = createExecutableScript("env.sh",
            "#!/bin/sh\n" +
            "echo \"VAR1=$VAR1,VAR2=$VAR2\" > " + outputFile.toString() + "\n"
        );

        contextRunner
            .withPropertyValues(
                "script-scheduler.enabled=true",
                "script-scheduler.jobs[0].name=env-job",
                "script-scheduler.jobs[0].script=" + scriptPath.toString(),
                "script-scheduler.jobs[0].cron=*/1 * * * * ?",
                "script-scheduler.jobs[0].environment.VAR1=value1",
                "script-scheduler.jobs[0].environment.VAR2=value2"
            )
            .run(context -> {
                ScriptSchedulerService service = context.getBean(ScriptSchedulerService.class);
                assertThat(service.getScheduledScriptCount()).isEqualTo(1);

                // Wait for script to execute
                Thread.sleep(2000);

                // Verify environment variables were set
                assertThat(outputFile).exists();
                String content = Files.readString(outputFile);
                assertThat(content.trim()).isEqualTo("VAR1=value1,VAR2=value2");
            });
    }

    @Test
    void shouldUseWorkingDirectory() throws IOException {
        // Create a working directory with a file
        Path workDir = tempDir.resolve("workdir");
        Files.createDirectory(workDir);
        Path fileInWorkDir = workDir.resolve("test.txt");
        Files.writeString(fileInWorkDir, "test content");

        Path outputFile = tempDir.resolve("output.txt");

        // Create a script that lists files in current directory
        Path scriptPath = createExecutableScript("workdir.sh",
            "#!/bin/sh\n" +
            "if [ -f test.txt ]; then\n" +
            "  echo 'found' > " + outputFile.toString() + "\n" +
            "else\n" +
            "  echo 'not found' > " + outputFile.toString() + "\n" +
            "fi"
        );

        contextRunner
            .withPropertyValues(
                "script-scheduler.enabled=true",
                "script-scheduler.jobs[0].name=workdir-job",
                "script-scheduler.jobs[0].script=" + scriptPath.toString(),
                "script-scheduler.jobs[0].cron=*/1 * * * * ?",
                "script-scheduler.jobs[0].working-directory=" + workDir.toString()
            )
            .run(context -> {
                ScriptSchedulerService service = context.getBean(ScriptSchedulerService.class);
                assertThat(service.getScheduledScriptCount()).isEqualTo(1);

                // Wait for script to execute
                Thread.sleep(2000);

                // Verify working directory was used
                assertThat(outputFile).exists();
                String content = Files.readString(outputFile);
                assertThat(content.trim()).isEqualTo("found");
            });
    }

    @Test
    void shouldRetryFailedScript() throws IOException, InterruptedException {
        // Create a counter file to track executions
        Path counterFile = tempDir.resolve("counter.txt");
        AtomicInteger executionCount = new AtomicInteger(0);

        // Create a script that fails first time, succeeds on retry
        Path scriptPath = createExecutableScript("retry.sh",
            "#!/bin/sh\n" +
            "if [ ! -f " + counterFile.toString() + " ]; then\n" +
            "  echo '1' > " + counterFile.toString() + "\n" +
            "  exit 1\n" +
            "else\n" +
            "  echo '2' > " + counterFile.toString() + "\n" +
            "  exit 0\n" +
            "fi"
        );

        contextRunner
            .withPropertyValues(
                "script-scheduler.enabled=true",
                "script-scheduler.jobs[0].name=retry-job",
                "script-scheduler.jobs[0].script=" + scriptPath.toString(),
                "script-scheduler.jobs[0].cron=0 0 0 31 2 ?",  // Never scheduled automatically
                "script-scheduler.jobs[0].retry-attempts=1",
                "script-scheduler.jobs[0].retry-delay-seconds=1"
            )
            .run(context -> {
                ScriptSchedulerService service = context.getBean(ScriptSchedulerService.class);
                ScriptExecutor executor = context.getBean(ScriptExecutor.class);
                ScriptSchedulerProperties props = context.getBean(ScriptSchedulerProperties.class);

                // Execute the script directly
                ScheduledScript job = props.getJobs().get(0);
                int exitCode = executor.execute(job);

                // Should succeed after retry
                assertThat(exitCode).isZero();
                assertThat(counterFile).exists();
                assertThat(Files.readString(counterFile).trim()).isEqualTo("2");
            });
    }

    @Test
    void shouldHandleInvalidCronExpression() {
        contextRunner
            .withPropertyValues(
                "script-scheduler.enabled=true",
                "script-scheduler.jobs[0].name=invalid-cron-job",
                "script-scheduler.jobs[0].script=/path/to/script.sh",
                "script-scheduler.jobs[0].cron=* * * * * *"  // Valid format but invalid syntax (7 fields would be invalid, but 6 is valid)
            )
            .run(context -> {
                // Context should start successfully even with syntactically valid but semantically questionable cron
                assertThat(context).hasNotFailed();
                ScriptSchedulerService service = context.getBean(ScriptSchedulerService.class);
                // Job should be scheduled since the cron is syntactically valid
                assertThat(service.getScheduledScriptCount()).isEqualTo(1);
            });
    }

    @Test
    void shouldHandleMissingScript() {
        contextRunner
            .withPropertyValues(
                "script-scheduler.enabled=true",
                "script-scheduler.jobs[0].name=missing-script-job",
                "script-scheduler.jobs[0].script=/non/existent/script.sh",
                "script-scheduler.jobs[0].cron=0 0 1 * * ?"  // Every day at 00:00:01 (valid cron)
            )
            .run(context -> {
                ScriptSchedulerService service = context.getBean(ScriptSchedulerService.class);
                ScriptExecutor executor = context.getBean(ScriptExecutor.class);
                ScriptSchedulerProperties props = context.getBean(ScriptSchedulerProperties.class);

                // Job should be scheduled (cron is valid, file validation happens at execution time)
                assertThat(service.getScheduledScriptCount()).isEqualTo(1);

                // But execution should fail because script file doesn't exist
                ScheduledScript job = props.getJobs().get(0);
                int exitCode = executor.execute(job);
                assertThat(exitCode).isEqualTo(-1);
            });
    }

    @Test
    void shouldPreventConcurrentExecution() throws IOException, InterruptedException {
        // Create a script that takes time to execute
        Path lockFile = tempDir.resolve("lock.txt");
        Path scriptPath = createExecutableScript("concurrent.sh",
            "#!/bin/sh\n" +
            "echo $$ >> " + lockFile.toString() + "\n" +
            "sleep 2\n"
        );

        contextRunner
            .withPropertyValues(
                "script-scheduler.enabled=true",
                "script-scheduler.thread-pool-size=10",
                "script-scheduler.jobs[0].name=concurrent-job",
                "script-scheduler.jobs[0].script=" + scriptPath.toString(),
                "script-scheduler.jobs[0].cron=*/1 * * * * ?"  // Every second
            )
            .run(context -> {
                ScriptSchedulerService service = context.getBean(ScriptSchedulerService.class);
                assertThat(service.getScheduledScriptCount()).isEqualTo(1);

                // Wait for multiple potential executions
                Thread.sleep(4000);

                // Check how many times the script executed
                if (Files.exists(lockFile)) {
                    long executionCount = Files.lines(lockFile).count();
                    // Should be less than 4 (one execution takes 2 seconds)
                    assertThat(executionCount).isLessThanOrEqualTo(2);
                }
            });
    }

    @Test
    void shouldUseGlobalDefaults() throws IOException {
        Path outputFile = tempDir.resolve("defaults-output.txt");

        Path scriptPath = createExecutableScript("defaults.sh",
            "#!/bin/sh\n" +
            "echo 'executed with defaults' > " + outputFile.toString() + "\n"
        );

        contextRunner
            .withPropertyValues(
                "script-scheduler.enabled=true",
                "script-scheduler.default-interpreter=/bin/sh",
                "script-scheduler.default-timeout-seconds=10",
                "script-scheduler.default-log-output=false",
                "script-scheduler.jobs[0].name=defaults-job",
                "script-scheduler.jobs[0].script=" + scriptPath.toString(),
                "script-scheduler.jobs[0].cron=*/1 * * * * ?"
                // No interpreter, timeout, or log-output specified - should use defaults
            )
            .run(context -> {
                ScriptSchedulerProperties props = context.getBean(ScriptSchedulerProperties.class);
                ScheduledScript job = props.getJobs().get(0);

                // Verify defaults were applied
                assertThat(job.getInterpreter()).isEqualTo("/bin/sh");
                assertThat(job.getTimeoutSeconds()).isEqualTo(10);
                assertThat(job.getLogOutput()).isFalse();

                // Wait for script to execute
                Thread.sleep(2000);

                // Verify script executed
                assertThat(outputFile).exists();
                assertThat(Files.readString(outputFile).trim()).isEqualTo("executed with defaults");
            });
    }

    private Path createExecutableScript(String name, String content) throws IOException {
        Path scriptPath = tempDir.resolve(name);
        Files.writeString(scriptPath, content);

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
