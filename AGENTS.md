# AGENTS.md - Development Guidelines for Kotlin / Spring WebFlux Backend

This document outlines the strict guidelines, architecture rules, and conventions for AI agents and developers working
on this reactive Kotlin backend. Specifications live in a **separate repository** (the documentation hub): spec commits
never land here, code commits never land there.

---

## 1. Language & Communication

- **User Interaction:** All direct communication, questions, and validations with the user **must** be conducted in
  **French**. Be concise and factual: no pleasantries, 1–2 simple sentences per explanation.
- **Code & Comments:** All source code, identifiers, comments, commit messages, and error-key defaults **must** be
  written in **US English**.
- **Strict Scope:** Address only the requested task. Do not refactor surrounding code or fix unrelated items.

---

## 2. Tech Stack

- **Language & Runtime:** Kotlin on JDK 25, Spring Boot WebFlux (fully reactive: `Mono` / `Flux`).
- **Persistence:** PostgreSQL through R2DBC; **Flyway** owns the schema (JDBC is used by Flyway at boot only).
- **Security:** Spring Security resource server (JWT), authentication delegated to an external ID provider (Authentik).
- **Build & Quality:** Gradle (`./gradlew build`), JUnit, `mockito-kotlin`, `StepVerifier`, `WebTestClient`,
  Testcontainers, ArchUnit (`HexagonalArchitectureTest`), Kover (`koverVerify` coverage gate).
- **Indentation:** **tabs** — match the repository convention.

---

## 3. Architecture & Project Structure

The root package `fr.laucoin.registry.backend` contains **only** `config/`, `domain/`, `infrastructure/` — nothing else
(ADR 001). Boundaries are enforced by ArchUnit on every `./gradlew build`.

1. **`domain/`**: business models, `port` interfaces, and service use-cases. No framework-specific adapter code, no
   dependency on `infrastructure`.
2. **`infrastructure/`**: the adapters.
    - `driving/api` — the REST layer (called by the outside world): controller contract interfaces, controllers,
      reader/writer DTOs, DTO mappers.
    - `driven/postgres` and `driven/idp` — the driven adapters (called by the application): entities, repositories,
      mappers, IDP client.
    - `infrastructure.driving` **never** depends on `infrastructure.driven`: the REST layer reaches persistence only
      through domain `port` interfaces.
3. **`config/`**: Spring configuration (security, CORS, properties binding, i18n, OpenAPI).

### Data Flow: controller → service → port → adapter

- **Controller contract interface** carries `@RequestMapping`, `@PreAuthorize`, OpenAPI annotations, and bean-validation
  constraints. The `@RestController` implementation only maps DTOs and delegates to a domain service. No endpoint ships
  without an authorization rule.
- **Domain service** owns the use-case and the business rules; it talks to the outside only through `port` interfaces.
- **Port implementation** (`driven/postgres`, `driven/idp`) does the I/O and maps entities to domain models.
- **No entity crosses the API boundary.** Postgres `entity` classes are package-private to
  `infrastructure.driven.postgres` (ArchUnit). Reader (response) / writer (request) DTOs only.
- **Non-blocking on the request path.** No `.block()`, `Thread.sleep`, or blocking JDBC/file IO. Push filtering, sorting,
  and pagination into SQL. Multi-step writes run inside `transactionalOperator::transactional` (ADR 002).
- **Placement:** a cross-cutting concern (security, i18n, properties) lives in `config/`; a business capability lives in
  `domain/` with its adapters under `infrastructure/`, grouped by resource.

---

## 4. Abstraction & Environment Configuration

- **Environment Agnostic:** The image is immutable. All configuration arrives as JVM system properties (`-D…`) or
  environment variables and is bound through typed properties. Environment-specific values **must never** be hardcoded or
  baked into the jar/image.
- **Fail Loudly:** Secrets (datasource password, IDP client secret) are placeholders in `application.yml`; a missing one
  **must fail startup**, never silently default.
- **Backend Abstraction:** Strictly decouple layers with explicit mappers (`entity ↔ domain model ↔ DTO`), even when the
  structures are identical. Never expose a domain model or an entity directly as an API payload.
- **Time & Randomness:** Obtain the current time and random values through injected abstractions (e.g. `Clock`) so they
  stay testable.

### Configuration keys

| Group                | Keys                                                                                                                                |
|----------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| Datasource           | `registry.datasource.base-url` (host:port, no scheme), `.database`, `.schemas`, `.username`, `.password`                            |
| IDP                  | `external.idp.jwks-uri`, `.authorization-uri`, `.token-uri`, `.end-session-uri`, `.client-id`, `.client-secret`                     |
| CORS                 | `external.cors.urls` — comma-separated allow-list, **never** `*`                                                                    |
| Server               | `registry.server.port` (8081), `registry.server.logging-level`                                                                      |
| Features             | `registry.feature.documentation.enabled` (Swagger), `registry.feature.observability.enabled` (Prometheus)                           |
| In `application.yml` | Per-picker `/search/**` result caps; `registry.feature.purge.*` — four cron expressions + four month thresholds (default 12 months) |

---

## 5. Coding Standards, Quality & Best Practices

- **Code Quality:** Pay special attention to:
    - **Security:** validate every input, authorize every endpoint, never leak internals (see §11).
    - **Performance:** non-blocking pipelines, SQL-side filtering/sorting/pagination, no N+1 queries, no unbounded
      collections.
    - **Readability & Maintainability:** Clean code principles, descriptive naming, immutability by default (`val`,
      `data class`), null-safety instead of sentinel values.
    - **Error Handling:** Errors are i18n message keys resolved through `ErrorConst` + `resources/i18n` bundles (en
      default, fr) via `RegistryControllerAdvice`. Never hardcode English in a response; internal messages and stack
      traces never reach the client.
    - **No Dead Code:** Remove unused code, methods, imports, and variables rather than leaving them in place "just in
      case."
    - **Method Size:** Methods should be **25 lines or fewer**. Split larger methods into smaller, well-named ones.
    - **Naming:** Variable and method names must be explicit and self-descriptive. **Exception — reactive and
      collection lambdas:** a **single** lambda parameter may be named with one or two letters (e.g. `it`) when the
      lambda has no more than one parameter.
    - **Reactive Readability:** In `Mono` / `Flux` chains, factor operators and intermediate steps into small,
      well-named private methods so the flow stays easy to read at a glance rather than one long chain. Ideally, each
      operator call fits on a **single line**; if an operator's logic needs to spread onto a second line, extract it into
      its own well-named method instead. Exceptions may be granted but **must be defined with the developer** case by
      case, not assumed.
    - **Reactive Discipline:** Handle errors inside the pipeline (`onErrorMap`, `onErrorResume`) and map them to domain
      errors; never swallow an error silently, never `subscribe()` inside a request path, never return `null` from a
      reactive method.

---

## 6. Multi-Agent Review Process (Mandatory Gate)

Before any code modification or new feature is finalized or committed, it **must pass a mandatory multi-agent review**
conducted by 4 distinct virtual agent perspectives.

Each reviewing agent must evaluate the proposed changes, provide feedback if needed, and assign a **formal rating out of
10** for their specific category:

1. **Architecture Agent (`/10`):**
    - Verifies strict adherence to the hexagonal layout (`config/`, `domain/`, `infrastructure/driving|driven`) and the
      ArchUnit rules.
    - Ensures strict data flow enforcement (controller → service → port → adapter); no entity or domain model leaks
      through the API; the contract interface carries the endpoint metadata.
    - Validates that configuration is externalized (never baked into the image) and that Flyway remains the only schema
      owner.

2. **TechLead Agent (`/10`):**
    - Enforces clean code rules, method size limits (<= 25 lines), and explicit naming conventions.
    - Validates reactive readability, absence of blocking calls, correct error mapping inside pipelines, transaction
      boundaries, and Kotlin idioms (null-safety, immutability).
    - Checks absence of dead code, dependency hygiene, and adherence to the non-negotiable invariants.

3. **Testeur / QA Agent (`/10`):**
    - Ensures every service, port implementation, and endpoint has associated tests (`StepVerifier` + `mockito-kotlin`
      for units, Testcontainers for repositories, `WebTestClient` for contract and authorization).
    - Validates strict compliance with the **AAA pattern** and required section comments (`// Arrange`, `// Act`,
      `// Assert`).
    - Verifies edge cases, error branches, authorization allow/deny cases, validation bounds, and that the Kover gate
      holds.

4. **Cybersecurity Expert Agent (`/10`):**
    - Verifies every endpoint carries the right `@PreAuthorize` (project-scoped checks through `hasPermission`, option
      gate where applicable) and that RBAC seed migrations stay in lockstep with the expressions.
    - Checks tenant isolation, injection resistance (parameterized SQL only), input validation and bounds, mass
      assignment, IDOR, and that no sensitive data (tokens, secrets, personal data, stack traces) is logged or returned.
    - Reviews authentication/JWT handling, CORS, security headers, dependency and image hardening, and the
      irreversibility safeguards of destructive operations.

*Review Gate Validation Criteria:*

- A minimum score of **8/10** is required for every individual category.
- If any score falls below **8/10**, the review is rejected and required refactorings must be completed before
  re-evaluation.
- Development is complete only when all 4 agents deliver their rating and give explicit approval.

---

## 7. Testing Guidelines

- **Framework:** JUnit 5 with `mockito-kotlin`, `StepVerifier` (reactor-test), `WebTestClient`, Testcontainers.
- **Gate:** `./gradlew build` is the pre-commit authority: compile, unit, parameterised, Testcontainers integration
  (including `@PreAuthorize` authorization tests), ArchUnit, and `koverVerify`. `./gradlew test` is a faster inner loop.
  Coverage thresholds only go up: raise them whenever the measured coverage rises.
- **Fixtures:** shared test data and builders live in the test sources' shared helpers and are excluded from coverage.
- **Method size:** the 25-line rule applies to tests as well.
- **Pattern:** Strictly use the **AAA pattern**.
- **Comments:** Every test must explicitly include these exact section comments with **no additional detail**:
  ```kotlin
  // Arrange
  // ...

  // Act
  // ...

  // Assert
  // ...
  ```

---

## 8. Documentation (doc.laucoin.fr)

- Reference: `https://doc.laucoin.fr/registry` (or a local `documentation/registry` path).
- Rule: Before implementing any feature or reading a specification, check whether a local path or URL was supplied in
  the prompt; if not, ask the user whether to fetch the specification from the default URL or from a local path. The
  agent must also always ask the user whether documentation updates are relevant for any given feature or change, and
  where the documentation is located, before attempting any updates.
- Separation: the specifications are in a separate repository. Never modify documentation files during code steps, and
  never touch `Registry-Backend` source during a specification step.
- Conflict/Missing: If a user request, or a specification fetched from the documentation hub, would require violating a
  rule in this document, stop before implementing it. Do not silently comply with the request, and do not silently
  ignore the spec. Propose one of two paths to the user: update this document to reflect the new rule, or adjust the
  request/spec to fit the existing rule. Proceed only once the user has picked one. A scope change or unforeseen case
  means updating the specification in the hub **first**.
- `README.md` and internal configuration files must be updated whenever an API, feature, configuration key, migration,
  or dependency changes. The `README.md` keeps an exhaustive **"How to install and use it? ⚙️"** section (prerequisites
  and runtime versions, configuration keys with defaults, local setup, build/run/test/verification commands).

### Where the backend specs live

| Source                                                      | What it holds                                                                                                                                                                                                    |
|-------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `functional/roles-and-permissions.md`                       | Security baseline — auth model, the two permission planes, global & project roles, the access matrices, the reciprocity rule for new roles                                                                       |
| `functional/features/*.md`                                  | Per-feature rules + BDD/Gherkin scenarios: `projects`, `project-profiles`, `participants`, `groups`, `movements`, `vehicles`, `activities`, `communications`, `alerts`, `users`, `preferences`, `data-retention` |
| `functional/domain-model.md`, `personas.md`, `workflows.md` | Business vocabulary, actors, end-to-end journeys                                                                                                                                                                 |
| `technical/backend.md`                                      | The backend engineering spec — stack, hexagonal layering, controller pattern, reactive discipline, validation, configuration, testing                                                                            |
| `technical/architecture.md`                                 | Hexagonal + reactive shape, request flow, deployment topology                                                                                                                                                    |
| `technical/security.md`                                     | Enforcement view — auth flow, JWT→user conversion, RBAC enforcement, visibility gating, data protection                                                                                                          |
| `technical/data-model.md`                                   | PostgreSQL schema, column conventions, tables, trigram search, migration history                                                                                                                                 |
| `technical/api-reference.md`                                | Every `/api/v2` endpoint grouped by domain, with its required permission                                                                                                                                         |
| `technical/getting-started.md`                              | Running the full stack locally                                                                                                                                                                                   |
| `technical/adr/`                                            | ADRs **001–006, 009, 011** govern the backend (007, 008, 012 are frontend-scoped — ignore them here)                                                                                                             |

---

## 9. Comments Policy

- No Superfluous Comments: Keep comments synthetic, modern, and state-of-the-art.
- Prohibitions: Do not write comments about migrations, past states, or changelogs.
- Future Actions: Any comment referring to future tasks or technical debt must be explicitly marked as `// TODO:`.
- Permission Required: The agent must always ask the user for permission before adding any comment to the code.
- Pre-approved exception — file-level header: For every class, interface, or other non-trivial unit that is not
  passive/self-evident by nature (i.e. anything outside domain models, DTOs, entities, and mappers), add a KDoc block
  directly above its declaration, with exactly these three lines and no others:

```kotlin
/**
 * Purpose: <what it does, one to two sentences, no per-method detail>
 * Scope: <what it owns / is responsible for>
 * Limits: <what it deliberately does not do, or its known boundaries>
 */
```

This exception is standing and does not require asking the user each time — every other comment still does.

---

## 10. Git & Version Control

- Commits: The agent is authorized to make commits, in the format `<type>(<scope>): <short summary>` with
  `type` ∈ `feat, fix, chore, docs, style, refactor, perf, test`. Commits **must** follow Conventional Commits —
  semantic-release derives the version, changelog, and tag from them (ADR 009); a non-conventional message produces a
  wrong or missing release.
- Pushing Prohibited: The agent must never perform a `git push`, and never opens a pull request unless explicitly asked.
- Commit Breakdown: Commit scopes must be strictly planned and agreed upon with the user before development begins,
  breaking work down into the smallest functionally testable increments (one behavior per commit; an endpoint and a new
  cross-field constraint are two commits). For a new resource, the natural order is: migration + entity/mapper +
  repository tests; domain model + port + service + unit tests; contract interface + `@PreAuthorize` + DTOs + endpoint +
  contract and authorization tests. Aim for minimal diffs (under ~100 lines where possible, excluding README/doc sync
  and generated mappers).
- Pre-commit verification: `./gradlew build` must pass cleanly (tests, ArchUnit, `koverVerify`). Do not commit if the
  build fails, if coverage drops below the gate, or if the feature cannot be verified independently.
- Work-in-progress gate: If a feature already implemented locally covers a complete testable functional scope but hasn't
  been committed, the agent refuses to start any new, unrelated feature. Only fixes, review feedback, or adjustments to
  that pending feature are allowed until it is committed. Local commits on the current branch are enough: the developer
  decides when to push and open the PR, and may ask the agent to keep working on the same branch meanwhile.
- Misunderstanding / Bug: Stop immediately. Do not stack patch commits on a broken change. Explain the issue in 1
  sentence to allow a `git reset`.

---

## 11. Non-Negotiable Technical Invariants

Constraints from the ADRs and the "accepted risks" tables — a change must not silently violate any of these:

- **Hexagonal boundaries are build-enforced.** A violation of `HexagonalArchitectureTest` fails the build. (ADR 001)
- **Every `@RestController` implements a contract interface** that carries the `@RequestMapping`, `@PreAuthorize`,
  OpenAPI annotations and bean-validation constraints; the impl only maps DTOs and delegates. No endpoint ships without
  an authorization rule. (ADR 001)
- **The backend enforces everything.** The frontend is never a security boundary: every condition is re-checked on every
  request, server-side.
- **No entity crosses the API boundary.** Reader / writer DTOs only.
- **Non-blocking on the request path.** No `.block()`, `Thread.sleep`, or blocking JDBC/file IO. The only JDBC use is
  Flyway at boot. (ADR 002)
- **Flyway owns the schema; migrations are forward-only** (`V1_0_0` …). R2DBC runtime never issues DDL. Never edit an
  applied migration — add a new `V…` file. (ADR 002, ADR 006)
- **SQL is always parameterized.** Never build a query by concatenating user input.
- **No unbounded collection is ever returned.** List endpoints paginate (`pageNumber` ≥ 0 default 0; `pageSize` 1–200
  default 20 → `PageModel`); `/search/**` pickers return a configured cap (default 10). (ADR 006)
- **Errors are i18n message keys** resolved through `ErrorConst` + `resources/i18n` bundles via
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
  but serve content only when their feature flag is on. The JWT converter refuses blocked (`423`) accounts, rejects
  email conflicts (`409`), and JIT-provisions first-time users with the default `USER` role. (ADR 004)
- **No secret or personal data in logs or responses.** Tokens, client secrets, passwords, and personal data are never
  logged nor returned beyond what the endpoint's contract requires.
- **CORS is an explicit allow-list**, never `*`.
- **One API version: `/api/v2`.** `/api/v1` was removed (breaking change, single controlled consumer). The irregular endpoints listed in `api-reference.md`
  (`.../profiles/{id}/accept/{accepted}`, `.../alerts/{id}/status/{status}`, the `impersonate` naming) are **frozen** —
  do not "fix" them with a breaking change.
- **Retention purges are irreversible and gated.** `/api/v2/purge/**` requires `REGISTRY_JOB_C` (held only by
  `USER_ADMINISTRATOR` / the single `SERVICE_ACCOUNT`); `dryRun` **defaults to `true`**; thresholds are configuration,
  not code; the four sweeps are staggered content-before-configuration to respect delete dependencies. There is no
  export before deletion. (ADR 011)
- **Last-administrator safety.** The system refuses to remove or demote the last *permanent* (no end date) level-0
  administrator of the platform or of a project. A temporary/support profile never counts toward this safeguard.
- **Immutable, hardened image.** Distroless Java 25, non-root, port 8081; secrets arrive as env/JVM config, never baked
  in. CI ships the JVM jar (not a GraalVM native image). semantic-release → GHCR, retain last 5. (ADR 009)

---

## 12. Project Commands

- **Tests / full gate:** `./gradlew build` → `build/libs/*.jar`. `./gradlew test` for a faster inner loop.
- **Run locally:** `./gradlew bootRun` (pass infra settings as `-D…` VM options).
- **Dependencies for local run:** `cd local-dev && cp .example.env .env && docker compose up -d` (PostgreSQL +
  Authentik).
- **Local specs preview:** `pnpm dev` — run in the **documentation hub repo**, not in `Registry-Backend`.

---

## 13. Developer Instructions (Manual — Preserved on Regeneration)

Ad hoc rules a developer has added directly to this file — process or behavioral preferences with no spec page to derive
them from. On regeneration, copy this section verbatim; never rewrite, prune, or re-derive its contents.

- **Squash agent fixes.** When the user asks for a fix to a commit the agent made that turned out not to work, commit the
  fix separately first. Once the user confirms it works, squash it into the original commit (fixup) instead of leaving
  a separate fix commit.
