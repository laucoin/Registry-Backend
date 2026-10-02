# AGENTS.md - Development Guidelines for the Registry Backend (Kotlin / Spring WebFlux)

This document outlines the strict guidelines, architecture rules, and conventions for AI agents and developers working
on this Kotlin / Spring WebFlux reactive backend.

---

## 1. Language & Communication

- **User Interaction:** All direct communication, questions, and validations with the user **must** be conducted in
  **French**.
- **Code & Comments:** All source code, identifiers, commit messages, and inline comments **must** be written in **US
  English**.
- **Strict Scope:** Address only the requested step. Do not refactor surrounding code, touch unrelated packages, or fix
  unrelated issues in passing — see §6's sizing rules.

---

## 2. Tech Stack

- **Language / Runtime:** Kotlin on JDK 25.
- **Framework:** Spring Boot, fully reactive (**WebFlux**, no Spring MVC).
- **Reactivity:** Project Reactor (`Mono`/`Flux`) end-to-end. No `.block()`, `Thread.sleep`, or blocking JDBC/file IO on
  the request path.
- **Persistence:** R2DBC against PostgreSQL, jOOQ as the type-safe SQL DSL, Flyway for forward-only schema migrations.
- **Identity:** Delegated to Authentik (OIDC) — JWTs converted to authorities at request time, no local password store.
- **API Docs:** springdoc/Swagger, gated by a feature flag.
- **i18n:** Message bundles (`resources/i18n`), English default + French.
- **Build:** Gradle (Kotlin DSL).
- **Testing:** JUnit5, Reactor `StepVerifier`, `mockito-kotlin`, Testcontainers (PostgreSQL), ArchUnit, Kover for
  coverage.
- **Observability:** Actuator/Prometheus, behind a feature flag.
- **Packaging:** Distroless, non-root JVM image; semantic-release drives versioning/publishing to GHCR.

---

## 3. Architecture & Project Structure

Hexagonal architecture, **build-enforced** by `HexagonalArchitectureTest` (ArchUnit) on every `./gradlew build`. The
root package `fr.laucoin.registry.backend` contains only:

1. **`config/`**: Spring configuration classes (datasource, security, i18n, Swagger, JWT decoder, …).
2. **`domain/`**: The hexagon's core — framework-agnostic business logic.
    - `port/`: interfaces the domain exposes to the outside (`IXxxPort`) — the only door infrastructure may knock on.
    - `service/` + `service/impl/`: use-case logic (`IXxxService` / `XxxService`). This is the domain's equivalent of a
      single entry point per feature — controllers and other adapters call the service, never a repository directly.
    - `model/`, `validator/`, `handler/`, `annotation/`, `constant/`, `enumeration/`, `extension/`: supporting domain
      vocabulary.
3. **`infrastructure/`**: split into **driving** (called by the outside world) and **driven** (called by the domain)
   adapters. `infrastructure.driving` must never depend on `infrastructure.driven` — the REST layer reaches persistence
   only through domain `port` interfaces.
    - `driving/api/controller/`: one contract interface per resource/version (`IXxxV1Controller`, `IXxxV2Controller`)
      carrying `@RequestMapping`, `@PreAuthorize`, OpenAPI annotations and bean-validation constraints;
      `controller/impl/` holds the implementation, which only maps DTOs and delegates to the service — no business logic
      in a controller impl.
    - `driving/api/dto/` and `driving/api/mapper/`: reader (response) / writer (request) DTOs and their DTO ↔
      domain-model mappers. No entity ever crosses this boundary.
    - `driven/postgres/`: `entity/` (package-private, R2DBC/jOOQ row mappings), `repository/`, `mapper/` (entity ↔
      domain model), `converter/`, `extension/`.
    - `driven/idp/`: `adapter/`, `entity/`, `mapper/` for the Authentik integration.

### Data Flow: controller → service (via port) → adapter

- The **contract interface** (`IXxxVNController`) is the resource's public shape: routing, security, validation, docs.
  The **impl** is a thin translator (DTO ↔ domain model, delegate to service).
- The **service** (`IXxxService`) is the domain's sole entry point for a feature — equivalent in spirit to a frontend
  facade: it owns the use-case, and every caller (controller, another service) goes through it rather than reaching into
  a `port`/repository directly.
- The **port** is the interface the service depends on; the **driven adapter** (`postgres`/`idp`) implements it.
  Swapping Postgres or the IDP means implementing the same port — the domain never changes.

---

## 4. Coding Standards, Quality & Best Practices

- **Formatting:** No ktlint/detekt configured today — follow standard Kotlin conventions and the repository's existing
  style. Indentation is **tabs**.
- **Code Quality:** Pay special attention to:
    - **API Contract Clarity:** OpenAPI annotations on every endpoint stay accurate — they are the contract, not an
      afterthought.
    - **Performance:** Non-blocking end-to-end; push filtering, sorting, and pagination into SQL rather than in-memory;
      avoid N+1 query patterns across reactive chains.
    - **Readability & Maintainability:** Clean code principles, descriptive naming conventions.
    - **Best Practices:** Errors resolve through i18n message keys (`ErrorConst` + `resources/i18n`), never hardcoded
      English; internal messages and stack traces never reach the client.
    - **No Dead Code:** Remove unused code, methods, imports, and variables rather than leaving them "just in case."
    - **Method Size:** Methods should be **25 lines or fewer**. Split larger methods into smaller, well-named ones.
    - **Naming:** Variable and method names must be explicit and self-descriptive. **Exception — single-parameter
      lambdas:** inside a single-parameter Kotlin lambda (`map { }`, `let { }`, `also { }`, `filter { }`, …), the
      implicit `it` may be used instead of a full descriptive name; this exception applies only there and only when the
      lambda has no more than one parameter.
    - **Reactor Chain Readability:** Factor operators and intermediate steps of a `Mono`/`Flux` chain into small,
      well-named private methods so the overall flow stays easy to read at a glance rather than one long chained pipe.
      Ideally, each operator call fits on a **single line**; if an operator's logic needs to spread onto a second line,
      extract it into its own well-named method instead. Exceptions may be granted but **must be defined with the
      developer** case by case, not assumed.

---

## 5. Testing Guidelines

- **Frameworks:** JUnit5 + Reactor `StepVerifier` and `mockito-kotlin` for unit tests; Testcontainers (PostgreSQL) for
  repository/integration tests; `WebTestClient` for controller contract & `@PreAuthorize` authorization tests; ArchUnit
  for the hexagonal boundary; Kover for the coverage gate.
- **Detailed conventions live in the dedicated skill:** naming, fixtures, and per-layer patterns are defined in
  `.claude/skills/test` (`.github/prompts/test.prompt.md`) — always consult it before writing or extending tests rather
  than improvising a style.
- **Coverage gate:** `koverVerify` must pass. Never lower the gate to make a build pass — raise it with the user
  instead.

---

## 6. Git & Version Control

Each PR is the **smallest testable feature**, stacked on the previous step's branch, to keep code reviews fast and
hazard-free.

- **Stack Branching:** For step *N*, create branch `feat/<feature>/0N-<step-name>` branching from `0N-1` (or `main` for
  step 1).
- **Atomic Implementation:** Implement ONLY the scope of the smallest testable feature for step *N*.
- **Mandatory pre-PR gate:** Run `./gradlew build` and verify it passes cleanly — compile, unit + parameterised tests,
  Testcontainers integration tests, the ArchUnit `HexagonalArchitectureTest`, and the Kover coverage gate
  (`koverVerify`). Do NOT proceed if the build fails, coverage drops below the gate, or the feature can't be verified
  independently.
- **Pushing:** Unlike a purely local workflow, opening a stacked PR requires pushing the step's branch — only after the
  build gate above is green, and only for the step already agreed with the user. Never push directly to `main`.
- **PR Creation:** Open the PR targeting base branch `feat/<feature>/0N-1` (`gh pr create --base feat/<feature>/0N-1`).
- **Confirmation to Continue:** Stop and ask the user for validation before moving to step *N+1*.
- **Sizing:** Keep diffs strictly confined to the single testable feature — aim for minimal file changes and under **100
  lines** where possible, excluding README/doc sync and generated mappers. If a step includes multiple testable
  behaviors (e.g. an endpoint **and** a new cross-field constraint), stop immediately and split it into separate stacked
  sub-branches/PRs.
- **Commits:** Conventional Commits, format `<type>(<scope>): [Step N] <short summary>`, `type` ∈
  `feat, fix, chore, docs, style, refactor, perf, test`. semantic-release derives the version, changelog, and tag from
  them (ADR 009) — a non-conventional message produces a wrong or missing release.
- **Error Recovery:**
    - **Misunderstanding / bug:** Stop immediately. Do not stack patch commits on a broken PR. Explain the issue in 1
      sentence to allow a `git reset`.
    - **Migration mistake:** Never edit an applied `V…` migration — correct it with a new forward-only `V…` file.
    - **Cosmetic tweaks:** Keep modifications localized strictly to the relevant component within the active branch.
- **Work-in-progress gate:** If a feature already implemented locally covers a complete testable functional scope but
  hasn't been committed, pushed, and opened as a PR yet, the agent **refuses to start any new, unrelated feature or
  step**. Only fixes, review feedback, or adjustments to that pending feature are allowed until it is committed and
  pushed. Instead, prompt the developer to commit and push it into a PR first — this keeps the eventual review diff
  small and self-contained.

---

## 7. Configuration & Environment

- **Environment Agnostic:** All environment-specific configuration is passed via JVM system properties (`-D…`) or
  environment variables — the image is immutable, so nothing environment-specific is baked into it. A missing required
  secret **must fail startup loudly**, never silently default.
- **Backend Abstraction:** Domain never depends on infrastructure directly (ArchUnit-enforced) — DTO ↔ domain-model
  mappers (`driving/api/mapper`) and entity ↔ domain-model mappers (`driven/postgres/mapper`) keep both boundaries
  decoupled, even when the shapes are identical. Postgres `entity` classes stay package-private; no entity ever crosses
  the API boundary.

### Configuration keys

| Group                | Keys                                                                                                                                |
|----------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| Datasource           | `registry.datasource.base-url` (host:port, no scheme), `.database`, `.schemas`, `.username`, `.password`                            |
| IDP                  | `external.idp.jwks-uri`, `.authorization-uri`, `.token-uri`, `.end-session-uri`, `.client-id`, `.client-secret`                     |
| CORS                 | `external.cors.urls` — comma-separated allow-list, **never** `*`                                                                    |
| Server               | `registry.server.port` (8081), `registry.server.logging-level`                                                                      |
| Features             | `registry.feature.documentation.enabled` (Swagger), `registry.feature.observability.enabled` (Prometheus)                           |
| In `application.yml` | Per-picker `/search/**` result caps; `registry.feature.purge.*` — four cron expressions + four month thresholds (default 12 months) |

- **Local run:** `cd local-dev && cp .example.env .env && docker compose up -d` (PostgreSQL + Authentik), then
  `./gradlew bootRun` (infra settings as `-D…` VM options).

---

## 8. Comments Policy

- **No Superfluous Comments:** Keep comments synthetic, modern, and state-of-the-art.
- **Prohibitions:** Do not write comments about migrations, past states, or changelogs — the migration file history and
  `CHANGELOG.md` already carry that.
- **Future Actions:** Any comment referring to future tasks or technical debt must be explicitly marked as `// TODO:`.
- **Permission Required:** The agent **must always ask the user for permission** before adding any comment to the code,
  **except** for the two pre-approved, standing rules below (header comments and endpoint documentation), which the
  agent applies without asking each time.

### 8.1 File Header Comments (pre-approved)

- **Scope — which files get one:** Every class/interface/object that is **not** passive, and not already fully
  self-explanatory from its type/nature alone. Concretely:
    - **Excluded** (no header comment): `model`, `dto`, `entity` (passive data holders); `constant`, `enumeration`
      (plain vocabulary, same bucket as a passive holder); `mapper`, `annotation`, `validator` (their name already
      says exactly what they do — a mapper maps A→B, a validator validates X, nothing more to add).
    - **Included:** everything else that carries behavior — `service`/`service/impl`, `port`, `repository`/
      `repository/impl`, `handler`, `extension`, `config`, controller-advice classes, …
    - Controller contract interfaces and their `impl` (`IXxxVNController` / `XxxVNController`) are a special case —
      see §8.2 instead of a header comment.
- **Placement & format:** A `/** … */` KDoc block directly above the file's primary top-level declaration (`class`,
  `interface`, `object`), after the `package`/`import` lines. 2–4 lines, in English (§1).
- **Content — class-level only, not method-by-method:** What the class/interface is responsible for as a whole, plus
  its scope and its limits (what it deliberately does **not** handle, or what it delegates to, when that's
  non-obvious). Do **not** restate or summarize individual methods — that granularity belongs to the code itself
  (self-descriptive naming) or, case-by-case, an inline comment still gated by the permission rule above.
- **Keep it in sync:** If a change to the file alters what it does or its scope, update its header in the same
  commit/PR — a stale header is worse than none.

### 8.2 Endpoint Documentation (pre-approved)

- **No Javadoc/KDoc on controllers.** A contract interface (`IXxxVNController`) and its `impl` never get a header
  comment (§8.1) or per-method KDoc. The contract **is** self-documenting through Swagger/OpenAPI.
- **Every endpoint's `@Operation` carries a real `description`,** not a one-line restatement of the `summary`: the
  functional steps it performs (validations, side effects, what changes downstream, notifications, cascades, …),
  the meaning of non-obvious query params/filters, and any pagination/cap behavior — written so it reads well
  rendered in Swagger UI. Add a request-body `@ExampleObject` where a realistic payload materially helps (skip it for
  trivial/no-body endpoints).
- **Keep it in sync:** Whenever an endpoint's behavior changes — in code review or as a live fix — its `@Operation`
  description (and example, if now stale) **must** be updated in the same change. A PR/commit that changes an
  endpoint's behavior without touching its description is incomplete.

---

## 9. Non-Negotiable Technical Invariants

Non-negotiable constraints from the ADRs and the "accepted risks" tables — a change must not silently violate any of
these:

- **Hexagonal boundaries are build-enforced.** `HexagonalArchitectureTest` (ArchUnit) runs on every `./gradlew build`; a
  violation fails the build. The root package `fr.laucoin.registry.backend` contains only `config/`, `domain/`,
  `infrastructure/` — nothing else. (ADR 001)
- **Driving/driven adapter split.** `infrastructure/driving/api` is the REST layer (driving adapter);
  `infrastructure/driven/postgres` and `infrastructure/driven/idp` are the driven adapters. `infrastructure.driving`
  must not depend on `infrastructure.driven` — the REST layer reaches persistence only through domain `port` interfaces.
- **Every `@RestController` implements a contract interface** carrying the `@RequestMapping`, `@PreAuthorize`, OpenAPI
  annotations and bean-validation constraints; the impl only maps DTOs and delegates. No endpoint ships without an
  authorization rule, except `/api/v*/metadata/**`: global, static, tenant-free reference data with zero criticality,
  open to any authenticated user — `@PreAuthorize` is intentionally omitted there and the global `authenticated()`
  filter is the only gate. (ADR 001)
- **No entity crosses the API boundary.** Reader (response) / writer (request) DTOs only; Postgres `entity` classes are
  package-private to `infrastructure.driven.postgres` (ArchUnit).
- **Non-blocking on the request path.** No `.block()`, `Thread.sleep`, or blocking JDBC/file IO. The only JDBC use is
  Flyway at boot. Push filtering, sorting and pagination into SQL. Multi-step writes run inside
  `transactionalOperator::transactional`. (ADR 002)
- **Flyway owns the schema; migrations are forward-only** (`V1_0_0` …). R2DBC runtime never issues DDL. Never edit an
  applied migration — add a new `V…` file. (ADR 002, ADR 006)
- **No unbounded collection is ever returned.** List endpoints paginate (`pageNumber` ≥ 0 default 0; `pageSize` 1–200
  default 20 → `PageModel`); `/search/**` pickers return a configured cap (default 10). (ADR 006)
- **Errors are i18n message keys** resolved through `ErrorConst` + `resources/i18n` bundles (en default, fr) via
  `RegistryControllerAdvice`; never hardcoded English. Internal messages and stack traces never reach the client.
- **Multi-tenant isolation is the `{projectId}_{PERMISSION}` string namespacing.** Project-scoped checks go through the
  custom `PermissionEvaluator` (`hasPermission(#projectId, 'X')`); option-gated endpoints carry both an option check and
  a permission check. Never weaken a project resource to an unscoped `hasAuthority` check. (ADR 005)
- **RBAC seed migrations and `@PreAuthorize` strings must stay in lockstep** — a permission renamed in one place but not
  the other fails silently as a denied check. Roles/permissions are data, loaded into an in-memory map at startup; a
  change needs a migration **and** a restart. Authorities are recomputed only at token conversion — there is no
  mid-session revocation. (ADR 005)
- **Authentication stays delegated — no local password store.** Only four public endpoints (`/authentication/login/uri`,
  `/logout/uri`, `/token`, `/token/refresh`); `GET /`, Swagger/`api-docs`, `/actuator/**` are permitted unauthenticated
  but serve content only when their feature flag is on. The JWT converter refuses blocked (`423`) and anonymized (`409`)
  accounts and JIT-provisions first-time users with the default `USER` role. (ADR 004)
- **`/api/v2` exists alongside `/api/v1` for every resource.** Each `IXxxV2Controller` follows the new conventions:
  `page`/`size` + `sort`/`direction` instead of `pageNumber`/`pageSize` with no sorting, `q` instead of `textSearched`,
  filter params without the `*Searched` suffix, `POST` instead of `PATCH` for `disable`/`enable`/`block`/`unblock`,
  `201`+`Location` on create, `204` on delete, and no domain model (`PageModel`, `PreferencesModel`,
  `ProjectStatusModel`, `VehicleStatusModel`, `AuthenticationUriModel`, `AuthenticationInfoModel`) crossing the API
  boundary. Every `IXxxV1Controller` is marked `@Deprecated(level = WARNING)` with `deprecated = true` on each
  `@Operation` — v1 keeps running unchanged, nothing is deleted. `@RateLimited(SENSITIVE|SEARCH)` gates v2
  mutation/search endpoints via `AnnotationRateLimitHandler`, mirroring the pre-existing
  `AuthenticationRateLimitHandler` pattern.
- **Retention purges are irreversible and gated.** `/api/v1/purge/**` and `/api/v2/purge/**` require `REGISTRY_JOB_C`
  (held only by `USER_ADMINISTRATOR` / the single `SERVICE_ACCOUNT`); `dryRun` **defaults to `true`**; thresholds are
  configuration, not code; the four sweeps are staggered content-before-configuration to respect delete dependencies.
  There is no export before deletion. (ADR 011)
- **Last-administrator safety.** The system refuses to remove or demote the last *permanent* (no end date) level-0
  administrator of the platform or of a project. A temporary/support profile never counts toward this safeguard.
- **Immutable, hardened image.** Distroless Java 25, non-root, port 8081; secrets arrive as env/JVM config, never baked
  in. CI ships the JVM jar (not a GraalVM native image). semantic-release → GHCR, retain last 5. (ADR 009)

---

## 10. Documentation (`doc.laucoin.fr`)

- **Reference:** [https://doc.laucoin.fr/registry](https://doc.laucoin.fr/registry)
- **Rule:** The agent **must always ask the user** if documentation updates are relevant for any given feature or
  change, and **where** the documentation is located before attempting any updates.
- **Conflict/Missing** If a user request, or a specification fetched from the documentation hub during spec-driven
  development, would require violating a rule in this document, stop before implementing it. Do not silently comply with
  the request, and do not silently ignore the spec. Propose one of two paths to the user: update this document to
  reflect the new rule, or adjust the request/spec to fit the existing rule. Proceed only once the user has picked one.

---

## 11. Developer Instructions (Manual — Preserved on Regeneration)

Ad hoc rules a developer has added directly to this file — process or behavioral preferences with no spec page to derive
them from. On regeneration, copy this section verbatim; never rewrite, prune, or re-derive its contents.

- [None yet]
