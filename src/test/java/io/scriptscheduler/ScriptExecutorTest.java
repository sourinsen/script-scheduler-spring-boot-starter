package io.scriptscheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ScriptExecutor}.
 *
 * @author Sourin Kumar Sen
 * @since 1.0.0
 */
class ScriptExecutorTest {

    @TempDir
    Path tempDir;

    private ScriptExecutor scriptExecutor;
    private ScriptSchedulerProperties properties;

    @BeforeEach
    void setUp() {
        properties = new ScriptSchedulerProperties();
        scriptExecutor = new ScriptExecutor(properties);
    }

    @Test
    void shouldExecuteSimpleScript() throws IOException {
        // Create a simple script that exits successfully
        Path scriptPath = createScript("test.sh",
            "#!/bin/sh\n" +
            "echo 'Hello from test script'\n" +
            "exit 0"
        );

        ScheduledScript script = new ScheduledScript();
        script.setName("test-script");
        script.setScript(scriptPath.toString());
        script.setCron("0 0 * * * ?");
        script.setLogOutput(true);

        int exitCode = scriptExecutor.execute(script);

        assertThat(exitCode).isZero();
    }

    @Test
    void shouldHandleScriptWithNonZeroExit() throws IOException {
        // Create a script that exits with error
        Path scriptPath = createScript("error.sh",
            "#!/bin/sh\n" +
            "echo 'Error occurred'\n" +
            "exit 1"
        );

        ScheduledScript script = new ScheduledScript();
        script.setName("error-script");
        script.setScript(scriptPath.toString());
        script.setCron("0 0 * * * ?");

        int exitCode = scriptExecutor.execute(script);

        assertThat(exitCode).isEqualTo(1);
    }

    @Test
    void shouldHandleScriptTimeout() throws IOException {
        // Create a script that sleeps longer than timeout
        Path scriptPath = createScript("timeout.sh",
            "#!/bin/sh\n" +
            "sleep 10\n" +
            "echo 'Should not see this'\n"
        );

        ScheduledScript script = new ScheduledScript();
        script.setName("timeout-script");
        script.setScript(scriptPath.toString());
        script.setCron("0 0 * * * ?");
        script.setTimeoutSeconds(1); // 1 second timeout

        int exitCode = scriptExecutor.execute(script);

        assertThat(exitCode).isEqualTo(-1);
    }

    @Test
    void shouldHandleScriptNotFound() {
        ScheduledScript script = new ScheduledScript();
        script.setName("missing-script");
        script.setScript("/non/existent/script.sh");
        script.setCron("0 0 * * * ?");

        int exitCode = scriptExecutor.execute(script);

        assertThat(exitCode).isEqualTo(-1);
    }

    @Test
    void shouldUseCustomInterpreter() throws IOException {
        // Create a shell script that uses bash-specific features
        Path scriptPath = createScript("test.sh",
            "#!/bin/bash\n" +
            "echo 'Testing custom interpreter'\n" +
            "exit 0"
        );

        ScheduledScript script = new ScheduledScript();
        script.setName("bash-script");
        script.setScript(scriptPath.toString());
        script.setCron("0 0 * * * ?");
        script.setInterpreter("/bin/bash"); // Use bash as custom interpreter

        int exitCode = scriptExecutor.execute(script);

        assertThat(exitCode).isZero();
    }

    @Test
    void shouldSetEnvironmentVariables() throws IOException {
        // Create a script that uses environment variables
        Path scriptPath = createScript("env.sh",
            "#!/bin/sh\n" +
            "if [ \"$TEST_VAR\" = \"test_value\" ]; then\n" +
            "  exit 0\n" +
            "else\n" +
            "  exit 1\n" +
            "fi"
        );

        Map<String, String> env = new HashMap<>();
        env.put("TEST_VAR", "test_value");

        ScheduledScript script = new ScheduledScript();
        script.setName("env-script");
        script.setScript(scriptPath.toString());
        script.setCron("0 0 * * * ?");
        script.setEnvironment(env);

        int exitCode = scriptExecutor.execute(script);

        assertThat(exitCode).isZero();
    }

    @Test
    void shouldUseWorkingDirectory() throws IOException {
        // Create a subdirectory
        Path workDir = tempDir.resolve("workdir");
        Files.createDirectory(workDir);

        // Create a marker file in the working directory
        Path markerFile = workDir.resolve("marker.txt");
        Files.writeString(markerFile, "test");

        // Create a script that checks for the marker file
        Path scriptPath = createScript("workdir.sh",
            "#!/bin/sh\n" +
            "if [ -f marker.txt ]; then\n" +
            "  exit 0\n" +
            "else\n" +
            "  exit 1\n" +
            "fi"
        );

        ScheduledScript script = new ScheduledScript();
        script.setName("workdir-script");
        script.setScript(scriptPath.toString());
        script.setCron("0 0 * * * ?");
        script.setWorkingDirectory(workDir.toString());

        int exitCode = scriptExecutor.execute(script);

        assertThat(exitCode).isZero();
    }

    @Test
    void shouldHandleRetryLogic() throws IOException {
        // Create a script that fails
        Path scriptPath = createScript("retry.sh",
            "#!/bin/sh\n" +
            "exit 1"
        );

        ScheduledScript script = new ScheduledScript();
        script.setName("retry-script");
        script.setScript(scriptPath.toString());
        script.setCron("0 0 * * * ?");
        script.setRetryAttempts(2);
        script.setRetryDelaySeconds(1);

        long startTime = System.currentTimeMillis();
        int exitCode = scriptExecutor.execute(script);
        long duration = System.currentTimeMillis() - startTime;

        assertThat(exitCode).isEqualTo(1);
        // Should have retried twice with 1 second delay each
        assertThat(duration).isGreaterThanOrEqualTo(2000);
    }

    @Test
    void shouldSucceedOnRetry() throws IOException {
        // Create a script that checks for a file that we'll create after first attempt
        Path markerFile = tempDir.resolve("retry-marker.txt");
        Path scriptPath = createScript("retry-success.sh",
            "#!/bin/sh\n" +
            "if [ -f " + markerFile.toString() + " ]; then\n" +
            "  exit 0\n" +
            "else\n" +
            "  # Create the file for next attempt\n" +
            "  touch " + markerFile.toString() + "\n" +
            "  exit 1\n" +
            "fi"
        );

        ScheduledScript script = new ScheduledScript();
        script.setName("retry-success-script");
        script.setScript(scriptPath.toString());
        script.setCron("0 0 * * * ?");
        script.setRetryAttempts(2);
        script.setRetryDelaySeconds(1);

        int exitCode = scriptExecutor.execute(script);

        assertThat(exitCode).isZero();
        assertThat(markerFile).exists();
    }

    @Test
    void shouldUseDefaultTimeout() throws IOException {
        properties.setDefaultTimeoutSeconds(2);
        scriptExecutor = new ScriptExecutor(properties);

        // Create a script that sleeps
        Path scriptPath = createScript("default-timeout.sh",
            "#!/bin/sh\n" +
            "sleep 5\n"
        );

        ScheduledScript script = new ScheduledScript();
        script.setName("default-timeout-script");
        script.setScript(scriptPath.toString());
        script.setCron("0 0 * * * ?");
        // Don't set timeout, should use default

        long startTime = System.currentTimeMillis();
        int exitCode = scriptExecutor.execute(script);
        long duration = System.currentTimeMillis() - startTime;

        assertThat(exitCode).isEqualTo(-1);
        assertThat(duration).isLessThan(4000); // Should timeout at 2 seconds
    }

    @Test
    void shouldUseDefaultInterpreter() throws IOException {
        properties.setDefaultInterpreter("/bin/sh");
        scriptExecutor = new ScriptExecutor(properties);

        Path scriptPath = createScript("default-interpreter.sh",
            "#!/bin/sh\n" +
            "exit 0"
        );

        ScheduledScript script = new ScheduledScript();
        script.setName("default-interpreter-script");
        script.setScript(scriptPath.toString());
        script.setCron("0 0 * * * ?");
        // Don't set interpreter, should use default

        int exitCode = scriptExecutor.execute(script);

        assertThat(exitCode).isZero();
    }

    @Test
    void shouldHandleScriptWithOutput() throws IOException {
        Path scriptPath = createScript("output.sh",
            "#!/bin/sh\n" +
            "echo 'Standard output line 1'\n" +
            "echo 'Standard output line 2'\n" +
            "echo 'Error output' >&2\n" +
            "exit 0"
        );

        ScheduledScript script = new ScheduledScript();
        script.setName("output-script");
        script.setScript(scriptPath.toString());
        script.setCron("0 0 * * * ?");
        script.setLogOutput(true);

        int exitCode = scriptExecutor.execute(script);

        assertThat(exitCode).isZero();
    }

    @Test
    void shouldNotLogOutputWhenDisabled() throws IOException {
        Path scriptPath = createScript("no-output.sh",
            "#!/bin/sh\n" +
            "echo 'Should not be logged'\n" +
            "exit 0"
        );

        ScheduledScript script = new ScheduledScript();
        script.setName("no-output-script");
        script.setScript(scriptPath.toString());
        script.setCron("0 0 * * * ?");
        script.setLogOutput(false);

        int exitCode = scriptExecutor.execute(script);

        assertThat(exitCode).isZero();
    }

    private Path createScript(String name, String content) throws IOException {
        Path scriptPath = tempDir.resolve(name);
        Files.writeString(scriptPath, content);

        // Make script executable on Unix-like systems
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
