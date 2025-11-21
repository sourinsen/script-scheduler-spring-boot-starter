# Script Scheduler Spring Boot Starter

[![Maven Central](https://img.shields.io/maven-central/v/io.github.sourinsen/script-scheduler-spring-boot-starter.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.sourinsen%22%20AND%20a:%22script-scheduler-spring-boot-starter%22)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-21%2B-blue)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2%2B-green)](https://spring.io/projects/spring-boot)

A production-ready Spring Boot starter that enables scheduling and execution of shell scripts through simple YAML configuration—no Java code required!

## Features

- 🚀 **Zero-Code Configuration**: Schedule scripts using only YAML/properties
- ⏰ **Cron-Based Scheduling**: Flexible scheduling with standard cron expressions
- 🔄 **Retry Logic**: Automatic retry with configurable attempts and delays
- ⚙️ **Environment Variables**: Inject custom environment variables per script
- 📁 **Working Directory**: Configure working directory for each script
- ⏱️ **Timeout Management**: Prevent runaway scripts with configurable timeouts
- 🔧 **Custom Interpreters**: Use any interpreter (bash, python, node, etc.)
- 📝 **Comprehensive Logging**: Detailed execution logs with stdout/stderr capture
- 🛡️ **Thread Safety**: Prevents concurrent execution of the same script
- 🔌 **Spring Boot Integration**: Seamless integration with Spring Boot ecosystem
- 🎯 **Production Ready**: Graceful shutdown, resource cleanup, and error handling

## Quick Start

### 1. Add Dependency

#### Maven
```xml
<dependency>
    <groupId>io.github.sourinsen</groupId>
    <artifactId>script-scheduler-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### Gradle
```gradle
implementation 'io.github.sourinsen:script-scheduler-spring-boot-starter:1.0.0'
```

### 2. Configure Your Scripts

Add configuration to your `application.yml`:

```yaml
script-scheduler:
  enabled: true
  jobs:
    - name: daily-backup
      script: /opt/scripts/backup.sh
      cron: "0 0 2 * * ?"  # Every day at 2 AM
      description: "Daily database backup"
```

### 3. Run Your Application

That's it! Your scripts will be automatically scheduled when your Spring Boot application starts.

## Configuration

### Basic Configuration

```yaml
script-scheduler:
  enabled: true                    # Enable/disable the scheduler (default: true)
  thread-pool-size: 10             # Thread pool size for parallel execution (default: 10)
  shutdown-timeout-seconds: 60     # Graceful shutdown timeout (default: 60)
  default-timeout-seconds: 300     # Default script timeout (default: 300)
  default-interpreter: /bin/bash   # Default script interpreter (default: /bin/sh)
  default-log-output: true         # Log script output by default (default: true)
```

### Job Configuration

Each job supports the following properties:

| Property | Required | Description | Default |
|----------|----------|-------------|---------|
| `name` | Yes | Unique job identifier | - |
| `script` | Yes | Path to the script file | - |
| `cron` | Yes | Cron expression (6 fields) | - |
| `description` | No | Job description | - |
| `interpreter` | No | Script interpreter | Uses default |
| `working-directory` | No | Working directory for script | Current directory |
| `timeout-seconds` | No | Script timeout in seconds | Uses default |
| `log-output` | No | Log script output | Uses default |
| `retry-attempts` | No | Number of retry attempts | 0 |
| `retry-delay-seconds` | No | Delay between retries | 10 |
| `environment` | No | Environment variables | - |

## Examples by Script Type

This section shows how to configure different types of scripts with the scheduler.

### Bash/Shell Scripts

```yaml
script-scheduler:
  jobs:
    - name: system-monitor
      script: /opt/scripts/monitor.sh
      interpreter: /bin/bash
      cron: "*/5 * * * * ?"  # Every 5 minutes
      description: "System monitoring"
      log-output: true
      timeout-seconds: 60
      environment:
        ALERT_EMAIL: "admin@example.com"
        CPU_THRESHOLD: "90"
        MEMORY_THRESHOLD: "85"
```

### Python Scripts

```yaml
script-scheduler:
  jobs:
    - name: data-processor
      script: /opt/scripts/process_data.py
      interpreter: /usr/bin/python3
      cron: "0 */15 * * * ?"  # Every 15 minutes
      description: "Data processing"
      log-output: true
      timeout-seconds: 300
      retry-attempts: 2
      retry-delay-seconds: 30
      environment:
        PYTHONUNBUFFERED: "1"
        INPUT_DIR: "/data/input"
        OUTPUT_DIR: "/data/output"
```

### Java Programs

```yaml
script-scheduler:
  jobs:
    - name: report-generator
      script: "cd /opt/scripts && java ReportGenerator --type daily"
      interpreter: /bin/sh
      cron: "0 0 2 * * ?"  # Daily at 2 AM
      description: "Daily report generation"
      log-output: true
      timeout-seconds: 120
      environment:
        JAVA_HOME: "/usr/lib/jvm/java-21"
        OUTPUT_DIR: "/opt/reports"
```

### Node.js Scripts

```yaml
script-scheduler:
  jobs:
    - name: api-health-check
      script: /opt/scripts/health_check.js
      interpreter: /usr/bin/node
      cron: "*/2 * * * * ?"  # Every 2 minutes
      description: "API health monitoring"
      log-output: true
      timeout-seconds: 60
      environment:
        NODE_ENV: "production"
        API_ENDPOINTS: "https://api.example.com/health,https://api2.example.com/status"
        TIMEOUT: "5000"
```

### PowerShell Scripts

```yaml
script-scheduler:
  jobs:
    - name: service-monitor
      script: /opt/scripts/monitor_services.ps1
      interpreter: /usr/local/bin/pwsh
      cron: "0 */10 * * * ?"  # Every 10 minutes
      description: "Service monitoring"
      log-output: true
      timeout-seconds: 120
      environment:
        SERVICES: "nginx,mysql,redis"
        ACTION: "restart"
```

### Windows Batch Scripts

```yaml
script-scheduler:
  jobs:
    - name: backup
      script: C:\\scripts\\backup.bat
      interpreter: cmd.exe
      cron: "0 0 3 * * ?"  # Daily at 3 AM
      description: "Automated backup"
      log-output: true
      timeout-seconds: 3600
      environment:
        BACKUP_SOURCE: "C:\\\\data"
        BACKUP_DEST: "D:\\\\backups"
        RETENTION_DAYS: "7"
```

### Ruby Scripts

```yaml
script-scheduler:
  jobs:
    - name: log-analyzer
      script: /opt/scripts/analyze_logs.rb
      interpreter: /usr/bin/ruby
      cron: "0 */30 * * * ?"  # Every 30 minutes
      description: "Log analysis"
      log-output: true
      timeout-seconds: 300
      environment:
        LOG_DIR: "/var/log/app"
        REPORT_FORMAT: "json"
        ERROR_THRESHOLD: "100"
```

### Perl Scripts

```yaml
script-scheduler:
  jobs:
    - name: text-processor
      script: /opt/scripts/process_text.pl
      interpreter: /usr/bin/perl
      cron: "0 0 * * * ?"  # Hourly
      description: "Text file processing"
      log-output: true
      timeout-seconds: 600
      environment:
        INPUT_DIR: "/data/input"
        OUTPUT_DIR: "/data/output"
        PROCESS_MODE: "clean"
```

### Multi-Step Pipelines

```yaml
script-scheduler:
  jobs:
    - name: etl-pipeline
      script: /opt/scripts/etl_pipeline.sh
      interpreter: /bin/bash
      cron: "0 0 1 * * ?"  # Daily at 1 AM
      description: "ETL data pipeline"
      log-output: true
      timeout-seconds: 3600
      retry-attempts: 2
      environment:
        PIPELINE_NAME: "Production ETL"
        DATA_DIR: "/data/etl"
        PYTHON_BIN: "/usr/bin/python3"
        JAVA_BIN: "/usr/bin/java"
        NODE_BIN: "/usr/bin/node"
```

### Docker Container Execution

```yaml
script-scheduler:
  jobs:
    - name: containerized-task
      script: /opt/scripts/run_container.sh
      interpreter: /bin/bash
      cron: "0 */6 * * * ?"  # Every 6 hours
      description: "Containerized data processing"
      log-output: true
      timeout-seconds: 600
      environment:
        DOCKER_IMAGE: "python:3.11-slim"
        CONTAINER_NAME: "data-processor"
        MEMORY_LIMIT: "512m"
        CPU_LIMIT: "1.0"
```

---

## Cron Expression Format

The scheduler uses Spring's cron format with 6 fields:

```
┌───────────── second (0-59)
│ ┌───────────── minute (0-59)
│ │ ┌───────────── hour (0-23)
│ │ │ ┌───────────── day of month (1-31)
│ │ │ │ ┌───────────── month (1-12 or JAN-DEC)
│ │ │ │ │ ┌───────────── day of week (0-7 or SUN-SAT)
│ │ │ │ │ │
* * * * * *
```

### Common Patterns

- `0 0 * * * ?` - Every hour
- `0 0 0 * * ?` - Every day at midnight
- `0 0 2 * * ?` - Every day at 2 AM
- `0 0 0 * * MON` - Every Monday at midnight
- `0 */5 * * * ?` - Every 5 minutes
- `0 0 9-17 * * MON-FRI` - Every hour 9 AM to 5 PM on weekdays

## Monitoring and Management

### Logging

The starter uses SLF4J for logging. Configure logging levels in your `application.yml`:

```yaml
logging:
  level:
    io.scriptscheduler: DEBUG
```

### Log Output Example

```
INFO  ScriptSchedulerService : Scheduling script 'daily-backup' with cron expression: 0 0 2 * * ?
INFO  ScriptSchedulerService : Script 'daily-backup' will first run at: 2024-01-15T02:00:00
INFO  ScriptExecutor         : Executing script: daily-backup with interpreter: /bin/bash and timeout: 3600s
INFO  ScriptExecutor         : [daily-backup] STDOUT: Backup started...
INFO  ScriptExecutor         : [daily-backup] STDOUT: Files compressed successfully
INFO  ScriptExecutor         : Script 'daily-backup' completed successfully in 145230 ms
```

### Metrics

When using Spring Boot Actuator, script execution metrics are available:

- Execution count per script
- Success/failure rates
- Average execution time
- Last execution status

## Error Handling

### Script Failures

Failed scripts are logged with their exit codes:

```yaml
script-scheduler:
  jobs:
    - name: error-prone-script
      script: /opt/scripts/might-fail.sh
      cron: "0 0 * * * ?"
      retry-attempts: 3        # Retry up to 3 times
      retry-delay-seconds: 30  # Wait 30 seconds between retries
```

### Timeout Handling

Scripts that exceed their timeout are terminated:

```yaml
script-scheduler:
  jobs:
    - name: long-running-script
      script: /opt/scripts/long-task.sh
      cron: "0 0 0 * * ?"
      timeout-seconds: 7200  # 2-hour timeout
```

### Missing Scripts

Scripts that don't exist or aren't readable will fail with appropriate error messages.

## Best Practices

1. **Use Absolute Paths**: Always use absolute paths for scripts and working directories
2. **Set Appropriate Timeouts**: Prevent runaway scripts with reasonable timeouts
3. **Handle Errors in Scripts**: Use proper exit codes in your scripts
4. **Avoid Overlapping Executions**: The scheduler prevents concurrent execution of the same script
5. **Use Description Field**: Document what each script does
6. **Test Scripts Manually**: Ensure scripts work before scheduling them
7. **Monitor Logs**: Regularly check logs for failed executions
8. **Use Environment Variables**: Avoid hardcoding values in scripts

## Troubleshooting

### Scripts Not Executing

1. Check if the scheduler is enabled: `script-scheduler.enabled=true`
2. Verify the script path exists and is readable
3. Validate the cron expression
4. Check logs for error messages

### Permission Issues

Ensure the application has execute permissions for scripts:

```bash
chmod +x /path/to/your/script.sh
```

### Environment Variables Not Working

Environment variables are added to the existing environment. Check for conflicts with system variables.

### Graceful Shutdown Issues

Increase `shutdown-timeout-seconds` if scripts need more time to complete during shutdown.

## Requirements

- Java 21 or higher
- Spring Boot 3.2.0 or higher
- Unix-like operating system (Linux, macOS) for shell scripts
- Windows support with appropriate interpreters (PowerShell, WSL)

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Support

- 📧 Email: sourin.next@gmail.com
- 🐛 Issues: [GitHub Issues](https://github.com/sourinsen/script-scheduler-spring-boot-starter/issues)
- 📖 Wiki: [GitHub Wiki](https://github.com/sourinsen/script-scheduler-spring-boot-starter/wiki)

## Acknowledgments

Built with ❤️ using:
- Spring Boot
- Spring Scheduling
- SLF4J
