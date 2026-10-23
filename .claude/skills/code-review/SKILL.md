---
name: code-review
description: Review gate for the Registry backend. Use only when the user asks to review, check or audit a diff, file, branch or change, or right before a commit. Never run it automatically after an edit. Rates 4 perspectives out of 10.
---

# Code review — Registry Backend

Review the change pointed at (a diff, a file, or the current branch vs `main`). Evaluate it from 4 perspectives, give
each a rating out of 10, and list findings most-severe first with `file:line`. Rules of reference: `AGENTS.md` (§5
standards, §11 invariants).

## Perspectives

**1. Architecture**
- Hexagonal layout (`config/`, `domain/`, `infrastructure/driving|driven`) and ArchUnit rules hold; `domain` is free of
  Spring web/persistence types and reaches infra only via `port` interfaces.
- Data flow controller → service → port → adapter; controllers implement a contract interface carrying the endpoint
  metadata; no entity or domain model leaks through the API; explicit mappers.
- Configuration is externalized; Flyway is the only schema owner (new `V…` migration, never edit an applied one).

**2. TechLead**
- Clean code: methods ≤ 25 lines, explicit naming, no dead code, tabs, idiomatic Kotlin (null-safety, immutability).
- Reactive: no blocking (`.block()`, `Thread.sleep`, blocking IO), no lost `Mono`/`Flux`, correct operators
  (`flatMap` vs `map`, `switchIfEmpty`, `concatMap`), errors mapped in the pipeline, no `subscribe()` on the request
  path, one operator per line with logic extracted into named methods.
- Modernity: the code uses the latest idiomatic way of doing things for the versions in `build.gradle.kts` (Kotlin,
  JDK 25, Spring Boot, Spring Security, Reactor). Flag deprecated or superseded APIs and legacy patterns for which a
  newer standard exists (Kotlin language and stdlib features, current Spring configuration style, current Reactor
  operators). Check the official release notes or documentation of the version in use rather than relying on memory,
  and only recommend what the project's versions support.
- Transactions: multi-step writes inside `transactionalOperator::transactional`.
- Concurrency: any read-then-write that protects an invariant (last administrator, uniqueness, quota, status
  transition, counter) takes its lock in the same transaction, before the check: `SELECT ... FOR UPDATE` (or an atomic
  conditional `UPDATE`) on the rows the decision depends on. Check that the rows are locked in a consistent order
  across code paths (no deadlock), that the lock is taken by the service-level transaction and not released early, that
  the lock timeout surfaces as `409` and not as `500`, and that no check-then-act is left to a plain unlocked `SELECT`.
  Look for lost updates on read-modify-write and for non-idempotent retries.
  Verify that tests cover the concurrent case (two simultaneous requests) when the change introduces or touches such
  an invariant.
- SQL: filtering/sorting/pagination pushed to SQL, no N+1, no load-then-filter.

**3. QA**
- Every service, port implementation and endpoint has tests (`StepVerifier` + `mockito-kotlin`, Testcontainers for
  repositories, `WebTestClient` for contract and authorization).
- AAA pattern with the exact `// Arrange`, `// Act`, `// Assert` comments; edge cases, error branches, allow/deny
  authorization cases, validation bounds; Kover gate holds.

**4. Cybersecurity**
- Every endpoint has the right `@PreAuthorize` (project-scoped checks through `hasPermission`, option gate where
  applicable) using permission constants, not literals; RBAC seed migrations stay in lockstep with the expressions.
- Tenant isolation, parameterized SQL, input validation and bounds, mass assignment, IDOR.
- Stateless security, CORS never `*`, headers stay in `SecurityConfig`; no tokens/secrets/PII logged or returned; no
  stack traces to the client; irreversibility safeguards of destructive operations.

**API conventions** (checked under Architecture): `/api/v2`, plural resource URLs, correct verbs and status codes,
pagination `page`/`size` returning `PageModel`, OpenAPI documentation on the contract interface.

## Rating scale (fixed, not discretionary)

A rating is **computed** from the findings, never judged by feel. Each finding is attributed to exactly one
perspective (the one it mainly belongs to) and has exactly one severity.

- **Start at 10 per perspective**, then subtract per finding of that perspective: **blocker −4**, **should-fix −1**,
  **nit −0.25** (nits together never cost more than −1). Floor at 0. No rounding.
- No finding means 10. Any finding means below 10. A rating without matching findings is invalid.
- A perspective that does not apply to the change (e.g. Cybersecurity on a test-only change) is rated 10 and marked
  "n/a".

**Severity definitions**

- **Blocker:** breaks a §11 invariant or a build-enforced rule (ArchUnit, `koverVerify`), a security flaw (missing or
  weakened authorization, tenant isolation, injection, leaked secret or internal message), a check-then-act on an
  invariant without a lock (race condition), wrong behavior or data loss,
  a blocking call on the request path, a missing migration or RBAC lockstep break, a failing build or coverage drop.
- **Should-fix:** breaks a §5 standard or a skill rule without being a blocker: method > 25 lines, new behavior
  (service, endpoint, port implementation) without tests, dead code, missing KDoc header, deprecated or superseded API,
  N+1 or load-then-filter, one-operator-per-line violation, missing validation bound.
- **Nit:** naming, formatting, wording or ordering with no behavioral effect.

With this scale, one blocker always rejects (≤ 6), three should-fix reject (7), two do not (8). Identical findings
must give identical ratings on a re-review.

## Output

1. Findings grouped by severity (blocker / should-fix / nit), each with an id: location, perspective, problem, why it
   matters, suggested fix. Do not restate unchanged code.
2. Ratings table, one row per perspective: starting 10, the ids of its findings with their deductions, resulting
   rating: Architecture /10, TechLead /10, QA /10, Cybersecurity /10.
3. Verdict: approved only if **every** rating is ≥ 8/10; otherwise list the refactorings required before re-review.
