# Code Style

## License

MIT license header required on all new source files.

## Logging

- Use **SLF4J** for logging
- Use `logger.error()` / `logger.warn()` / `logger.info()` / `logger.debug()` for different levels
- Never use `System.out.println()` in production code

## Serialization

- Use **Jackson** for JSON serialization
- Use **FastJSON2** for alternative JSON handling

## Error Handling

- No swallowed exceptions (at minimum, log them)
- Proper exception handling in async operations
- Use try-with-resources for all `Closeable` objects

## Formatting

- 4-space indentation
- Follow existing patterns in the file being edited
- Max line length: 120 characters
