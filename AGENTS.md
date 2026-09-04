# Instructions for AI Agents — Spec-Driven Development & Stacked PRs

## 1. Project Context & Documentation Resolution

- **Target Project:** Registry
- **Scope:** Backend
- **Target Repository:** `Registry-Backend` (Kotlin / Spring WebFlux). Specifications live in a **separate
  repository** — the documentation hub (`documentation/registry/`). Spec commits never land here; code commits never
  land there.
- **Default Documentation URL:** `https://doc.laucoin.fr/registry`

### Agent Rule for Doc Resolution:

Before implementing any feature or reading a specification:

1. Check if a local path (e.g., `documentation/registry`) or specific URL was supplied in the user's prompt.
2. If unspecified, ask the user before proceeding:
   > *"Should I fetch the specification from the default URL (`https://doc.laucoin.fr/registry`) or a local path?"*

### Where the backend specs live:

| Source                                                      | What it holds                                                                                                                                                                                                    |
|-------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `functional/roles-and-permissions.md`                       | Security baseline — auth model, the two permission planes, global & project roles, the access matrices, the reciprocity rule for new roles                                                                       |
| `functional/features/*.md`                                  | Per-feature rules + BDD/Gherkin scenarios: `projects`, `project-profiles`, `participants`, `groups`, `movements`, `vehicles`, `activities`, `communications`, `alerts`, `users`, `preferences`, `data-retention` |
| `functional/domain-model.md`, `personas.md`, `workflows.md` | Business vocabulary, actors, end-to-end journeys                                                                                                                                                                 |
| `technical/backend.md`                                      | The backend engineering spec — stack, hexagonal layering, controller pattern, reactive discipline, validation, configuration, testing                                                                            |
| `technical/architecture.md`                                 | Hexagonal + reactive shape, request flow, deployment topology                                                                                                                                                    |
| `technical/security.md`                                     | Enforcement view — auth flow, JWT→user conversion, RBAC enforcement, visibility gating, data protection                                                                                                          |
| `technical/data-model.md`                                   | PostgreSQL schema, column conventions, tables, trigram search, migration history                                                                                                                                 |
| `technical/api-reference.md`                                | Every `/api/v1` endpoint grouped by domain, with its required permission                                                                                                                                         |
| `technical/getting-started.md`                              | Running the full stack locally                                                                                                                                                                                   |
| `technical/adr/`                                            | ADRs **001–006, 009, 011** govern the backend (007, 008, 012 are frontend-scoped — ignore them here)                                                                                                             |

## 2. Communication Style & Behavioral Rules

- **Absolute Conciseness:** Direct, factual, no pleasantries or theoretical ramblings.
- **Simplicity:** No academic or unnecessarily complex jargon. Explain actions in 1–2 simple sentences.
- **Strict Scope:** Address only the requested task. Do not refactor surrounding code or fix unrelated items.
- **Language:** Reply to the user in their language; every file written to the repository (code, comments, commit
  messages) is **English only**.

## 3. Spec-Driven Development (SDD) Protocol

Strict separation must be maintained between documentation/specs and implementation code — and here they are literally
different repositories.

### Phase 1: Specification (VitePress — the documentation hub)

- Create or update specifications in `documentation/registry/` (functional first, then technical — never draft technical
  before the functional spec and `roles-and-permissions.md` baseline are settled).
- Slice specifications into the **smallest testable features**. For this backend that means, per resource:
    - `Step 1: Flyway migration + R2DBC entity & mapper + repository tests` (Testcontainers).
    - `Step 2: Domain model, port interface & service use-case + unit tests` (`StepVerifier`, `mockito-kotlin`).
    -
    `Step 3: One controller contract interface + @PreAuthorize + writer/reader DTOs + one endpoint + WebTestClient contract & authorization test`.
    - Cross-field validation constraints, option-gating, and each state-transition endpoint (`/disable`, `/enable`,
      `/block`, …) are their own steps.
- **FORBIDDEN:** Do not touch `Registry-Backend` source or `.vitepress/config.*` during this phase.

### Phase 2: Implementation via GitHub PR Stacks (`Registry-Backend`)

- Base implementation **exclusively** on the validated specification step fetched from the resolved documentation
  source.
- Deliver every single implementation step as an isolated GitHub PR stacked on the previous step's branch.
- **FORBIDDEN:** Do not modify documentation files during code implementation steps.

## 4. Git Strategy & GitHub Stacked PRs Execution

Each PR must represent the **smallest testable feature** to ensure fast, hazard-free code reviews. All branch/PR
commands below run inside `Registry-Backend`.

1. **Stack Branching:** For step $N$, create branch `feat/<feature>/0N-<step-name>` branching from `0N-1` (or `main` for
   step 1).
2. **Atomic Implementation:** Implement ONLY the scope of the smallest testable feature for step $N$.
3. **MANDATORY Pre-PR Testing & Verification:**

- Run `./gradlew build` and verify it passes cleanly — it compiles, runs unit + parameterised + Testcontainers
  integration tests, the ArchUnit `HexagonalArchitectureTest`, and the Kover coverage gate (`koverVerify`).
- Do NOT proceed if the build fails, if coverage drops below the gate, or if the feature cannot be verified
  independently.

4. **GitHub PR Creation:**

- Open/create the Pull Request targeting base branch `feat/<feature>/0N-1` (using
  `gh pr create --base feat/<feature>/0N-1`).

5. **Confirmation to Continue:** Stop and ask the user for validation before moving to step $N+1$.

## 5. Requirement Validation & Internal Documentation / README Updates

Before marking any task or PR step as complete:

1. **Validation Against Specification:**

- Explicitly verify that the written code strictly matches every functional and technical requirement stated for this
  testable feature — including the `@PreAuthorize` expression, option gate, validation constraints, pagination bounds,
  and i18n error keys called out in `api-reference.md` / `security.md`.
- Ensure zero regressions: `./gradlew build` green, ArchUnit clean, coverage gate held.

2. **README.md & Internal Doc Synchronization:**

- Update `README.md` and internal configuration files whenever an API, feature, configuration key, migration, or
  dependency changes.
- The `README.md` MUST include an exhaustive **"How to install and use it? ⚙️"** section detailing:
    - Prerequisites & runtime versions (JDK 25, Docker + Compose for PostgreSQL and the OIDC provider).
    - Configuration keys (see §8) with defaults and descriptions.
    - Local setup & installation steps (`local-dev/compose.yml`, the `-D` VM options, `./gradlew bootRun`).
    - Build, run, test, and verification commands.

## 6. Smallest Testable Feature Sizing Limits

- **Scope Rule:** Keep diffs strictly confined to the single testable feature — aim for minimal file changes and under
  **100 lines** where possible, excluding README/doc sync and generated mappers.
- **Atomic Commits:** Format `<type>(<scope>): [Step N] <short summary>`. `type` ∈
  `feat, fix, chore, docs, style, refactor, perf, test`. Commits **must** follow Conventional Commits — semantic-release
  derives the version, changelog, and tag from them (ADR 009); a non-conventional message produces a wrong or missing
  release.
- If a step includes multiple testable behaviors (e.g. an endpoint **and** a new cross-field constraint), stop
  immediately and split it into separate stacked sub-branches/PRs.
- Indentation is **tabs** — match the repository convention.

## 7. Adjustments & Error Recovery

- **Misunderstanding / Bug:** Stop immediately. Do not stack patch commits on a broken PR. Explain the issue in 1
  sentence to allow a `git reset`.
- **Scope Change / Unforeseen Case:** Update the specification documentation in the hub FIRST. Do not code until the
  spec commit is created.
- **Migration mistake:** Never edit an applied `V…` migration — correct it with a new forward-only `V…` file.
- **Cosmetic tweaks:** Keep modifications localized strictly to the relevant component within the active branch.

## 8. Project Commands

- **Local Specs Preview:** `pnpm dev` — run in the **documentation hub repo**, not in `Registry-Backend`.
- **Tests:** `./gradlew build` — the full gate (compile, unit, parameterised, Testcontainers integration incl.
  `@PreAuthorize` authorization tests, ArchUnit, `koverVerify` + `koverHtmlReport`). `./gradlew test` for a faster inner
  loop, but `build` is the pre-PR authority.
- **Build / Verification:** `./gradlew build` → `build/libs/*.jar`. Run locally with `./gradlew bootRun` (pass infra
  settings as `-D…` VM options).
- **Dependencies for local run:** `cd local-dev && cp .example.env .env && docker compose up -d` (PostgreSQL +
  Authentik).

### Configuration keys (JVM system properties `-D…` or environment variables — the image is immutable)

| Group                | Keys                                                                                                                                |
|----------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| Datasource           | `registry.datasource.base-url` (host:port, no scheme), `.database`, `.schemas`, `.username`, `.password`                            |
| OIDC                 | `external.oidc.jwks-uri`, `.authorization-uri`, `.token-uri`, `.end-session-uri`, `.client-id`, `.client-secret`                    |
| CORS                 | `external.cors.urls` — comma-separated allow-list, **never** `*`                                                                    |
| Server               | `registry.server.port` (8081), `registry.server.logging-level`                                                                      |
| Features             | `registry.feature.documentation.enabled` (Swagger), `registry.feature.observability.enabled` (Prometheus)                           |
| In `application.yml` | Per-picker `/search/**` result caps; `registry.feature.purge.*` — four cron expressions + four month thresholds (default 12 months) |

Secrets (datasource password, OIDC client secret) are placeholders in `application.yml` — a missing one **must fail
startup loudly**, never silently default.

## 9. Project Invariants

Non-negotiable constraints from the ADRs and the "accepted risks" tables — a change must not silently violate any of
these:

- **Hexagonal boundaries are build-enforced.** `HexagonalArchitectureTest` (ArchUnit) runs on every `./gradlew build`; a
  violation fails the build. The root package `fr.laucoin.registry.backend` contains only `config/`, `domain/`,
  `infrastructure/` — nothing else. (ADR 001)
- **Inverted adapter naming.** `infrastructure/out/api` is the REST layer; `infrastructure/in/postgres` and
  `infrastructure/in/keycloak` are the driven adapters. `infrastructure.out` must not depend on `infrastructure.in` —
  the REST layer reaches persistence only through domain `port` interfaces.
- **Every `@RestController` implements a contract interface** that carries the `@RequestMapping`, `@PreAuthorize`,
  OpenAPI annotations and bean-validation constraints; the impl only maps DTOs and delegates. No endpoint ships without
  an authorization rule. (ADR 001)
- **No entity crosses the API boundary.** Reader (response) / writer (request) DTOs only; Postgres `entity` classes are
  package-private to `infrastructure.in.postgres` (ArchUnit).
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
- **One API version: `/api/v1`.** There is no `/api/v2`. The irregular endpoints listed in `api-reference.md`
  (`.../profiles/{id}/accept/{accepted}`, `.../alerts/{id}/status/{status}`, the `impersonate` naming) are **frozen** —
  do not "fix" them with a breaking change.
- **Retention purges are irreversible and gated.** `/api/v1/purge/**` requires `REGISTRY_JOB_C` (held only by
  `USER_ADMINISTRATOR` / the single `SERVICE_ACCOUNT`); `dryRun` **defaults to `true`**; thresholds are configuration,
  not code; the four sweeps are staggered content-before-configuration to respect delete dependencies. There is no
  export before deletion. (ADR 011)
- **Last-administrator safety.** The system refuses to remove or demote the last *permanent* (no end date) level-0
  administrator of the platform or of a project. A temporary/support profile never counts toward this safeguard.
- **Immutable, hardened image.** Distroless Java 25, non-root, port 8081; secrets arrive as env/JVM config, never baked
  in. CI ships the JVM jar (not a GraalVM native image). semantic-release → GHCR, retain last 5. (ADR 009)

## 10. Developer Instructions (Manual — Preserved on Regeneration)

Ad hoc rules a developer has added directly to this file — process or behavioral preferences with no spec page to derive
them from. On regeneration, copy this section verbatim; never rewrite, prune, or re-derive its contents.

- [None yet]
