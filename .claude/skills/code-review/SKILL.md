---
name: code-review
description: Review gate for the Registry backend. Orchestrates architecture, cybersecurity, QA, techlead, and interactive peer reviews sequentially from macro to micro, delegating details to sub-skills, using a /100 scale with iterative re-checks after fixes.
---

# Code review — Registry Backend (Orchestrator)

## 0. Scope, Volumetry & Context Analysis

- **Default:** Analyzes the diff of the current branch against `main` (`git diff main...HEAD`). The diff is the scoring
  basis; gaps in untouched existing code are reported as debt without deduction.
- **Volumetry threshold / Full project:** If the diff is too large (too many files/lines modified) or if the user
  requests a review of the **entire project**, immediately interrupt the analysis and suggest switching to an
  appropriate model with a large context window (e.g., 1M tokens) or proceeding by chunks.

---

## Contract: Sequential, Iterative, and Interactive Review (Macro -> Micro)

The analysis is performed **category by category, from macro to micro**, delegating each perspective to its dedicated
sub-skill.

- **Stop & Re-check Rule:** At the end of a category:
    - If the score is **100/100**, proceed directly to the next category.
    - If issues are detected (e.g., 80/100), **stop immediately**. Propose/apply the fixes with the user, then **re-run
      an evaluation pass on this same category** to update the score (e.g., back up to 100/100) before moving forward.
- **Evidence over Opinion:** Run the build (`./gradlew build`) beforehand to ensure technical health.
- **Branches & Commits:** Ignore branch and commit count/naming during review.

---

## Sequence of Perspectives & Delegation

The orchestrator delegates evaluations to specialized skills in the following descending order:

### 1. Architecture (Macro)

- **Delegated skill:** `architecture-review`
- *Verifies:* Hexagonal architecture compliance, domain isolation, data flow, configuration externalization, Flyway, and
  contract backward compatibility (`/api/v2`, OpenAPI).

### 2. Cybersecurity (Perimeter & Defense)

- **Delegated skill:** `cybersecurity-review`
- *Verifies:* `@PreAuthorize` annotations, tenant isolation, injection protection, supply chain (CVEs on dependencies),
  and absence of secret or PII leaks.

### 3. QA (Testing Strategy)

- **Delegated skill:** `qa-review`
- *Verifies:* Systematic test coverage for any new behavior, strict adherence to the AAA pattern, edge cases, error
  handling, role/project scope isolation, non-flaky guarantees, and the Kover gate.

### 4. TechLead (Micro & Code Quality)

- **Delegated skill:** `techlead-review`
- *Verifies:* Strict craft thresholds (max method length, parameter count, nesting level, complexity, clean lambdas,
  early return), systematic helper extraction, repository hygiene (no junk files), code spelling/grammar, and REST API
  design guidelines.

### 5. Peer (Feature by Feature)

- **Delegated skill:** `peer-review`
- *Verifies:* Collaborative and guided walkthrough feature by feature, IDE workspace cleanup via MCP, concise feature
  summaries (intent, impact, points of attention), constructive dialogue on implementation choices, and on-the-fly
  adjustments applied directly in the workspace.

---

## Scoring Scale (/100) & Step-by-Step Output

For each evaluated category (1 to 4):

- **Starts at 100 points**, then deducts per finding: **blocker −40**, **should-fix −10**, **nit −2.5** (floor at 0).
- Output includes a partial score out of 100, followed by targeted fixes and the re-verification cycle if the score is
  below 100/100.
