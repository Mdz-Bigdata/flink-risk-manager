# Testing

## Framework

- **JUnit 5** for unit tests
- **Spring Boot Test** for web layer tests

## Test Types

- **Unit tests**: `*Test.java` — run during `test` phase
- **Integration tests**: `*IT.java` — requires Docker dependencies running

## Running Tests

```bash
# Unit tests, common module
mvn test -pl flink-risk-common

# Unit tests, job module
mvn test -pl flink-risk-job -am

# Unit tests, web module
mvn test -pl flink-risk-web -am

# All unit tests
mvn test
```

## Test-First Workflow (Recommended)

When fixing a bug:
1. **Reproduce first** — write/adjust a test that **fails** with the current behaviour
2. **Confirm red** — run the test, verify it fails
3. **Fix the code** — change production code to make the test pass
4. **Confirm green** — run the test again, verify it passes

## Docker Test Environment

```bash
# Start dependencies
./docker-start.sh start

# Run integration tests
mvn test -pl flink-risk-job -Dtest=*IT

# Stop dependencies
./docker-start.sh stop
```
