# AGENTS.md — Registry Backend

Reactive REST API for the Registry platform. This file is the single source of truth for agents working here. It applies
to ALL AI agents (Claude Code, Copilot, Cursor, or any other assistant) generating or modifying code in this
repository — permanent project policy, not suggestions.

## Stack

- **Kotlin 2.4** on **JVM 25**, built with **Gradle** (Kotlin DSL).
- **Spring Boot 4.1 WebFlux** — fully reactive (`Mono` / `Flux`), non-blocking.
- **R2DBC + PostgreSQL**, migrations via **Flyway** (`src/main/resources/db/migrations`).
- **OAuth2 resource server (JWT)** + an **OIDC provider** for auth (provider-agnostic; local dev runs Authentik).
- **Kover** (coverage), **ArchUnit** (architecture), **springdoc** (OpenAPI), **Micrometer** metrics (Prometheus
  exposition format, scraped by **VictoriaMetrics**).

## Commands

```bash
./gradlew build            # compile + test + coverage verify
./gradlew test             # tests only (JUnit5, parallel forks)
./gradlew bootRun          # run in dev (needs local-dev/compose.yml deps up)
./gradlew koverHtmlReport  # coverage report -> build/reports/kover
```

`build` runs `koverVerify` + `koverHtmlReport` after tests. Local deps (Postgres, Authentik, VictoriaMetrics) come from
`local-dev/compose.yml`; default app port is `8081`.

## Architecture — hexagonal, enforced by ArchUnit

Package root: `fr.laucoin.registry.backend`

- `config` — Spring config (security, r2dbc, i18n, swagger). Nothing else may depend on it.
- `domain` — business core: `service` (+ `service/impl`), `port` (interfaces to infra), `model`, `validator`, `handler`,
  `extension`, `enumeration`, `constant`, `annotation`. Keep Spring web / persistence types out (see the deviations
  noted below).
- `infrastructure/in` — the data side: `postgres` (R2DBC repos + `entity`), `oidc`. Entities stay inside their
  sub-package.
- `infrastructure/out` — the HTTP side: `api` (controllers + DTO mappers). Must **not** depend on `infrastructure/in`.

`in`/`out` here name the *data direction* (into and out of the app), not the hexagonal driving/driven split — don't
rename them, but don't read them as "inbound/outbound adapter" either.

Rules actually enforced by `src/test/.../test/HexagonalArchitectureTest.kt`: infra never depends on `config`; `out`
never depends on `in`; controllers (`@RestController`) implement a contract interface; postgres `entity` only accessed
within the postgres package; naming suffixes per package (`*Port`, `*Service`, `*Handler`, `*Const`, `*Enum`, `*Ext`,
`*Model`, `*Config`, `*Validator`).

**Not enforced by ArchUnit, but still the rule:** domain reaches infrastructure only through `port` interfaces. There
are known deviations to shrink, not to copy — `domain/handler` holds WebFlux `WebFilter`s, and a few `domain/handler`
and `domain/validator` classes import `infrastructure/out/api` DTOs directly. New code must not add to that list.

---

## 🔒 Security — treat as first-class, review every change against it

Config lives in `config/SecurityConfig.kt` (`@EnableWebFluxSecurity` + `@EnableReactiveMethodSecurity`) and
`domain/handler`.

- **Authentication** — stateless JWT via OAuth2 resource server; tokens converted by `TokenConverterService`. CSRF is
  disabled *because* the API is stateless/token-based — keep it that way, never fall back to sessions/form login.
- **Authorization is permission-based, not role-based-only.** Every endpoint carries `@PreAuthorize` using the custom
  `hasPermission(...)` evaluator (`PermissionService`), e.g.
  `@PreAuthorize("hasPermission(#projectId, '$REGISTRY_PROJECT_OPTION_ACTIVITY') && hasPermission(#projectId, '$REGISTRY_PROJECT_ACTIVITY_R')")`.
    - **Never add an endpoint without an explicit `@PreAuthorize`**, with exactly two exceptions, both enforced by
      tests rather than convention:
        1. **`@HttpCacheable` endpoints must NOT carry `@PreAuthorize`** — `CacheHeadersHandler`'s 304 short-circuit
           answers before the controller method is invoked, so the check would be silently skipped. The security
           *filter* chain still runs, so those endpoints are authenticated by the `anyExchange().authenticated()`
           baseline. `HttpCacheableArchitectureTest` fails the build on the combination.
        2. **Public endpoints** (`SecurityConfig`'s allow-list) carry no method check.
    - **`@PreAuthorize` requires a `Mono`/`Flux` return type.** Reactive method security does not intercept a plain
      return value — annotating such a method yields a 500 at runtime, not a denial. A controller method that needs
      authorization must be reactive.
      `anyExchange().authenticated()` is only a baseline for the two exceptions above.
    - Permissions are constants in `domain/constant/ProjectPermissionConst.kt` and `UserPermissionConst.kt`, with CRUD
      suffixes `_C` / `_R` / `_U` / `_D` (+ feature-option gates like `REGISTRY_PROJECT_OPTION_*`). Reuse existing
      constants; add new ones there, never inline string literals.
    - Check option/feature permission **and** operation permission where the pattern above applies.
- **Security headers** — set centrally by `ApiHeadersHandler` (`nosniff` + the `Cache-Control: no-store` default, at
  commit time). Add/adjust headers there, not per-controller. `HeadersHandler` is the *locale* filter (registered
  `FIRST` in the chain) — don't confuse the two.
- **CORS** — origins come from `external.cors.urls` config with `allowCredentials = true`; never widen to `*`. Only the
  header/method allow-lists in `SecurityConfig` are permitted. **Any header the API newly sends or newly reads has to
  be declared there too** — a response header the browser must read goes in `exposedHeaders` (`ETag`, `Retry-After`,
  `X-Correlation-Id`), a request header the client sends goes in `allowedHeaders`. A header written by a filter but
  absent from that list is invisible to the SPA.
- **Public endpoints** are an explicit, minimal allow-list (auth login/logout/token URIs, and docs/actuator only when
  their feature flag is on). Don't add to it casually.
- **Errors** go through `AuthorizationErrorHandler` (401/403) and `RegistryControllerAdvice` — never leak stack traces,
  internal messages, or entity internals in responses.
- **Input validation** is mandatory: Bean Validation (`@Valid`, `@Min`/`@Max`, etc.) on request params/bodies, plus the
  custom domain annotations in `domain/annotation` and validators in `domain/validator`. Validate before touching the
  domain.
- **Audit trail** — privileged/destructive actions (RBAC and access changes, deletes, anonymization) are wrapped with
  `IAuditService.audit(...)` using an `AuditActionEnum` entry. Emission is best-effort and must never change the
  wrapped pipeline's result. Regular CRUD stays out of the trail.
- **Correlation id** — `CorrelationIdHandler` puts one `X-Correlation-Id` per request in the Reactor context and echoes
  it on the response; the audit trail stamps it. Don't invent a second request identifier.
- Never log tokens, credentials, or PII.

## 🌐 API best practices

- **`/api/v2` is the current version; `/api/v1` is deprecated** and kept only until consumers migrate. New endpoints go
  in v2. Every v1 operation carries `@Operation(deprecated = true)` and a `(v1, deprecated)` `@Tag` — keep it that way.
- **Versioned, resource-oriented URLs**: `/api/v2/...`, plural nouns, hierarchical nesting for ownership
  (`/api/v2/projects/{projectId}/activities`). Keep new endpoints consistent with existing ones.
- **HTTP semantics**: GET (read, no side effects), POST (create), PUT (full update), PATCH (partial), DELETE (remove).
  Return appropriate status codes; let `RegistryControllerAdvice` shape error bodies.
- **Controllers are interfaces**: declare the contract (`I<Name>V2Controller`) with `@Tag`/`@Operation` OpenAPI docs,
  `@PreAuthorize`, and validation; the implementation delegates to a domain service. Document every operation —
  springdoc UI is the API surface. `@Tag` names must be unique per version, or springdoc merges v1 and v2 into one
  section.
- **Don't restate in `description` what the annotations already generate.** Request params, types, defaults and bounds
  are derived from the signature; repeating them in prose only creates a second place to forget to update.
- **Cross-cutting HTTP behaviour is declared ON the endpoint**, never inferred from a hard-coded path list: rate
  limiting is `@RateLimited(category)`, HTTP caching is `@HttpCacheable`, authorization is `@PreAuthorize`. The filters
  (`RateLimitHandler`, `CacheHeadersHandler`) resolve those annotations through `AnnotatedEndpointsHandler`. Whoever
  edits an endpoint must be able to see everything that applies to it without knowing the whole repository.
- **DTOs, not entities, at the boundary**: separate `reader` (response) and `writer` (request) DTOs with dedicated
  mappers (`infrastructure/out/api/mapper`). Never expose persistence entities.
- **Pagination is the default for collections**: in v2 the params are `page` (`@Min(0)`) and `size`
  (`@Min(1)`/`@Max(200)`) — v1's `pageNumber`/`pageSize` are deprecated names — plus `sort` + `direction`; return a
  `PageModel`. Non-paginated dashboard collections take a `limit` bounded by `ApiConst.DEFAULT_COLLECTION_LIMIT`.
  Never return unbounded lists.
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
- **Reach for the framework's own constant/validator/type before writing one.** Spring and Jakarta already name most
  headers, statuses and constraints (`XContentTypeOptionsServerHttpHeadersWriter.NOSNIFF`, `HttpHeaders.*`,
  `@Email`, `@Min`). Hand-rolled equivalents need a stated reason — e.g. `ValidEmails` exists because Kotlin
  container-element constraints (`List<@Email String>`) are not traversed by Hibernate Validator here (verified).
- **No shaded third-party internals.** `com.nimbusds.jose.shaded.gson` is nimbus's private repackaging and can vanish
  on any upgrade; use the project's own Jackson for JSON.
- **Every endpoint needs a consumer.** Before adding one, know who calls it (Registry-Frontend, Registry-E2E, or a
  scheduler); delete API surface that nothing exercises rather than carrying it forward into v2.
- **Dependency versions live in a `val` at the top of `build.gradle.kts`**, grouped under the existing section
  comments — never inline in the coordinate string.
- Domain depends on ports, not adapters; controllers delegate to services.
- Add DB changes as new Flyway migrations — never edit an applied one. Index deliberately: a b-tree per sortable
  column taxes every write and rarely pays for itself at these row counts.
- i18n messages live in `src/main/resources/i18n`.

## 💬 Comment policy (strict — binding for all agents and all generated code)

Only three kinds of comment may exist in this repository. Everything else is removed on sight.

1. **Test structure markers** — `// Arrange`, `// Act`, `// Assert` (including combined forms such as `// Act + Assert`)
   must be present in tests and must never be deleted, reworded or moved.
2. **One KDoc block (`/** … */`) directly above a complex method**, explaining its intent, its high-level logic or a
   constraint the code cannot express. A class or object gets one only when the complexity is *global* to it — never as
   a summary of its members. Never use a `//` block for documentation.
3. **Tooling directives** (`@Suppress`, `// noinspection`, coverage-ignore markers) — not comments under this policy;
   keep them where the tooling needs them.

Forbidden, without exception:

- Inline explanations and end-of-line (trailing) comments inside a body — the test markers above are the only in-body
  comments allowed anywhere.
- Step-by-step narration of what the next lines do, section dividers, commented-out code.
- Any comment that restates a name, a signature or a type.
- Comments on DTOs, enum entries, plain constants, data holders, and API-contract restatements that the signature and
  the OpenAPI annotations already carry.
- **References to an ADR or to any decision-record number.** Rationale worth keeping is stated in the code's own terms;
  the ADR set lives in the Documentations repository and is never cited from source.
- Obvious, self-explanatory declarations get no comment at all.

### Rule for future code generation (binding)

When generating, refactoring or reviewing ANY code in this repository — as Claude Code, Copilot, Cursor or any other
assistant:

1. The default for any line of code is NO comment.
2. Strictly preserve `// Arrange`, `// Act`, `// Assert` wherever they appear; never delete, reword or move them.
3. Document only genuinely complex methods, with a single KDoc block directly above the declaration.
4. Never write an ADR reference into a comment.
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

When the user reports a bug, or disputes behaviour a test asserts, treat the documentation as part of the investigation
rather than as background reading.

1. **Find what the docs claim** about the behaviour — the feature page, the roles/permissions matrix, the API reference,
   and the journey scenarios in `critical-scenarios.md`.
2. **Decide which side is wrong.** The reported behaviour, the code, and the docs are three independent claims; a
   disagreement between them is itself the finding. Where a page names an authoritative source (for example,
   roles-and-permissions.md defers to the seed migrations for the permission matrix), that source wins over the prose.
3. **Fix the documentation in the same change as the code.** A bug fix that leaves the docs asserting the old, wrong
   behaviour just moves the defect. If the docs were right and the code was wrong, say so explicitly in the change; if
   the docs were wrong, correct them and keep the scenario list in step.
4. **Never silently loosen a test to match observed behaviour.** Establish which behaviour is correct first, then align
   the test with that — citing the authority you relied on.
5. **Report contradictions you cannot resolve** instead of picking a side; they usually mean a decision is owed.
