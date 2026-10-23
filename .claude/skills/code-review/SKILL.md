---
name: code-review
description: Review gate for the Registry backend. Use only when the user asks to review, check or audit a diff, file, branch or change, or right before a commit. Never run it automatically after an edit. One exhaustive pass, 4 perspectives rated out of 10, ends with a merge-ready verdict.
---

# Code review — Registry Backend

Review the change pointed at (a diff, a file, or the current branch vs `main`). Evaluate it from 4 perspectives, give
each a rating out of 10, and list findings most-severe first with `file:line`. Rules of reference: `AGENTS.md` (§5
standards, §11 invariants) plus the checks below (best practices, dependencies, build evidence).

## Contract: one review, merge-ready after the fixes

The user asks for **one** review, not a loop. Therefore:

- **Exhaustive, single pass.** Report every finding (blockers, should-fix and nits) in this one pass. Never hold a
  finding back for a later round and never discover a new category of problem on re-review.
- **Never sample.** List the whole change first (`git diff <base>...HEAD --name-status`, with sizes). Every changed
  file under `src/main`, `src/main/resources` (migrations, i18n, config) and `build.gradle.kts` is read. Tests are read
  for the changed behavior. If the change is too large to read fully in one session, say so up front and split the
  review by area (agreed with the user) instead of reading a subset and rating the whole.
- **Verify before claiming.** A finding about something *missing* (test, lock, annotation, header) is only reported
  after searching for it (grep on the symbol, the test directory, the migration folder). State what was searched.
  A claim that could not be verified is marked `unverified`, carries no deduction, and is listed apart.
- **Evidence over opinion.** Run the gate (`./gradlew build`; if it cannot run, say why and mark the QA and
  build-related points `unverified`). Quote the failing task or coverage figure.
- **Actionable.** Every finding gives the exact fix (what to change, where), precise enough to apply without
  another round of questions. When a fix needs a decision from the user (design, scope, spec), ask it at the end in
  one grouped `AskUserQuestion`, not as a vague remark.
- **Done definition.** Once every blocker and should-fix is fixed (or explicitly accepted by the user) and the build is
  green, the code is ready to merge. A follow-up check only verifies the fixes and regressions *introduced by those
  fixes*; it must not raise anything that was already visible in the first pass.

## Perspectives

**1. Architecture**
- Hexagonal layout (`config/`, `domain/`, `infrastructure/driving|driven`) and ArchUnit rules hold; `domain` is free of
  Spring web/persistence types and reaches infra only via `port` interfaces.
- Data flow controller → service → port → adapter; controllers implement a contract interface carrying the endpoint
  metadata; no entity or domain model leaks through the API (check every return type of every endpoint); explicit mappers.
- Configuration is externalized (typed properties, no environment-specific value baked in); Flyway is the only schema
  owner (new `V…` migration, never edit an applied one); the config keys are documented in `README.md`.
- Documentation sync: README ("How to install and use it?"), OpenAPI annotations and `api-reference` impact of
  changed endpoints, config keys, migrations or dependencies are up to date (report drift, do not edit docs during
  a code review).
- Backward compatibility: breaking API/contract changes are intentional, versioned (`/api/v2`) and flagged for the
  frontend (`../Registry-Frontend`) when a response/request shape changes.

**2. TechLead**
- Clean code: methods ≤ 25 lines (tests included), explicit naming, no dead code or unused imports, tabs, idiomatic
  Kotlin (null-safety, immutability), KDoc `Purpose/Scope/Limits` header on every non-passive class.
- Reactive: no blocking (`.block()`, `Thread.sleep`, blocking IO), no lost `Mono`/`Flux`, correct operators
  (`flatMap` vs `map`, `switchIfEmpty`, `concatMap`), errors mapped in the pipeline, no `subscribe()` on the request
  path, one operator per line with logic extracted into named methods, no `null` returned from a reactive method.
- Modernity: the code uses the latest idiomatic way of doing things for the versions in `build.gradle.kts` (Kotlin,
  JDK 25, Spring Boot, Spring Security, Reactor). Flag deprecated or superseded APIs and legacy patterns for which a
  newer standard exists (Kotlin language and stdlib features, current Spring configuration style, current Reactor
  operators). Check the official release notes or documentation of the version in use rather than relying on memory,
  and only recommend what the project's versions support.
- Transactions: multi-step writes inside `transactionalOperator::transactional`; no partial write on error paths.
- Concurrency: any read-then-write that protects an invariant (last administrator, uniqueness, quota, status
  transition, counter) takes its lock in the same transaction, before the check: `SELECT ... FOR UPDATE` (or an atomic
  conditional `UPDATE`) on the rows the decision depends on. Check that the rows are locked in a consistent order
  across code paths (no deadlock), that the lock is taken by the service-level transaction and not released early, that
  the lock timeout surfaces as `409` and not as `500`, and that no check-then-act is left to a plain unlocked `SELECT`.
  Look for lost updates on read-modify-write and for non-idempotent retries.
  Verify that tests cover the concurrent case (two simultaneous requests) when the change introduces or touches such
  an invariant.
- SQL and performance: filtering/sorting/pagination pushed to SQL, no N+1, no load-then-filter, no unbounded
  collection or query; new filter/sort/join columns are backed by an index (check the migrations); no per-request
  recomputation of static data; caches have a bound and an invalidation story.
- Migrations: forward-only, idempotent-safe on existing data, no long exclusive lock on a large table without
  justification, data-destructive statements (`DELETE`, `DROP`) are intentional and called out, FK `ON DELETE` choices
  are coherent with the retention rules.
- Observability and errors: logs carry no secret/PII, error keys exist in every i18n bundle (en, fr), exceptions map to
  the right status code, nothing is swallowed.

**3. QA**
- Every service, port implementation, mapper and endpoint has tests (`StepVerifier` + `mockito-kotlin`, Testcontainers
  for repositories, `WebTestClient` for contract and authorization).
- AAA pattern with the exact `// Arrange`, `// Act`, `// Assert` comments; edge cases, error branches, allow/deny
  authorization cases, validation bounds (min, max, max+1); concurrency cases for locked invariants.
- Tests assert behavior (values, status, payload), not only that a mock was called; no test silently disabled or
  weakened by the change; the Kover gate holds.

**4. Cybersecurity**
- Every endpoint has the right `@PreAuthorize` (project-scoped checks through `hasPermission`, option gate where
  applicable) using permission constants, not literals; RBAC seed migrations stay in lockstep with the expressions.
  A rule enforced only in the service does not replace the contract annotation.
- Tenant isolation, parameterized SQL, input validation and bounds (every `@RequestParam`, path variable, body field
  and collection size), mass assignment, IDOR, SSRF/open redirect on any URL taken from input.
- Stateless security, CORS never `*`, headers stay in `SecurityConfig`; public endpoints strictly match the §11 list
  (any addition is reported, and accepted only if the user confirms it); no tokens/secrets/PII logged or returned; no
  stack traces to the client; irreversibility safeguards of destructive operations; file/export endpoints set
  `no-store` and a safe `Content-Disposition`.
- Dependencies and supply chain (whenever `build.gradle.kts`, the version catalog, lock files, the `Dockerfile` or CI
  workflows change, and once per branch review otherwise):
  - list added, removed and upgraded dependencies (`git diff <base>...HEAD -- build.gradle.kts gradle/`);
  - check each added/upgraded one, and the Spring Boot / Spring Security / Netty / Jackson / R2DBC driver / Nimbus
    versions in use, against published advisories (GitHub Advisory Database, OSV.dev, NVD) with a web search; give the
    advisory id (CVE/GHSA), the affected range, the fixed version;
  - check that the project's own tooling covers it (`dependency-review.yml`, CodeQL) and run the available dependency
    report (`./gradlew dependencies` or the configured scanner) when possible;
  - flag unpinned/`latest`/snapshot versions, new repositories, and base image changes (must stay distroless,
    non-root).
  A known exploitable CVE on a dependency actually used is a blocker; a CVE on a transitive dependency with a fix
  available is a should-fix; an unverifiable result is reported `unverified`.
- Secrets: no credential, token or key in code, test fixtures, seed data, `application.yml` defaults or CI files.

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
- `unverified` items carry no deduction but block the "ready to merge" verdict until verified or accepted.

**Severity definitions**

- **Blocker:** breaks a §11 invariant or a build-enforced rule (ArchUnit, `koverVerify`), a security flaw (missing or
  weakened authorization, tenant isolation, injection, leaked secret or internal message, exploitable dependency CVE),
  a check-then-act on an invariant without a lock (race condition), wrong behavior or data loss, a blocking call on the
  request path, a missing migration or RBAC lockstep break, a failing build or coverage drop.
- **Should-fix:** breaks a §5 standard or a skill rule without being a blocker: method > 25 lines, new behavior
  (service, endpoint, port implementation) without tests, dead code, missing KDoc header, deprecated or superseded API,
  N+1 or load-then-filter, one-operator-per-line violation, missing validation bound, missing index on a new
  filter/sort column, documentation drift, vulnerable transitive dependency with a fix available.
- **Nit:** naming, formatting, wording or ordering with no behavioral effect.

With this scale, one blocker always rejects (≤ 6), three should-fix reject (7), two do not (8). Identical findings
must give identical ratings on a re-review.

## Output

1. Scope: files reviewed (count, by area), what was run (`./gradlew build` result, dependency check), anything
   `unverified` and why.
2. Findings grouped by severity (blocker / should-fix / nit), each with an id: location, perspective, problem, why it
   matters, exact fix. Do not restate unchanged code.
3. Ratings table, one row per perspective: starting 10, the ids of its findings with their deductions, resulting
   rating: Architecture /10, TechLead /10, QA /10, Cybersecurity /10.
4. Verdict:
   - **Approved** only if every rating is ≥ 8/10, no blocker is open and nothing is `unverified`.
   - **Ready to merge after fixes**: state the exact list of findings to fix (ids). Once they are applied and the
     build is green, no further review round is needed.
   - Otherwise, list the refactorings required, with the decisions the user has to make (one grouped question).
