Review code changes in Flink Risk Control against project conventions.

## Review Checklist

### License
- [ ] MIT license header present on all new/modified source files

### Code Style
- [ ] SLF4J used for logging (no direct System.out)
- [ ] Jackson used for JSON serialization
- [ ] 4-space indentation, consistent with surrounding code
- [ ] try-with-resources used for `Closeable` objects

### Error Handling
- [ ] No swallowed exceptions (at minimum, log them)
- [ ] Proper exception handling in async operations

### Tests
- [ ] New behaviour is covered by a test
- [ ] If this is a bug fix, there is a test that would have caught the original bug

### Commit / PR
- [ ] Commit message follows conventional format
- [ ] PR targets `main` branch
- [ ] PR description explains *why*, not just *what*

Report each item as pass or fail with a brief explanation. Suggest fixes for any failures.
