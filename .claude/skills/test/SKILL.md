---
name: test
description: Generates comprehensive, robust, and non-flaky backend tests (unit tests, Testcontainers integration tests, WebTestClient) following the AAA pattern, strict role/scoped isolation security requirements, and Kover quality standards.
---

# Test Writer (Backend Test Generation)

Generate robust, deterministic, and comprehensive tests for backend code, strictly adhering to quality invariants and
testing standards (coverage, RBAC role isolation, AAA pattern, error handling, and zero flakiness).

## Core Guiding Principles for Writing

### 1. Typology & Target Technical Stack

- **Unit Tests (Reactive Streams & Components):**
    - Use `StepVerifier` to validate reactive streams (Reactor).
    - Use `mockito-kotlin` for mocking dependencies.
- **Integration Tests (Repositories & Persistence):**
    - Systematic use of Testcontainers to isolate and provision databases (e.g., PostgreSQL).
- **Endpoint & Contract Tests (Security & API):**
    - Use `WebTestClient` to test API contracts and endpoint authorization.
- **Kover Gate:** Ensure that the generated code is fully covered to prevent any drop in overall code coverage.

### 2. Role Isolation & Security (RBAC & Scopes)

- **Global vs. Scoped Roles Separation:** Explicitly validate the distinct behavior between global application roles and
  scoped roles.
- **Cross-Scoped Isolation (Negative A/B Testing):**
    - Any test attempting to access a **Scoped Role A** resource using credentials/roles restricted to **Scoped Role B**
      must explicitly include a negative test verifying an access-denied failure (`403 Forbidden` or security
      exception).
    - Mandatory coverage to prevent cross-scoped data leakage.

### 3. Test Structure & Rigor (AAA Pattern)

- **Strict AAA Separation:** Every test must explicitly separate its phases using mandatory comments:
    - `// Arrange`
    - `// Act`
    - `// Assert`
- **Behavioral Assertions:** Validate **real behavior** (return values, HTTP status codes, payloads, database mutations)
  rather than merely verifying mock invocations (`verify`).
- **Integrity:** No test may be disabled (`@Disabled`), ignored, or weakened.

### 4. Edge Cases, Errors, Concurrency & Anti-Flaky

- **Error Branches & Boundaries:** Provide test cases for error flows, allow/deny authorization paths, and validation
  bounds (min value, max value, max+1).
- **Concurrency & Race Conditions:** If the code touches critical invariants (locking, uniqueness, counters, statuses),
  implement concurrency tests simulating simultaneous requests.
- **Anti-Flaky & Determinism Guarantees:**
    - No unseeded randomness or dependency on an unmocked system clock (`Clock` injection required).
    - **Absolute ban on `Thread.sleep()`** in reactive or integration tests (prefer `StepVerifier` time virtualizers or
      reactive synchronization).
    - **State Isolation:** Testcontainers must ensure total database isolation and rollbacks between tests.

---

## Expected Output Format

When writing tests, structure your deliverable as follows:

1. **Test Strategy & Covered Cases:** Brief description of the tested flows (positive, negative, security/RBAC,
   boundaries).
2. **Test Code Implementation:** Clean, idiomatic (Kotlin/Spring) code strictly following the `// Arrange`, `// Act`,
   `// Assert` pattern.
3. **Invariant Verification:** Confirmation that the use of Testcontainers, `StepVerifier`, `WebTestClient`, and the
   absence of `Thread.sleep()` are properly respected.
