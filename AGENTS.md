# AGENTS.md — Registry Backend

Reactive REST API for the Registry platform. This file is the single source of truth for agents working here.
It applies to ALL AI agents (Claude Code, Copilot, Cursor, or any other assistant) generating or modifying code
in this repository — permanent project policy, not suggestions.

## Stack

- **Kotlin 2.4** on **JVM 25**, built with **Gradle** (Kotlin DSL).
- **Spring Boot 4.1 WebFlux** — fully reactive (`Mono` / `Flux`), non-blocking.
- **R2DBC + PostgreSQL**, migrations via **Flyway** (`src/main/resources/db/migrations`).
- **OAuth2 resource server (JWT)** + **Keycloak** for auth.
- **Kover** (coverage), **ArchUnit** (architecture), **springdoc** (OpenAPI), **Micrometer** metrics (Prometheus
  exposition format, scraped by **VictoriaMetrics** — ADR 025).

## Commands

```bash
./gradlew build            # compile + test + coverage verify
./gradlew test             # tests only (JUnit5, parallel forks)
./gradlew bootRun          # run in dev (needs local-dev/compose.yml deps up)
./gradlew koverHtmlReport  # coverage report -> build/reports/kover
```

`build` runs `koverVerify` + `koverHtmlReport` after tests. Local deps (Postgres, Keycloak, VictoriaMetrics) come from
`local-dev/compose.yml`; default app port is `8081`.

## Architecture — hexagonal, enforced by ArchUnit

Package root: `fr.laucoin.registry.backend`

- `config` — Spring config (security, r2dbc, i18n, swagger). Nothing else may depend on it.
- `domain` — business core: `service` (+ `service/impl`), `port` (interfaces to infra), `model`, `validator`, `handler`,
  `extension`, `enumeration`, `constant`, `annotation`. No Spring web / persistence types here.
- `infrastructure/in` — inbound adapters: `postgres` (R2DBC repos + `entity`), `keycloak`. Entities stay inside their
  sub-package.
- `infrastructure/out` — outbound adapters: `api` (controllers + DTO mappers). Must **not** depend on
  `infrastructure/in`.

Rules (see `src/test/.../test/HexagonalArchitectureTest.kt`): infra never depends on `config`; `out` never depends on
`in`; controllers (`@RestController`) implement a contract interface; postgres `entity` only accessed within postgres
package. Domain reaches infrastructure only through `port` interfaces.

---

## 🔒 Security — treat as first-class, review every change against it

Config lives in `config/SecurityConfig.kt` (`@EnableWebFluxSecurity` + `@EnableReactiveMethodSecurity`) and
`domain/handler`.

- **Authentication** — stateless JWT via OAuth2 resource server; tokens converted by `TokenConverterService`. CSRF is
  disabled *because* the API is stateless/token-based — keep it that way, never fall back to sessions/form login.
- **Authorization is permission-based, not role-based-only.** Every endpoint carries `@PreAuthorize` using the custom
  `hasPermission(...)` evaluator (`PermissionService`), e.g.
  `@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ACTIVITY') && hasPermission(#projectId, '$REGISTRY_PROJECT_ACTIVITY_R')")`.
    - **Never add an endpoint without an explicit `@PreAuthorize`.** `anyExchange().authenticated()` is only a baseline.
    - Permissions are constants in `domain/constant/ProjectPermissionConst.kt` and `UserPermissionConst.kt`, with CRUD
      suffixes `_C` / `_R` / `_U` / `_D` (+ feature-option gates like `REGISTRY_PROJECT_OPTION_*`). Reuse existing
      constants; add new ones there, never inline string literals.
    - Check option/feature permission **and** operation permission where the pattern above applies.
- **Security headers** — set centrally by `HeadersHandler` (registered `FIRST` in the filter chain). Add/adjust headers
  there, not per-controller.
- **CORS** — origins come from `external.cors.urls` config with `allowCredentials = true`; never widen to `*`. Only the
  header/method allow-lists in `SecurityConfig` are permitted.
- **Public endpoints** are an explicit, minimal allow-list (auth login/logout/token URIs, and docs/actuator only when
  their feature flag is on). Don't add to it casually.
- **Errors** go through `AuthorizationErrorHandler` (401/403) and `RegistryControllerAdvice` — never leak stack traces,
  internal messages, or entity internals in responses.
- **Input validation** is mandatory: Bean Validation (`@Valid`, `@Min`/`@Max`, etc.) on request params/bodies, plus the
  custom domain annotations in `domain/annotation` and validators in `domain/validator`. Validate before touching the
  domain.
- Never log tokens, credentials, or PII.

## 🌐 API best practices

- **Versioned, resource-oriented URLs**: `/api/v1/...`, plural nouns, hierarchical nesting for ownership
  (`/api/v1/projects/{projectId}/activities`). Keep new endpoints consistent with existing ones.
- **HTTP semantics**: GET (read, no side effects), POST (create), PUT (full update), PATCH (partial), DELETE (remove).
  Return appropriate status codes; let `RegistryControllerAdvice` shape error bodies.
- **Controllers are interfaces**: declare the contract (`I<Name>V1Controller`) with `@Tag`/`@Operation` OpenAPI docs,
  `@PreAuthorize`, and validation; the implementation delegates to a domain service. Document every operation —
  springdoc UI is the API surface.
- **DTOs, not entities, at the boundary**: separate `reader` (response) and `writer` (request) DTOs with dedicated
  mappers (`infrastructure/out/api/mapper`). Never expose persistence entities.
- **Pagination is the default for collections**: validated `pageNumber` (`@Min(0)`) / `pageSize` (`@Min(1)`/`@Max`),
  return a `PageModel`. Never return unbounded lists.
- Consistent, i18n error messages via constants (`ErrorConst`) + `src/main/resources/i18n`; validation messages
  reference message keys, not hardcoded English.
- Breaking changes require a new API version, not mutation of `v1`.

## ⚡ Performance

- **Reactive end-to-end — never block a reactor thread.** No `.block()`, `Thread.sleep`, or blocking JDBC/IO; compose
  with `map`/`flatMap`/`switchIfEmpty` and the helpers in `domain/extension/ReactiveExt.kt`. A single blocking call can
  stall the event loop.
- **Prefer `flatMap`/`concatMap` over nested subscribes**; be deliberate about concurrency vs ordering (`flatMap`
  concurrent, `concatMap` ordered).
- **Push work to the database**: filter/sort/paginate in SQL (R2DBC), don't load-then-filter in memory. Avoid N+1 query
  patterns across reactive chains.
- **Bound everything**: enforced page-size max, no unbounded `Flux` collection into memory for large sets.
- Metrics are available via Micrometer (`/actuator/prometheus`, stored in VictoriaMetrics) — prefer measuring over
  guessing on hot paths.

---

## Conventions

- **Indentation: tabs** (Kotlin).
- Domain depends on ports, not adapters; controllers delegate to services.
- Add DB changes as new Flyway migrations — never edit an applied one.
- i18n messages live in `src/main/resources/i18n`.

## 💬 Comment policy (strict — binding for all agents and all generated code)

- **No extraneous comments.** Never write inline explanations, end-of-line (trailing) comments, step-by-step narration
  inside method bodies, commented-out code, section dividers, or docstrings that merely restate a signature. Code must
  read cleanly on its own.
- **The one allowed comment:** a single English **block comment — KDoc `/** … */`** — placed **directly above** a
  complex or non-obvious declaration (class, function, SQL constant, `@Query`), explaining its intent, high-level logic
  or a constraint the code cannot express (ADR reference, security invariant, regression rationale). Never use a `//`
  block for documentation: `//` is reserved for the test structure markers below and for tooling directives.
  If an in-body detail is genuinely load-bearing, fold it into that block comment — never leave it inline in the body.
  Obvious, self-explanatory declarations get no comment at all.
- **Test structure comments are mandatory and untouchable:** `// Arrange`, `// Act`, `// Assert` (including combined
  forms such as `// Act + Assert`) must be present in tests and must never be deleted, reworded or moved. They are the
  only permitted in-body comments in tests.
- Tooling directives (`@Suppress`, `// noinspection`, coverage-ignore markers) are not comments under this policy; keep
  them where the tooling needs them.
- When touching existing code, remove any comment that violates this policy in the code you touch; hoist genuinely
  non-obvious inline rationale into the block comment above the declaration instead of deleting the knowledge.

### Rule for future code generation (binding)

When generating, refactoring or reviewing ANY code in this repository — as Claude Code, Copilot, Cursor or any other
assistant:

1. Produce no extraneous comments — the default for any line of code is NO comment.
2. Strictly preserve `// Arrange`, `// Act`, `// Assert` wherever they appear; never delete, reword or move them.
3. Limit documentation to English KDoc block comments (`/** … */`) placed directly above complex or non-obvious
   declarations, per the allowed exception above.
4. If valuable non-obvious rationale currently lives in an inline comment, hoist a condensed version into the block
   comment above the declaration instead of deleting the information.
5. When you touch a file, bring the comments in the code you touched up to this standard.

## Testing

See `.github/prompts/test.prompt.md`. In short: JUnit5 + `mockito-kotlin` (`mock`/`whenever`/`verify`), `reactor-test`
`StepVerifier` for reactive flows, `@ParameterizedTest` + `@MethodSource` (backtick names) for cases, and `TestContext`
(Testcontainers Postgres + `WebTestClient`) for integration — including authorization tests for `@PreAuthorize` rules.
Keep coverage green (`koverVerify`).

## Reusable prompts

- [`.github/prompts/test.prompt.md`](.github/prompts/test.prompt.md) — write/extend tests.
- [`.github/prompts/code-review.prompt.md`](.github/prompts/code-review.prompt.md) — review a change.

## Reported bugs — check the documentation before fixing

When the user reports a bug, or disputes behaviour a test asserts, treat the documentation as part of
the investigation rather than as background reading.

1. **Find what the docs claim** about the behaviour — the feature page, the roles/permissions matrix,
   the API reference, and the journey scenarios in `critical-scenarios.md`.
2. **Decide which side is wrong.** The reported behaviour, the code, and the docs are three
   independent claims; a disagreement between them is itself the finding. Where a page names an
   authoritative source (for example, roles-and-permissions.md defers to the seed migrations for the
   permission matrix), that source wins over the prose.
3. **Fix the documentation in the same change as the code.** A bug fix that leaves the docs asserting
   the old, wrong behaviour just moves the defect. If the docs were right and the code was wrong,
   say so explicitly in the change; if the docs were wrong, correct them and keep the scenario list
   in step.
4. **Never silently loosen a test to match observed behaviour.** Establish which behaviour is correct
   first, then align the test with that — citing the authority you relied on.
5. **Report contradictions you cannot resolve** instead of picking a side; they usually mean a
   decision is owed.
