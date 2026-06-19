Run tests in the Flink Risk Control project.

Ask the user:
1. **Module**: which module to test? (`flink-risk-common`, `flink-risk-job`, `flink-risk-web`)
2. **Test type**: unit test or integration test?
3. **Class name** (optional): specific test class

For **unit tests**, run:
```bash
mvn test -pl <module> -DskipTests=false
```

For **integration tests** (requires Docker dependencies running):
```bash
./docker-start.sh start
mvn test -pl <module> -Dtest=*IT
```

Report the test outcome and any failures.
