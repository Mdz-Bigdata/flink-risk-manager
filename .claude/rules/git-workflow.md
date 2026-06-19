# Git Workflow

## Branch Naming

- **Main branch**: `main` (target for all PRs)
- **Feature branches**: `feature/<description>`
- **Bug fix branches**: `fix/<issue-number>-<description>`

Always create a dedicated branch before starting any implementation work.

## Commit Message Format

```
type(scope): description

- Detail line 1
- Detail line 2
```

### Types

| Type | When to use |
|------|-------------|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation |
| `refactor` | Refactoring |
| `test` | Tests |
| `chore` | Maintenance, deps, tooling |

### Example

```
feat(cep): add activity frequent participate pattern

- Add ACTIVITY_FREQUENT CEP pattern with 4 events
- Update PatternFactory with within(Duration.ofSeconds(5))
```

## Pull Requests

- Target branch: `main`
- Title: short (under 70 chars)
- Body: bullet summary + test plan
- If the change has user-visible impact, confirm doc updates are included
