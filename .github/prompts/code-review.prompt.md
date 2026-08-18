---
mode: agent
description: Review a change in the Registry backend against project conventions
---

# Code review — Registry Backend

Review the change I point you at (a diff, a file, or the current branch vs `main`). Report findings most-severe first;
if nothing is wrong, say so. Prefer concrete, actionable comments with `file:line`.

**Review the code as it is now, not as the diff describes it.** Later commits on a branch often already answer an
earlier comment. Check `HEAD`, and check whether the CI run you are looking at is even on `HEAD` before calling a
check red.

**Verify claims instead of asserting them.** "The framework can't do X" is a finding only after you have run something
that shows it. A one-off probe test is cheap and settles the argument permanently.

## What to check

**Correctness**

- Logic, edge cases, null/empty handling, off-by-one, wrong operators.
- Error and authorization paths — is every failure mode handled and mapped correctly?

**Reactive (WebFlux) — high priority**

- No blocking on reactor threads: no `.block()`, `Thread.sleep`, blocking JDBC/IO, or blocking calls inside `map`/
  `flatMap`.
- Streams are actually subscribed (returned or composed) — no "lost" `Mono`/`Flux` whose side effects never run.
- Correct operator choice: `flatMap` vs `map`, `switchIfEmpty`, `concatMap` ordering, error operators (`onErrorResume`,
  `onErrorMap`).

**Architecture (hexagonal — partly enforced by ArchUnit)**

- `domain` stays free of Spring web / persistence types and talks to infra only via `port` interfaces. ArchUnit does
  **not** check this one — a new `domain` class importing `infrastructure/…` or `org.springframework.web` passes the
  build and still has to be flagged.
- `infrastructure/out` doesn't depend on `infrastructure/in`; nothing depends on `config`; postgres `entity` stays
  inside its package; controllers implement a contract interface.
- No shaded third-party internals (`com.nimbusds.jose.shaded.*`) — they are private repackagings, not API.

**Security — top priority (see AGENTS.md § Security)**

- Every endpoint has an explicit `@PreAuthorize` with the correct `hasPermission(...)` checks (option gate + operation
  CRUD permission); no missing or overly-broad authorization, no auth bypass.
- Permissions use `ProjectPermissionConst`/`UserPermissionConst` constants, not inline literals.
- Input validated before reaching the domain (`@Valid`, `@Min`/`@Max`, custom `domain/annotation` + `domain/validator`).
- Stays stateless (no session/form-login regressions); CORS not widened to `*`; security headers stay in
  `ApiHeadersHandler` (`HeadersHandler` is the locale filter).
- A new request/response header must also be declared in `SecurityConfig`'s CORS `allowedHeaders` / `exposedHeaders`,
  or the browser cannot send or read it.
- No tokens/credentials/PII logged; errors go through the advice/handlers and don't leak internals or stack traces.

**API best practices (see AGENTS.md § API)**

- Resource-oriented, versioned URLs (`/api/v1/...`, plural nouns, correct ownership nesting); correct HTTP verb + status
  codes.
- Controller is an interface with OpenAPI `@Tag`/`@Operation` docs; implementation delegates to a domain service.
  `@Tag` names unique per API version; v1 operations stay `deprecated = true`.
- `@Operation(description = …)` must not restate request params, types, defaults or bounds — springdoc already derives
  those from the signature, and prose copies rot.
- Cross-cutting HTTP behaviour (`@PreAuthorize`, `@RateLimited`, `@HttpCacheable`) is declared **on the endpoint**.
  Flag any filter or handler that recognises endpoints by hard-coded path or query-param lists: the next person to edit
  that endpoint will not know the behaviour exists.
- Every new endpoint has an identified consumer (Registry-Frontend, Registry-E2E, or a scheduler). Unreferenced API
  surface is a finding, not a feature.
- Boundary uses reader/writer DTOs + mappers — never exposes persistence entities.
- Collection endpoints paginate with validated `pageNumber`/`pageSize` and return `PageModel`; no unbounded responses.
  Breaking changes go in a new version, not `v1`.

**Data**

- Schema changes ship as new Flyway migrations (never edit an applied one); R2DBC queries and mappers are correct;
  filtering/sorting/pagination pushed to SQL (no load-then-filter, no N+1).
- Indexes are justified per index. A b-tree for every sortable column taxes every write; question index sets that cover
  most columns of a table.
- Migrations added *and* reverted inside the same unreleased PR should usually be collapsed into one.

**Duplication (DRY) — look for it deliberately, it hides well**

- Near-identical classes: mappers/DTOs/services that differ only by a type parameter or a constant belong on a shared
  base or a generic.
- Two types whose names differ by one character or a plural `s` are a defect even when both are needed — rename so the
  difference is the *purpose*, not the spelling.
- The same annotation block, magic number, or default repeated across many endpoint signatures wants a constant or a
  `@ParameterObject`.
- Duplicated i18n keys and duplicated dependency version literals.

**Side effects and response latency**

- Audit, metrics, notification and other side effects must never change or fail the API response. Note that
  `@TransactionalEventListener` is **not** supported under reactive (R2DBC) transactions — it needs a thread-bound
  transaction — so "move it after commit" is not a valid suggestion here.

**Build & CI**

- Dependency versions in a `val`, grouped under the existing section comments — never inline in the coordinate string.
- Growth in the Spring context (new controllers, new starters) can push the test JVM past its heap; `maxHeapSize` and
  `maxParallelForks` in `build.gradle.kts` are part of the review surface when the surface grows.
- Repo hygiene: no committed local artefacts or per-tool agent files that `.gitignore` should own; prefer a pattern
  (`local-dev/**/data`) over one line per directory.

**Style & tests**

- Tabs for indentation; idiomatic Kotlin; matches surrounding code.
- Tests cover the new/changed behavior; coverage stays green (`koverVerify`).
- **Comment policy (AGENTS.md § Comment policy)**: flag inline explanations or narration inside method bodies,
  end-of-line (trailing) comments, commented-out code, signature-restating docstrings, and any ADR reference in a
  comment. Intent documentation belongs in one English KDoc block comment (`/** … */`) directly above a genuinely
  complex method — flag a `//` block used for documentation. In tests, `// Arrange` / `// Act` / `// Assert` structure
  comments are mandatory — flag their absence, and flag any change that deletes or edits them.

**Prefer what already exists**

- Spring / Jakarta constants, validators and types before hand-rolled ones (`HttpHeaders.*`,
  `XContentTypeOptionsServerHttpHeadersWriter.NOSNIFF`, `@Email`). A hand-rolled equivalent needs a stated, verified
  reason in the code.
- A new port/service method that an existing one already covers via its search-param model is a finding — check the
  existing signature's optional flags before adding a sibling.

## Output

Group findings by severity (blocker / should-fix / nit). For each: location, what's wrong, why it matters, and a
suggested fix. Do not restate unchanged code as findings.

Say plainly which review comments the code already answers, and push back with evidence when a suggestion would make
the code worse — a review is a conversation, not a checklist.
