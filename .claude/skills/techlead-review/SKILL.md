---
name: techlead-review
description: Evaluates craft, code maintainability, spelling, textual quality, and REST API best practices (URL structure, HTTP verbs, status codes, versioning, pagination, security, date formatting, and filter/search handling) for the Registry backend. Incorporates strict limits (max 25 lines per method, max 7 parameters, max 2 nesting levels, max cyclomatic complexity of 6), clean lambdas without curly brace blocks, the early return pattern, systematic extraction into helpers, DRY principles, and linguistic conventions (code in English, comments in French, flawless spelling).
---

# TechLead Review (Craft, Clean Code, Spelling & API Design Guidelines)

Examine the change (diff, files, or current branch) against the craft, readability, maintainability, spelling
correction, and API design requirements of the Registry backend. Start with a base score of **100 points** and apply
deductions: **blocker −40**, **should-fix −10**, **nit −2.5** (minimum floor at 0).

## Evaluation Criteria & Strict Thresholds

### 1. Method Metrics & Complexity (Hard Limits)

- **Method Size:** A method must not exceed **25 lines** (excluding signature declaration and imports/annotations). Any
  longer block must be broken down or extracted.
- **Parameter Count:** Maximum **7 parameters** per method/constructor (prefer a parameter object or a DTO if the number
  of parameters increases). *Exemption:* pure data holders (entities, DTOs and domain models: classes under `entity/`,
  `dto/` or `model/` that carry fields and no business logic) are not counted; each parameter there is a column or a
  field. The named parameters of a Spring Data `@Query` repository method are not counted either: they are the bind
  markers of the SQL statement (temporary exemption, until the data access plan replaces the hand-written `@Query`).
  REST contracts and domain services are not exempt: group filter, sort and pagination parameters into a parameter
  object.
- **Nesting Levels:** Maximum **2 nesting levels** (e.g., a loop or an `if` nested inside another). Beyond that, extract
  the blocks into dedicated methods or helpers.
- **Cyclomatic Complexity:** Limited to a maximum of **6** per method. Control structures that are too complex
  (`if/else` chains, giant `when` blocks) must be simplified or polymorphed.

### 2. Style, Lambdas, Guard Clauses & Helpers

- **Early Return Pattern (Guard Clauses):** Systematically favor the **`return early`** pattern (guard clauses) to
  handle error cases, `null` checks, or boundary conditions right at the beginning of the method to flatten the
  execution flow and avoid indentation cascades.
- **Clean Lambdas:** Lambda expressions must fit on a single line and **not contain superfluous curly brace blocks (
  `{ ... }`)** when it can be written concisely or converted to a method reference (`::`).
- **Systematic Extraction (Helpers):** Anything that can be factored, cleaned up, or rendered stateless (calculations,
  formatting, data transformations, validations) **must be extracted** into dedicated helper classes (`domain/helper` or
  `infrastructure/**/helper`) to keep services and use-cases focused exclusively on business orchestration.

### 3. Non-Duplication (DRY) & Factorization

- **Zero Unjustified Duplication:** Track down and eliminate any unjustified code redundancy. Factorization toward
  shared components, helpers, or utility methods is an ongoing requirement.
- **Readability & Maintenance Factor:** Each block of code must be clean, explicit, and immediately understandable.

### 4. Linguistic Conventions & Spelling Correction

- **Code in English:** All source code identifiers (variable names, parameters, methods, classes, interfaces, packages,
  log messages, exceptions, API error messages) must be written **in English** clearly, rigorously, and free of spelling
  or grammar mistakes.
- **Comments in French:** As an exception, explanatory comments in the code (`// ...` or business JavaDoc) must be
  written **in French** with rigorously correct spelling.
- **KDoc Header:** Every non-passive class or interface carries the `Objectif / Périmètre / Limites` block defined in
  `AGENTS.md` §8 (missing or malformed header: should-fix).
- **Spelling and Semantic Validation:** Check for the absence of typos, spelling mistakes, or awkward phrasing across
  all modified texts (literals, log messages, error messages, associated documentation).
- **Translation and Consistency:** Ensure that translation, log, and exception files are properly spelled, consistent,
  understandable, and translated into all application languages (for translation files).

### 5. REST API Design Best Practices

- **Documentation & Security:**
    - Use recognized standards like OpenAPI to describe and document endpoints.
    - Transport, authentication and authorization concerns are reviewed in `cybersecurity-review`.
- *All URLs, versions and status codes below are examples of the rule; the project's own version is `/api/v2`
  (`AGENTS.md` §10).*
- **URL and Naming Conventions:**
    - Use *lower camel case* for attributes and query parameters.
    - Use English **nouns** (never verbs) in the **plural** for resources and collections (e.g., `GET /v1/products`,
      rather than `/getAllProducts` or `/product/1234`).
    - To express a relationship, use sub-resources (e.g., `GET /v1/products/1234/features/1`).
    - The major version is mandatory in the URL (e.g., `/v1/products`; in this project `/api/v2/products`). Every
      endpoint must be versioned; the prefix itself is not a rule.
    - If an operation does not correspond to a standard CRUD pattern, favor the action format:
      `/{resource}/{id}:{action}` (e.g., `POST /users/12:subscribe`).
- **HTTP Verbs and Status Codes:**
    - Respect verb adequacy (`GET` for reading, `POST` for creation, `PUT` for update, `DELETE` for deletion).
    - Return appropriate standard status codes: `200 OK`, `201 Created` (on creation), `202 Accepted` (asynchronous),
      `204 No Content` (on deletion), `206 Partial Content` (only for range-based partial responses; paginated pages
      returning `PageModel` use `200`), `301/302/304` (redirects/cache),
      `400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found`, `405 Method Not Allowed`,
      `406 Not Acceptable`, and `500 Internal Server Error`.
- **Query String, Pagination & Formats:**
    - Use the `q` keyword for full-text search (e.g., `?q=electric+drill`).
    - Use `attribute=value` for filtering, and `sort attribute` / `sort -attribute` (or `?desc / ?asc`) for sorting.
    - Allow partial responses via the `fields` parameter (e.g., `?fields=brand,idModel`).
    - Pagination is mandatory on collections, with a default and a maximum page size (project values: `AGENTS.md` §10).
    - Use the ISO 8601 format for dates and times (e.g., `2018-04-09T09:42:51Z`).
    - A health check must be exposed (in this project through `/actuator/**`, behind its feature flag).

---

## Output Format

1. **Partial Score:** Start at 100, apply deductions (e.g., `TechLead: 90/100`).
2. **List of Anomalies:** Grouped by severity with precise location (`file:line`), explanation of the metric violation,
   API rules, or spelling issue, and exact fix.
