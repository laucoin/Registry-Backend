# AGENTS.md - Development Guidelines for Kotlin / Spring WebFlux Backend

Strict guidelines for AI agents and developers working on this reactive Kotlin backend. Specifications live in a
**separate repository** (the documentation hub): spec commits never land here, code commits never land there.

---

## 1. Language & Communication

- **User Interaction:** All direct communication, questions, and validations with the user **must** be conducted in
  **French**. Be concise and factual: no pleasantries, 1–2 simple sentences per explanation, except when a decision is
  requested (see below).
- **Interactive mode first:** always prefer the interactive mode (`AskUserQuestion`) over a free-text question or a
  silent assumption whenever a decision belongs to the user (design choice, scope, naming, trade-off, ambiguous
  request, documentation update). Each question must carry enough context to decide without re-reading the code:
  what is at stake, the relevant code or spec reference (`file:line`), 2–4 options each with its consequences and
  trade-offs, and a recommended option placed first. Group related decisions in a single call. Do not ask when a
  convention of this document or the existing code already settles the point.
- **Code & Comments:** All source code, identifiers, commit messages, and error-key defaults **must** be written in
  **US English**. Comments and KDoc **must** be written in **French**; the `// Arrange`, `// Act`, `// Assert` test
  markers stay as is.
- **Strict Scope:** Address only the requested task. Do not refactor surrounding code or fix unrelated items.

---

## 2. Tech Stack

- **Language & Runtime:** Kotlin on JDK 25, Spring Boot WebFlux (fully reactive: `Mono` / `Flux`).
- **Persistence:** PostgreSQL through R2DBC; **Flyway** owns the schema (JDBC is used by Flyway at boot only).
- **Security:** Spring Security resource server (JWT), authentication delegated to an external ID provider (Authentik).
- **Build & Quality:** Gradle, JUnit, `mockito-kotlin`, `StepVerifier`, `WebTestClient`, Testcontainers, ArchUnit
  (`HexagonalArchitectureTest`), Kover (`koverVerify` coverage gate).
- **Indentation:** **tabs** — match the repository convention.

---

## 3. Architecture

The root package `fr.laucoin.registry.backend` contains **only** `config/`, `domain/`, `infrastructure/` (ADR 001).
Boundaries are enforced by ArchUnit on every `./gradlew build`.

- **`domain/`**: business models, `port` interfaces, service use-cases. Never depends on `infrastructure`.
- **`infrastructure/driving/api`**: REST layer — controller contract interfaces, controllers, reader/writer DTOs, mappers.
- **`infrastructure/driven/postgres` and `driven/idp`**: driven adapters — entities, repositories, mappers, IDP client.
- **`config/`**: Spring configuration (security, CORS, properties binding, i18n, OpenAPI). Cross-cutting concerns live
  here; business capabilities live in `domain/` with adapters under `infrastructure/`, grouped by resource.

**Data flow:** controller → service → port → adapter. The contract interface carries the endpoint metadata
(`@RequestMapping`, `@PreAuthorize`, OpenAPI, bean validation); the `@RestController` only maps DTOs and delegates to a
domain service; the service owns the business rules and reaches the outside only through ports; the port
implementation does the I/O and maps entities to domain models. Multi-step writes run inside
`transactionalOperator::transactional` (ADR 002). The prohibitions are listed in §11.

---

## 4. Configuration Principles

- **Environment agnostic:** the image is immutable. All configuration arrives as JVM system properties (`-D…`) or
  environment variables, bound through typed properties. Never hardcode or bake environment-specific values.
- **Fail loudly:** secrets (datasource password, IDP client secret) are placeholders in `application.yml`; a missing one
  **must fail startup**, never silently default.
- **Explicit mappers** (`entity ↔ domain model ↔ DTO`) even when structures are identical.
- **Time & randomness** come from injected abstractions (e.g. `Clock`) so they stay testable.
- The configuration keys are documented in `README.md` (single reference).

---

## 5. Coding Standards

- **Security:** validate every input, authorize every endpoint, never leak internals (see §11).
- **Performance:** non-blocking pipelines; SQL-side filtering, sorting and pagination; no N+1; no unbounded collection.
- **Kotlin:** immutability by default (`val`, `data class`); null-safety instead of sentinel values.
- **Errors:** i18n message keys resolved through `ErrorConst` + `resources/i18n` bundles (en default, fr) via
  `RegistryControllerAdvice`. Never hardcode English in a response.
- **Reactive discipline:** handle errors inside the pipeline (`onErrorMap`, `onErrorResume`) and map them to domain
  errors; never swallow an error, never `subscribe()` in a request path, never return `null` from a reactive method.
- **Reactive readability:** one operator per line; if an operator's logic needs a second line, extract it into a
  well-named private method. Exceptions are agreed with the developer case by case.
- **Method size:** 25 lines or fewer (tests included).
- **Naming:** explicit and self-descriptive. A lambda with a single parameter may use a one- or two-letter name (`it`).
- **No dead code:** remove unused code, methods, imports and variables.

---

## 6. Skills (mandatory routing)

When a request matches a skill, **load and follow it before doing anything else**. Do not improvise the procedure.

| Request                                                    | Skill         |
|------------------------------------------------------------|---------------|
| Review a diff, file or branch; "review", "check my change" | `code-review` |
| Add, write, extend or fix tests                            | `test`        |
| Create a commit, plan commit scopes                        | `commit`      |
| Compare the code against the documentation                 | `doc-drift`   |

`code-review` and `test` run **only** before a commit or when the user asks for them; never automatically after an
edit. Before each commit, `code-review` must give every one of its 4 ratings ≥ 8/10, and the new code must be covered
by tests.

---

## 7. Build, Tests & Commands

- **Gate:** `./gradlew build` (compile, unit, Testcontainers integration, ArchUnit, `koverVerify`) is the pre-commit
  authority. `./gradlew test` is a faster inner loop. Coverage thresholds only go up.
- **Run locally:** `./gradlew bootRun` (pass infra settings as `-D…` VM options).
- **Local dependencies:** `cd local-dev && cp .example.env .env && docker compose up -d` (PostgreSQL + Authentik).
- Test writing conventions (AAA, fixtures, naming) are in the `test` skill.

---

## 8. Documentation

- **Default source:** `https://doc.laucoin.fr/registry/functional` (technical specs under `/technical`). If a local
  copy may be fresher, propose it to the user instead of asking where the specs are.
- Always ask the user whether documentation updates are relevant to a change, and where the documentation is located,
  before updating anything.
- **Separation:** never modify documentation during code steps, never touch backend source during a specification step.
  A scope change or unforeseen case means updating the specification **first**.
- **Conflict/Missing:** if a request or a spec would require violating a rule of this document, stop. Propose either to
  update this document or to adjust the request/spec, and proceed only once the user has picked one.
- `README.md` and internal configuration files must be updated whenever an API, feature, configuration key, migration or
  dependency changes. The README keeps an exhaustive **"Comment l'installer et l'utiliser ? ⚙️"** section (prerequisites,
  configuration keys with defaults, local setup, build/run/test commands).
- ADRs **001–006, 009, 011** govern the backend (007, 008, 012 are frontend-scoped).

---

## 9. Comments Policy

- Ask the user before adding any comment. Keep comments synthetic; none about migrations, past states or changelogs. Write them in French.
- Future work or technical debt is marked `// TODO:`.
- **Standing exception — KDoc header:** every class, interface or non-trivial unit that is not passive (anything outside
  domain models, DTOs, entities and mappers) gets exactly this block above its declaration:

```kotlin
/**
 * Objectif : <ce qu'il fait, une à deux phrases, sans détail par méthode>
 * Périmètre : <ce dont il est propriétaire / responsable>
 * Limites : <ce qu'il ne fait volontairement pas, ou ses bornes connues>
 */
```

---

## 10. Git

- **Commits:** the agent may commit, in Conventional Commits format `<type>(<scope>): <short summary>`,
  `type` ∈ `feat, fix, chore, docs, style, refactor, perf, test` (semantic-release derives versions from them, ADR 009).
  Use the `commit` skill.
- **The git remote is read-only:** no write action on it (push, creating or editing a pull request, issue or release,
  etc.). Reads (fetch, viewing pull requests and issues) are allowed. Write actions are also denied in
  `.claude/settings.json`.
- **Scopes** are planned and agreed with the user before development begins.
- **Pre-commit:** `./gradlew build` must pass. Never commit a failing build or a coverage drop.
- **Work-in-progress gate:** while a complete, testable feature is implemented but uncommitted, refuse any new unrelated
  feature; only fixes, review feedback and adjustments to the pending feature are allowed.
- **Misunderstanding / bug:** stop immediately, never stack patch commits on a broken change. Explain the issue in one
  sentence so the user can `git reset`.

---

## 11. Non-Negotiable Technical Invariants

A change must not silently violate any of these:

- **Hexagonal boundaries are build-enforced** by `HexagonalArchitectureTest`. `infrastructure.driving` never depends on
  `infrastructure.driven`: the REST layer reaches persistence only through domain ports. (ADR 001)
- **Every `@RestController` implements a contract interface** carrying `@RequestMapping`, `@PreAuthorize`, OpenAPI
  annotations and bean-validation constraints. No endpoint ships without an authorization rule. (ADR 001)
- **The backend enforces everything.** The frontend is never a security boundary.
- **No entity crosses the API boundary.** Postgres entities are package-private to `infrastructure.driven.postgres`;
  reader/writer DTOs only, never a domain model or entity as payload.
- **Non-blocking on the request path.** No `.block()`, `Thread.sleep`, blocking JDBC/file IO. The only JDBC use is
  Flyway at boot. (ADR 002)
- **Flyway owns the schema; migrations are forward-only** (`V1_0_0` …). R2DBC runtime never issues DDL. Never edit an
  applied migration. (ADR 002, ADR 006)
- **SQL is always parameterized.** Never concatenate user input.
- **No unbounded collection is ever returned.** List endpoints paginate (`page` ≥ 0 default 0; `size` 1–200
  default 20 → `PageModel`); `/search/**` pickers return a configured cap (default 10). (ADR 006)
- **Errors are i18n message keys** via `ErrorConst` + `RegistryControllerAdvice`. Internal messages and stack traces
  never reach the client.
- **Multi-tenant isolation is the `{projectId}_{PERMISSION}` string namespacing.** Project-scoped checks go through the
  custom `PermissionEvaluator` (`hasPermission(#projectId, 'X')`); option-gated endpoints carry both an option check and
  a permission check. Never weaken a project resource to an unscoped `hasAuthority`. (ADR 005)
- **RBAC seed migrations and `@PreAuthorize` strings stay in lockstep.** Roles/permissions are data loaded into memory
  at startup: a change needs a migration **and** a restart. Authorities are recomputed only at token conversion — no
  mid-session revocation. (ADR 005)
- **Authentication stays delegated — no local password store.** Only four public endpoints
  (`/authentication/login/uri`, `/logout/uri`, `/token`, `/token/refresh`); `GET /`, Swagger/`api-docs` and
  `/actuator/**` are permitted unauthenticated but serve content only when their feature flag is on. The JWT converter
  refuses blocked (`423`) accounts, rejects email conflicts (`409`), and JIT-provisions first-time users with the
  default `USER` role. (ADR 004)
- **No secret or personal data in logs or responses** beyond what the endpoint's contract requires.
- **CORS is an explicit allow-list**, never `*`.
- **One API version: `/api/v2`.** The irregular endpoints listed in `api-reference.md`
  (`.../profiles/{id}/accept/{accepted}`, `.../alerts/{id}/status/{status}`, the `impersonate` naming) are **frozen**.
- **Retention purges are irreversible and gated.** `/api/v2/purge/**` requires `REGISTRY_JOB_C`; `dryRun` **defaults to
  `true`**; thresholds are configuration, not code; the four sweeps are staggered content-before-configuration. No
  export before deletion. (ADR 011)
- **Last-administrator safety.** Never remove or demote the last *permanent* (no end date) level-0 administrator of the
  platform or of a project. A temporary/support profile never counts.
- **Immutable, hardened image.** Distroless Java 25, non-root, port 8081; secrets arrive as env/JVM config. CI ships the
  JVM jar. semantic-release → GHCR, retain last 5. (ADR 009)
