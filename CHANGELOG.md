# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Future features and improvements will be listed here

## [1.0.0] - 2024-01-15

### Added
- Initial release of Script Scheduler Spring Boot Starter
- Auto-configuration for Spring Boot applications
- YAML-based job configuration without requiring Java code
- Cron-based scheduling using Spring's TaskScheduler
- Shell script execution with configurable interpreter
- Support for multiple concurrent jobs with thread pool management
- Configurable timeouts for script execution
- Working directory configuration per script
- Environment variable injection per script
- Comprehensive logging of job execution and script output
- Automatic retry logic with configurable attempts and delays
- Graceful error handling and process cleanup
- Enable/disable toggle for the entire scheduler
- Prevention of concurrent execution of the same script
- Graceful shutdown with configurable timeout
- Support for various script interpreters (sh, bash, python, etc.)
- IDE autocomplete support via spring-configuration-metadata.json
- Comprehensive test coverage including unit and integration tests
- Production-ready with proper resource management
- Maven Central publishing configuration
- Apache 2.0 License

### Configuration Properties
- `script-scheduler.enabled` - Enable/disable the scheduler
- `script-scheduler.thread-pool-size` - Configure thread pool size
- `script-scheduler.shutdown-timeout-seconds` - Graceful shutdown timeout
- `script-scheduler.default-timeout-seconds` - Default script timeout
- `script-scheduler.default-interpreter` - Default script interpreter
- `script-scheduler.default-log-output` - Default logging behavior
- `script-scheduler.jobs` - List of scheduled script jobs

### Job Properties
- `name` - Unique job identifier
- `script` - Path to script file
- `cron` - Cron expression for scheduling
- `description` - Optional job description
- `interpreter` - Script interpreter override
- `working-directory` - Working directory for execution
- `timeout-seconds` - Script timeout override
- `log-output` - Logging behavior override
- `retry-attempts` - Number of retry attempts
- `retry-delay-seconds` - Delay between retries
- `environment` - Environment variables map

### Documentation
- Comprehensive README with examples and best practices
- JavaDoc for all public APIs
- Contributing guidelines
- Example configurations for common use cases

### Testing
- Unit tests for all components
- Integration tests for end-to-end scenarios
- Test coverage for:
  - Successful script execution
  - Script timeout handling
  - Script failure scenarios
  - Invalid configuration handling
  - Multiple concurrent jobs
  - Environment variable injection
  - Working directory configuration
  - Retry logic
  - Graceful shutdown
  - Auto-configuration activation/deactivation

## Version History

- **1.0.0** - Initial release with full feature set
- **0.0.1-SNAPSHOT** - Development version

---

For detailed migration guides and upgrade instructions, please refer to the [Wiki](https://github.com/sourinsen/script-scheduler-spring-boot-starter/wiki).

[Unreleased]: https://github.com/sourinsen/script-scheduler-spring-boot-starter/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/sourinsen/script-scheduler-spring-boot-starter/releases/tag/v1.0.0
