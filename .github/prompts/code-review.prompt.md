---
mode: agent
description: Review a change in the Registry backend against project conventions
---

# Code review — Registry Backend

Review the change I point you at (a diff, a file, or the current branch vs `main`). Report findings most-severe first; if nothing is wrong, say so. Prefer concrete, actionable comments with `file:line`.

## What to check

**Correctness**
- Logic, edge cases, null/empty handling, off-by-one, wrong operators.
- Error and authorization paths — is every failure mode handled and mapped correctly?

**Reactive (WebFlux) — high priority**
- No blocking on reactor threads: no `.block()`, `Thread.sleep`, blocking JDBC/IO, or blocking calls inside `map`/`flatMap`.
- Streams are actually subscribed (returned or composed) — no "lost" `Mono`/`Flux` whose side effects never run.
- Correct operator choice: `flatMap` vs `map`, `switchIfEmpty`, `concatMap` ordering, error operators (`onErrorResume`, `onErrorMap`).

**Architecture (hexagonal — enforced by ArchUnit)**
- `domain` stays free of Spring web / persistence types and talks to infra only via `port` interfaces.
- `infrastructure/out` doesn't depend on `infrastructure/in`; nothing depends on `config`; postgres `entity` stays inside its package; controllers implement a contract interface.

**Security — top priority (see AGENTS.md § Security)**
- Every endpoint has an explicit `@PreAuthorize` with the correct `hasPermission(...)` checks (option gate + operation CRUD permission); no missing or overly-broad authorization, no auth bypass.
- Permissions use `ProjectPermissionConst`/`UserPermissionConst` constants, not inline literals.
- Input validated before reaching the domain (`@Valid`, `@Min`/`@Max`, custom `domain/annotation` + `domain/validator`).
- Stays stateless (no session/form-login regressions); CORS not widened to `*`; security headers stay in `HeadersHandler`.
- No tokens/credentials/PII logged; errors go through the advice/handlers and don't leak internals or stack traces.

**API best practices (see AGENTS.md § API)**
- Resource-oriented, versioned URLs (`/api/v1/...`, plural nouns, correct ownership nesting); correct HTTP verb + status codes.
- Controller is an interface with OpenAPI `@Tag`/`@Operation` docs; implementation delegates to a domain service.
- Boundary uses reader/writer DTOs + mappers — never exposes persistence entities.
- Collection endpoints paginate with validated `pageNumber`/`pageSize` and return `PageModel`; no unbounded responses. Breaking changes go in a new version, not `v1`.

**Data**
- Schema changes ship as new Flyway migrations (never edit an applied one); R2DBC queries and mappers are correct; filtering/sorting/pagination pushed to SQL (no load-then-filter, no N+1).

**Style & tests**
- Tabs for indentation; idiomatic Kotlin; matches surrounding code.
- Tests cover the new/changed behavior; coverage stays green (`koverVerify`).

## Output

Group findings by severity (blocker / should-fix / nit). For each: location, what's wrong, why it matters, and a suggested fix. Do not restate unchanged code as findings.
