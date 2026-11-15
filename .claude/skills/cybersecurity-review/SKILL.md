---
name: cybersecurity-review
description: Evaluates Registry backend security, including RBAC access control, tenant isolation, input validation, injection protection, CSP/CSRF/XSS mitigations, secret management, and supply chain security.
---

# Cybersecurity Review (Perimeter & Defense)

Review the change (diff, files, or current branch) against the security and isolation invariants of the Registry
backend. Start with a baseline score of **100 points** and deduct points based on findings: **blocker −40**,
**should-fix −10**, **nit −2.5** (floor at 0).

## Evaluation Criteria

### 1. Access Control & Authorizations (RBAC)

- **Endpoint Securing:** Every endpoint must have the appropriate `@PreAuthorize` annotation with project/tenant-scoped
  filters (e.g., `hasPermission(#projectId, '$REGISTRY_PROJECT_PARTICIPANT_R')`, ADR 005).
- **Annotation Exceptions:**
    - **Authentication:** Not required on authentication endpoints.
    - **Static Metadata:** Not required (discouraged) on purely static metadata endpoints or enums with translations.
    - **Dynamic Metadata (⚠️ Attention):** `@PreAuthorize` remains **strictly required** for metadata endpoints fetching
      dynamic information from the database.
- **Permission Constants:** Use exclusively the typed constants of `domain/constant` (e.g.,
  `ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_R`) and never literal strings.
- **RBAC Seed Synchronization:** RBAC seed migrations must remain strictly aligned with `@PreAuthorize` expressions.

### 2. Tenant Isolation & Input Validation

- **Strict Multi-Tenant Isolation:** Rigorously verify that no request allows unauthorized cross-tenant data access or
  modification.
- **Validation & Parameterized Queries:** SQL injection is strictly prohibited (exclusive use of parameterized queries /
  R2DBC). Strict validation and bounds checking for every `@RequestParam`, `@PathVariable`, request body field, and
  collection size.
- **Web Attacks & Common Vulnerabilities Prevention:**
    - **CSRF:** Verify protection against Cross-Site Request Forgery (relevant if sessions/cookies are used, or
      validation of a stateless JWT architecture).
    - **XSS & Security Headers (CSP):** Presence and strict configuration of defensive HTTP headers (Content Security
      Policy - CSP, X-Content-Type-Options, X-Frame-Options, Strict-Transport-Security).
    - **Other flaws:** Protection against mass assignment, IDOR, SSRF, or open redirect on any input-derived URL.

### 3. Flow Protection & Data Exposure

- **Secure Configuration:** Stateless security, strictly configured CORS (never `*`), security headers maintained in
  `SecurityConfig`.
- **Public Endpoints:** Public endpoints must strictly match the explicit whitelist (`AGENTS.md` §10). Any addition must be
  explicitly flagged and approved.
- **Data Leaks & Errors:** No tokens, secrets, passwords, or PII logged or returned in error payloads. No stack traces
  exposed to clients.
- **File Downloads:** Export/file endpoints must enforce `Cache-Control: no-store` and a secure `Content-Disposition`
  header.

### 4. Supply Chain & Dependencies

- **Dependency Audit:** Upon modifying `build.gradle.kts`, version catalogs, lock files, `Dockerfile`, or CI workflows:
    - List added, removed, or upgraded dependencies.
    - Check each modified dependency and core version (Spring Boot, Spring Security, Netty, Jackson, R2DBC, Nimbus)
      against published advisories (GitHub Advisory Database, OSV.dev, NVD). Report the CVE/GHSA ID, affected range, and
      fixed version.
- **Tooling & Image Security:** Docker base images must remain distroless/minimal and run as non-root. *Note: An
  exploitable CVE on a direct dependency is a **blocker**; a CVE on a transitive dependency with a fix available is a
  **should-fix**.*

### 5. Secret Management

- No credentials, API keys, JWT tokens, passwords, or private keys allowed in source code, test fixtures, seed data,
  `application.yml` defaults, or CI files.

---

## Output Format

- **Partial Score:** Start at 100, apply deductions, and output the resulting score (e.g., `Cybersecurity: 90/100`).
- **Findings List:** Grouped by severity with precise locations (`file:line`), threat explanation, and exact fixes.
