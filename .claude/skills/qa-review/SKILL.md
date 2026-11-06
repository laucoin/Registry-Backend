---
name: qa-review
description: Evaluates backend test strategy and quality, including comprehensive coverage (unit tests, Testcontainers integration tests, WebTestClient), AAA pattern adherence, strict role/scoped isolation security testing (A/B cross-scoped tenant boundaries), non-flaky deterministic guarantees, error handling, and the Kover gate.
---

# QA Review (Test Strategy & Robustness)

Review the change (diff, files, or current branch) against the quality and testing invariants of the backend.
Start with a baseline score of **100 points** and deduct points based on findings: **blocker −40**, **should-fix −10**,
**nit −2.5** (floor at 0).

## Evaluation Criteria

### 1. Test Coverage & Typology

- **Tested Scope:** Every modified or created service, port implementation, mapper, and endpoint must have dedicated
  tests.
- **Testing Tools & Stacks:**
    - `StepVerifier` (for reactive streams) + `mockito-kotlin`.
    - Testcontainers for repositories and database integration tests.
    - `WebTestClient` for contract and endpoint authorization tests.
- **Kover Gate:** Global and incremental code coverage thresholds mandated by the Kover plugin must be respected (zero
  coverage drop tolerated).

### 2. App & Scoped Roles Isolation Testing (RBAC)

- **Global vs. Scoped Roles:** Test suites must validate the distinct behavior between global application roles and
  scoped roles.
- **Cross-Scoped Isolation (A/B Testing):**
    - Every test attempting to access a **Scoped Role A** resource using credentials/roles restricted to **Scoped Role
      B** must
      explicitly verify an access-denied failure (`403 Forbidden` / security exception).
    - Mandatory negative test coverage targeting cross-scoped data leakage.

### 3. Test Structure & Rigor (AAA Pattern)

- **Strict AAA Structure:** Every test must explicitly separate its phases with mandatory comments:
    - `// Arrange`
    - `// Act`
    - `// Assert`
- **Behavioral Assertions:** Tests must validate **real behavior** (return values, HTTP statuses, payloads, database
  mutations) rather than merely verifying mock invocations (`verify`).
- **Intact Tests:** No test may be disabled (`@Disabled`), ignored, or weakened (assertions removed) to bypass CI.

### 4. Edge Cases, Errors, Concurrency & Anti-Flaky Guarantees

- **Error Branches & Boundaries:** Mandatory coverage for edge cases, error flows, allow/deny authorization paths, and
  validation bounds (min value, max value, max+1).
- **Concurrency & Race Conditions:** If the change touches or introduces a critical invariant (locking, uniqueness,
  counters, statuses), concurrency tests simulating simultaneous requests are required.
- **Zero Flaky Tests & Determinism:**
    - No unseeded randomness or dependency on unmocked system clock (`Clock` injection required).
    - **No arbitrary `Thread.sleep()`** in reactive or integration tests (use `StepVerifier` time virtualizers or
      reactive synchronization).
    - **Clean State Isolation:** Testcontainers must ensure total database isolation and rollbacks between tests to
      prevent execution order dependencies.

---

## Output Format

- **Partial Score:** Start at 100, apply deductions, and output the resulting score (e.g., `QA: 90/100`).
- **Findings List:** Grouped by severity with precise locations (`file:line`), explanation, and exact fixes.
