---
mode: agent
description: Write or extend tests for the Registry backend (Kotlin, JUnit5, Reactor, Testcontainers)
---

# Write tests — Registry Backend

Write or extend tests for the code I point you at, following this project's conventions.

## Stack & tooling

- **JUnit5** (`useJUnitPlatform`), tests run in parallel forks.
- **mockito-kotlin** — `mock()`, `whenever(...).thenReturn(...)`, `verify(...)`.
- **reactor-test** — `StepVerifier` for `Mono`/`Flux` assertions.
- **Testcontainers + WebTestClient** for integration, via the shared `TestContext`.
- **ArchUnit** for architecture rules (`test/HexagonalArchitectureTest.kt`).
- Run with `./gradlew test`; keep coverage green (`./gradlew build` runs `koverVerify`).

## Conventions to follow

- Mirror the package of the class under test under `src/test/kotlin/...`; name the file `<Class>Test.kt`.
- **Indent with tabs.**
- **Unit tests (default for `domain/service`, `validator`, `extension`)**: construct the class directly, `mock()` its
  ports, no Spring context. Example shape (see `domain/service/impl/RoleServiceTest.kt`):

  ```kotlin
  class RoleServiceTest {
      private val port: IRolePort = mock()
      private val service = RoleService(port, "ROLE_2")

      @Test
      fun `Should return the level for a known role`() {
          // Arrange
          whenever(port.findAll()).thenReturn(Flux.fromIterable(roles))
          // Act + Assert
      }
  }
  ```

- **Reactive assertions** — verify with `StepVerifier`:

  ```kotlin
  StepVerifier.create(service.doThing(input))
      .expectNext(expected)
      .verifyComplete()
  ```

- **Parameterized cases** — `@ParameterizedTest` + `@MethodSource`, with a `@JvmStatic` provider in a `companion object`
  returning `Stream<Arguments>`. Descriptive backtick test names (`fun \`Should ... \` ()`).
- **Integration tests** — extend/use `TestContext` (`@SpringBootTest(RANDOM_PORT)` + Testcontainers Postgres +
  `WebTestClient`). Use helpers in `test/` (`WebTestClientExt`, `ModelExt`, `TestContext`, `TestContainerDatabase`).
  Seed data via SQL under `src/test/resources/db/migrations`.
- Use existing test helpers/extensions rather than re-inventing setup.
- **Comments (AGENTS.md § Comment policy)**: structure each test with `// Arrange`, `// Act`, `// Assert` (or
  `// Act + Assert`) comments — these are mandatory and must never be deleted or modified. Write no other comments
  inside test bodies; a genuinely complex test gets one English KDoc block (`/** … */`) directly above it, never an ADR
  reference.

## Deliverable

- Cover the happy path, edge cases, empty/`switchIfEmpty` branches, and error/authorization paths.
- Add cases to an existing `*Test.kt` when one already exists for the class; otherwise create it.
- Run `./gradlew test` and report the result. Do not weaken assertions or coverage thresholds to make a build pass.
