---
name: commit
description: Plan and create commits for the Registry backend (Conventional Commits, smallest testable increments, build gate). Use whenever asked to commit, split work into commits, or plan commit scopes.
---

# Commit — Registry Backend

## Format

`<type>(<scope>): <short summary>` in US English, `type` ∈ `feat, fix, chore, docs, style, refactor, perf, test`.
Conventional Commits are mandatory: semantic-release derives the version, changelog and tag from them (ADR 009).

## Breakdown

- Plan the commit scopes with the user **before** development begins.
- One behavior per commit, the smallest functionally testable increment (an endpoint and a new cross-field constraint
  are two commits). Aim for diffs under ~100 lines, excluding README/doc sync and generated mappers.
- Natural order for a new resource:
  1. migration + entity/mapper + repository tests;
  2. domain model + port + service + unit tests;
  3. contract interface + `@PreAuthorize` + DTOs + endpoint + contract and authorization tests.

## Gates

- Run `./gradlew build` first; it must pass cleanly (tests, ArchUnit, `koverVerify`). Do not commit on failure, on a
  coverage drop, or if the feature cannot be verified independently.
- Run the `code-review` skill beforehand; all 4 ratings must be ≥ 8/10.
- Work-in-progress gate: if a complete testable feature is uncommitted, refuse unrelated new work until it is committed.
- The git remote is read-only: no write action on it (push, creating or editing a pull request, issue or release,
  etc.). The user handles those.
- If a misunderstanding or a bug shows up, stop immediately and never stack patch commits on a broken change. Explain
  the issue in one sentence so the user can `git reset`.
