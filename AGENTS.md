# AGENTS.md — Registry Backend

Reactive REST API for the Registry platform. This file is the single source of truth for agents working here.

## Stack

- **Kotlin 2.4** on **JVM 25**, built with **Gradle** (Kotlin DSL).
- **Spring Boot 4.1 WebFlux** — fully reactive (`Mono` / `Flux`), non-blocking.
- **R2DBC + PostgreSQL**, migrations via **Flyway** (`src/main/resources/db/migrations`).
- **OAuth2 resource server (JWT)** + **Keycloak** for auth.
- **Kover** (coverage), **ArchUnit** (architecture), **springdoc** (OpenAPI), **Micrometer/Prometheus** (metrics).

## Commands

```bash
./gradlew build            # compile + test + coverage verify
./gradlew test             # tests only (JUnit5, parallel forks)
./gradlew bootRun          # run in dev (needs local-dev/compose.yml deps up)
./gradlew koverHtmlReport  # coverage report -> build/reports/kover
```

`build` runs `koverVerify` + `koverHtmlReport` after tests. Local deps (Postgres, Keycloak) come from `local-dev/compose.yml`; default app port is `8081`.

## Architecture — hexagonal, enforced by ArchUnit

Package root: `fr.laucoin.registry.backend`

- `config` — Spring config (security, r2dbc, i18n, swagger). Nothing else may depend on it.
- `domain` — business core: `service` (+ `service/impl`), `port` (interfaces to infra), `model`, `validator`, `handler`, `extension`, `enumeration`, `constant`, `annotation`. No Spring web / persistence types here.
- `infrastructure/in` — inbound adapters: `postgres` (R2DBC repos + `entity`), `keycloak`. Entities stay inside their sub-package.
- `infrastructure/out` — outbound adapters: `api` (controllers + DTO mappers). Must **not** depend on `infrastructure/in`.

Rules (see `src/test/.../test/HexagonalArchitectureTest.kt`): infra never depends on `config`; `out` never depends on `in`; controllers (`@RestController`) implement a contract interface; postgres `entity` only accessed within postgres package. Domain reaches infrastructure only through `port` interfaces.

---

## 🔒 Security — treat as first-class, review every change against it

Config lives in `config/SecurityConfig.kt` (`@EnableWebFluxSecurity` + `@EnableReactiveMethodSecurity`) and `domain/handler`.

- **Authentication** — stateless JWT via OAuth2 resource server; tokens converted by `TokenConverterService`. CSRF is disabled *because* the API is stateless/token-based — keep it that way, never fall back to sessions/form login.
- **Authorization is permission-based, not role-based-only.** Every endpoint carries `@PreAuthorize` using the custom `hasPermission(...)` evaluator (`PermissionService`), e.g.
  `@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ACTIVITY') && hasPermission(#projectId, '$REGISTRY_PROJECT_ACTIVITY_R')")`.
  - **Never add an endpoint without an explicit `@PreAuthorize`.** `anyExchange().authenticated()` is only a baseline.
  - Permissions are constants in `domain/constant/ProjectPermissionConst.kt` and `UserPermissionConst.kt`, with CRUD suffixes `_C` / `_R` / `_U` / `_D` (+ feature-option gates like `REGISTRY_PROJECT_OPTION_*`). Reuse existing constants; add new ones there, never inline string literals.
  - Check option/feature permission **and** operation permission where the pattern above applies.
- **Security headers** — set centrally by `HeadersHandler` (registered `FIRST` in the filter chain). Add/adjust headers there, not per-controller.
- **CORS** — origins come from `external.cors.urls` config with `allowCredentials = true`; never widen to `*`. Only the header/method allow-lists in `SecurityConfig` are permitted.
- **Public endpoints** are an explicit, minimal allow-list (auth login/logout/token URIs, and docs/actuator only when their feature flag is on). Don't add to it casually.
- **Errors** go through `AuthorizationErrorHandler` (401/403) and `RegistryControllerAdvice` — never leak stack traces, internal messages, or entity internals in responses.
- **Input validation** is mandatory: Bean Validation (`@Valid`, `@Min`/`@Max`, etc.) on request params/bodies, plus the custom domain annotations in `domain/annotation` and validators in `domain/validator`. Validate before touching the domain.
- Never log tokens, credentials, or PII.

## 🌐 API best practices

- **Versioned, resource-oriented URLs**: `/api/v1/...`, plural nouns, hierarchical nesting for ownership (`/api/v1/projects/{projectId}/activities`). Keep new endpoints consistent with existing ones.
- **HTTP semantics**: GET (read, no side effects), POST (create), PUT (full update), PATCH (partial), DELETE (remove). Return appropriate status codes; let `RegistryControllerAdvice` shape error bodies.
- **Controllers are interfaces**: declare the contract (`I<Name>V1Controller`) with `@Tag`/`@Operation` OpenAPI docs, `@PreAuthorize`, and validation; the implementation delegates to a domain service. Document every operation — springdoc UI is the API surface.
- **DTOs, not entities, at the boundary**: separate `reader` (response) and `writer` (request) DTOs with dedicated mappers (`infrastructure/out/api/mapper`). Never expose persistence entities.
- **Pagination is the default for collections**: validated `pageNumber` (`@Min(0)`) / `pageSize` (`@Min(1)`/`@Max`), return a `PageModel`. Never return unbounded lists.
- Consistent, i18n error messages via constants (`ErrorConst`) + `src/main/resources/i18n`; validation messages reference message keys, not hardcoded English.
- Breaking changes require a new API version, not mutation of `v1`.

## ⚡ Performance

- **Reactive end-to-end — never block a reactor thread.** No `.block()`, `Thread.sleep`, or blocking JDBC/IO; compose with `map`/`flatMap`/`switchIfEmpty` and the helpers in `domain/extension/ReactiveExt.kt`. A single blocking call can stall the event loop.
- **Prefer `flatMap`/`concatMap` over nested subscribes**; be deliberate about concurrency vs ordering (`flatMap` concurrent, `concatMap` ordered).
- **Push work to the database**: filter/sort/paginate in SQL (R2DBC), don't load-then-filter in memory. Avoid N+1 query patterns across reactive chains.
- **Bound everything**: enforced page-size max, no unbounded `Flux` collection into memory for large sets.
- Metrics are available via Micrometer/Prometheus — prefer measuring over guessing on hot paths.

---

## Conventions

- **Indentation: tabs** (Kotlin).
- Domain depends on ports, not adapters; controllers delegate to services.
- Add DB changes as new Flyway migrations — never edit an applied one.
- i18n messages live in `src/main/resources/i18n`.

## Testing

See `.github/prompts/test.prompt.md`. In short: JUnit5 + `mockito-kotlin` (`mock`/`whenever`/`verify`), `reactor-test` `StepVerifier` for reactive flows, `@ParameterizedTest` + `@MethodSource` (backtick names) for cases, and `TestContext` (Testcontainers Postgres + `WebTestClient`) for integration — including authorization tests for `@PreAuthorize` rules. Keep coverage green (`koverVerify`).

## Reusable prompts

- [`.github/prompts/test.prompt.md`](.github/prompts/test.prompt.md) — write/extend tests.
- [`.github/prompts/code-review.prompt.md`](.github/prompts/code-review.prompt.md) — review a change.
