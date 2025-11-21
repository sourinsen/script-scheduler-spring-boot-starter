# Contributing to Script Scheduler Spring Boot Starter

First off, thank you for considering contributing to Script Scheduler Spring Boot Starter! It's people like you that make this project such a great tool.

## Code of Conduct

This project and everyone participating in it is governed by our Code of Conduct. By participating, you are expected to uphold this code. Please report unacceptable behavior to sourin.next@gmail.com.

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check existing issues as you might find out that you don't need to create one. When you are creating a bug report, please include as many details as possible:

* **Use a clear and descriptive title** for the issue to identify the problem.
* **Describe the exact steps which reproduce the problem** in as many details as possible.
* **Provide specific examples to demonstrate the steps**. Include links to files or GitHub projects, or copy/pasteable snippets.
* **Describe the behavior you observed after following the steps** and point out what exactly is the problem with that behavior.
* **Explain which behavior you expected to see instead and why.**
* **Include logs** with DEBUG level enabled if possible.
* **Include your configuration** (anonymize sensitive data).
* **Specify the version** of the starter you're using.
* **Specify your Java version** and Spring Boot version.

### Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues. When creating an enhancement suggestion, please include:

* **Use a clear and descriptive title** for the issue to identify the suggestion.
* **Provide a step-by-step description of the suggested enhancement** in as many details as possible.
* **Provide specific examples to demonstrate the steps** or mock-ups if applicable.
* **Describe the current behavior** and **explain which behavior you expected to see instead** and why.
* **Explain why this enhancement would be useful** to most users.

### Pull Requests

1. Fork the repo and create your branch from `main`.
2. If you've added code that should be tested, add tests.
3. If you've changed APIs, update the documentation.
4. Ensure the test suite passes.
5. Make sure your code follows the existing code style.
6. Issue that pull request!

## Development Setup

### Prerequisites

- Java 21 or higher
- Maven 3.8+
- Git

### Setting Up Your Environment

1. Fork and clone the repository:
```bash
git clone https://github.com/sourinsen/script-scheduler-spring-boot-starter.git
cd script-scheduler-spring-boot-starter
```

2. Build the project:
```bash
mvn clean install
```

3. Run tests:
```bash
mvn test
```

4. Run integration tests:
```bash
mvn verify
```

## Coding Guidelines

### Java Style Guide

* Use 4 spaces for indentation (no tabs)
* Maximum line length is 120 characters
* Use meaningful variable and method names
* Write JavaDoc for all public classes and methods
* Follow standard Java naming conventions

### Code Organization

* Keep classes focused and single-purpose
* Prefer composition over inheritance
* Use dependency injection
* Write defensive code with proper null checks
* Handle exceptions appropriately

### Testing

* Write unit tests for all new code
* Maintain at least 80% code coverage
* Write integration tests for complex scenarios
* Use descriptive test method names
* Follow the Arrange-Act-Assert pattern

### Commit Messages

* Use the present tense ("Add feature" not "Added feature")
* Use the imperative mood ("Move cursor to..." not "Moves cursor to...")
* Limit the first line to 72 characters or less
* Reference issues and pull requests liberally after the first line
* Consider starting the commit message with an applicable emoji:
    * 🎨 `:art:` when improving the format/structure of the code
    * 🐛 `:bug:` when fixing a bug
    * ✨ `:sparkles:` when introducing new features
    * 📝 `:memo:` when writing docs
    * ♻️ `:recycle:` when refactoring code
    * ✅ `:white_check_mark:` when adding tests
    * 🔧 `:wrench:` when updating configuration

### Documentation

* Update the README.md if you change functionality
* Add JavaDoc to all public methods and classes
* Include examples in documentation when helpful
* Keep documentation concise and clear

## Testing Your Changes

### Running Tests

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=ScriptExecutorTest

# Run with coverage
mvn clean test jacoco:report
```

### Writing Tests

Example test structure:

```java
@Test
void shouldExecuteScriptSuccessfully() {
    // Arrange
    ScheduledScript script = createTestScript();
    
    // Act
    int exitCode = executor.execute(script);
    
    // Assert
    assertThat(exitCode).isZero();
}
```

## Project Structure

```
script-scheduler-spring-boot-starter/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── io/scriptscheduler/spring/
│   │   │       ├── ScriptSchedulerAutoConfiguration.java
│   │   │       ├── ScriptSchedulerProperties.java
│   │   │       ├── ScriptSchedulerService.java
│   │   │       ├── ScriptExecutor.java
│   │   │       └── ScheduledScript.java
│   │   └── resources/
│   │       └── META-INF/
│   │           ├── spring.factories
│   │           └── spring-configuration-metadata.json
│   └── test/
│       ├── java/
│       │   └── io/scriptscheduler/spring/
│       │       └── [Test Classes]
│       └── resources/
│           └── [Test Resources]
├── pom.xml
├── README.md
├── LICENSE
└── CONTRIBUTING.md
```

## Release Process

1. Update version in `pom.xml`
2. Update `CHANGELOG.md`
3. Create a git tag: `git tag -a v1.0.0 -m "Release version 1.0.0"`
4. Push changes and tags: `git push origin main --tags`
5. Deploy to Maven Central: `mvn clean deploy`

## Getting Help

If you need help, you can:

* Open an issue with a question label
* Email the maintainers at sourin.next@gmail.com
* Check the [Wiki](https://github.com/sourinsen/script-scheduler-spring-boot-starter/wiki)

## Recognition

Contributors will be recognized in the project README and release notes. We value all contributions, whether they're code, documentation, bug reports, or feature suggestions.

Thank you for contributing!
