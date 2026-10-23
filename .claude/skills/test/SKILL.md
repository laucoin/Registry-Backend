---
name: test
description: Write, extend or fix tests for the Registry backend (Kotlin, JUnit5, StepVerifier, mockito-kotlin, Testcontainers, WebTestClient). Use only when the user asks to add, write, complete or fix tests, or right before a commit when new code lacks coverage. Never write tests automatically after an edit.
---

# Write tests — Registry Backend

Write or extend tests for the code pointed at, following these conventions.

## Stack

JUnit 5, `mockito-kotlin` (`mock()`, `whenever`, `verify`), reactor-test `StepVerifier`, Testcontainers +
`WebTestClient` via the shared `TestContext`, ArchUnit (`HexagonalArchitectureTest`). Run `./gradlew test`; the full
gate is `./gradlew build` (includes `koverVerify`).

## Conventions

- Mirror the package of the class under test under `src/test/kotlin/...`; file `<Class>Test.kt`. Tabs for indentation.
- **AAA pattern, strictly.** Every test contains exactly these comments, with no additional detail:

  ```kotlin
  // Arrange
  // Act
  // Assert
  ```

- Methods ≤ 25 lines, tests included. Descriptive backtick names: ``fun `Should ... when ...`()``.
- **Unit tests** (services, validators, extensions): construct the class directly, `mock()` its ports, no Spring
  context. Reactive results are verified with `StepVerifier.create(...).expectNext(...).verifyComplete()`.
- **Parameterized cases:** `@ParameterizedTest` + `@MethodSource` with a `@JvmStatic` provider returning
  `Stream<Arguments>`.
- **Repositories:** Testcontainers PostgreSQL. **Endpoints:** `WebTestClient` for contract and `@PreAuthorize`
  authorization (allow and deny cases).
- Reuse the shared helpers and fixtures (`WebTestClientExt`, `ModelExt`, `TestContext`, `TestContainerDatabase`);
  seed data via SQL under `src/test/resources/db/migrations`. Fixtures are excluded from coverage.

## Deliverable

- Cover the happy path, edge cases, empty/`switchIfEmpty` branches, error branches, validation bounds and
  authorization allow/deny.
- Add cases to an existing `*Test.kt` when one exists for the class.
- Run `./gradlew test` and report the result. Never weaken assertions or coverage thresholds to pass; thresholds only
  go up.
