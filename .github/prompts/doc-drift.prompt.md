---
mode: agent
name: doc-drift
description: Compare the Registry backend codebase against its documentation and surface drift
---

# Documentation drift check — Registry Backend

Find every place where the documentation and the backend codebase disagree, then help me resolve each one. Don't change
anything until step 4 tells you which action to take.

## Step 1 — Locate the documentation

Ask me where the documentation lives, if I haven't said already. Default: `https://doc.laucoin.fr/registry`. I may
instead give a local path (a checkout of the docs source) — treat that as authoritative over the remote default whenever
provided.

Once you know the location, look for an `AGENTS.md` at its root (`<location>/AGENTS.md` whether that's a URL or a local
path) and follow it for how to navigate the docs. If there isn't one, fall back to the site's own navigation: this
documentation set is generally split into a **Functional** section (what the product does, roles/permissions, features
with business rules and BDD scenarios) and a **Technical** section (architecture, stack, API contracts, data model,
ADRs). Use that split for steps 2 and 3 below. If the real structure differs, adapt to what you actually find.

## Step 2 — Compare the technical diff

Compare the Technical documentation against what the backend actually does:

- Hexagonal layering (`domain` / `port` / `infrastructure/in` / `infrastructure/out`) and whether it still matches
  what's documented.
- Reactive stack (WebFlux/R2DBC) claims vs actual code.
- API contracts: versions (`/api/v1`, `/api/v2`, sort-direction grammar, etc.), resource shapes, status codes,
  pagination — read the controller interfaces and OpenAPI annotations as the source of truth.
- Security model: authentication, `@PreAuthorize`/permission checks, CORS, headers.
- Data model: entities, migrations under `src/main/resources/db/migration`, relationships.
- Any ADRs — do they still reflect the decision actually implemented?

## Step 3 — Compare feature by feature

Walk the Functional documentation's feature list. For each documented feature, check against the domain/service code and
tests:

- Does it still exist, and does the described behavior match the current implementation?
- Do the documented roles/permissions match `ProjectPermissionConst`/`UserPermissionConst` usage?
- Do documented business rules, validation, and edge cases match `domain/validator`/`domain/service` logic?
- Do documented BDD scenarios still hold against current behavior?

## Step 4 — Verdict and action

If nothing surfaced in steps 2 and 3, tell me that and stop — no changes needed.

Otherwise, list every discrepancy point-by-point. For each one give: what the doc says, what the code actually does
(with `file:line`), and the doc section it came from. Then ask me, per discrepancy (or in bulk if I say so), to pick
one:

1. **Update the documentation** to match the code.
2. **Update the code** to match the documentation.
3. **Re-explain the feature** — my understanding of the doc or the code was wrong; I'll clarify and you re-evaluate that
   point.

Wait for my decision before touching anything.

## Step 5 — Making the change

- **Update the documentation**: if the location I gave you in step 1 was the remote URL, you can't write to it — ask me
  for a local path to the documentation source before editing. If I already gave a local path, edit it there directly.
- **Update the code**: follow this repo's conventions (`AGENTS.md`, and the checks in `code-review.prompt.md`) and
  update/add tests for the changed behavior.
- **Re-explain**: fold my correction back into your understanding and re-check whether the discrepancy still stands
  before moving on.
